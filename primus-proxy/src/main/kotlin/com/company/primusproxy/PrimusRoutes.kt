package com.company.primusproxy

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.registerPrimusRoutes() {
    routing {
        // === Auth (新規登録/ログイン/自分情報) ===
        route("/auth") {
            // POST /auth/register
            post("/register") {
                val req = try {
                    call.receive<RegisterReq>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_json"))
                    return@post
                }

                if (req.email.isBlank() || req.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_fields"))
                    return@post
                }

                // 同期ブロック内では「判定＋登録」だけ行う
                var conflict = false
                var authResp: AuthResp? = null

                synchronized(UserStore) {
                    if (UserStore.findByEmail(req.email) != null) {
                        conflict = true
                        return@synchronized
                    }

                    val salt = generateSalt()
                    val hash = hashPassword(req.password, salt)
                    val newUser = UserData(
                        id = UUID.randomUUID().toString(),
                        email = req.email,
                        nickname = req.nickname ?: "Guest",
                        passHash = hash,
                        salt = salt,
                        role = "USER",
                        createdAt = System.currentTimeMillis()
                    )
                    UserStore.add(newUser)
                    val token = TokenManager.createToken(newUser.id)
                    authResp = AuthResp(token, newUser.toPublic())
                }
                // ロックを抜けてから suspend 関数を呼ぶ
                if (conflict) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "email_exists"))
                    return@post
                }

                // ここまで来るなら authResp は必ず非 null の想定
                call.respond(authResp!!)
            }

            // POST /auth/login
            post("/login") {
                val req = try {
                    call.receive<LoginReq>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_json"))
                    return@post
                }

                val user = UserStore.findByEmail(req.email)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_credentials"))
                    return@post
                }

                val hash = hashPassword(req.password, user.salt)
                if (hash != user.passHash) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_credentials"))
                    return@post
                }

                val token = TokenManager.createToken(user.id)
                call.respond(AuthResp(token, user.toPublic()))
            }

            // GET /auth/me
            get("/me") {
                val token = call.request.headers[HttpHeaders.Authorization]
                    ?.removePrefix("Bearer ")
                    ?.trim()
                val userId = if (!token.isNullOrBlank()) TokenManager.verify(token) else null

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_token"))
                    return@get
                }
                val user = UserStore.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "user_not_found"))
                    return@get
                }
                call.respond(user.toPublic())
            }

            // GET /auth  （元の「認証チェックのみ」をそのまま移植）
            get {
                if (!checkServiceToken(call.request.headers[HttpHeaders.Authorization]) &&
                    !checkUserToken(call.request.headers[HttpHeaders.Authorization])
                ) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                    return@get
                }
                call.respond(mapOf("ok" to true))
            }
        }

        // ヘルスチェック
        get("/") {
            call.respondText("primus-proxy ok", ContentType.Text.Plain)
        }

        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }

        // クライアント用設定
        get("/config") {
            if (!checkServiceToken(call.request.headers[HttpHeaders.Authorization]) &&
                !checkUserToken(call.request.headers[HttpHeaders.Authorization])
            ) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@get
            }

            val apiBase = System.getenv("OPENAI_API_BASE") ?: "https://api.openai.com"
            val voicevoxBase = System.getenv("VOICEVOX_BASE_URL") ?: ""
            val nlpProvider = "openai"

            call.respond(
                ConfigResponse(
                    api_base_url = apiBase,
                    voicevox_base_url = voicevoxBase,
                    nlp_provider = nlpProvider
                )
            )
        }

        // テレメトリ（今は捨てるだけ）
        post("/telemetry") {
            if (!checkServiceToken(call.request.headers[HttpHeaders.Authorization]) &&
                !checkUserToken(call.request.headers[HttpHeaders.Authorization])
            ) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@post
            }

            val body = call.receiveText()
            println("telemetry: $body")
            call.respond(mapOf("ok" to true))
        }

        // === Primus 対話本体 ===
        post("/v1/chat/completions") {
            if (!checkServiceToken(call.request.headers[HttpHeaders.Authorization]) &&
                !checkUserToken(call.request.headers[HttpHeaders.Authorization])
            ) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@post
            }

            println("🔥🔥 Request received at /v1/chat/completions 🔥🔥")

            val raw = call.receiveText()

            val primusReq = parsePrimusChatReq(raw)
            val userMessage = primusReq.messages
                .lastOrNull { it.role == "user" }
                ?.content
                ?.takeIf { it.isNotBlank() }

            if (userMessage == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "no_user_message", "detail" to raw.take(500))
                )
                return@post
            }

            val systemPrompt = System.getenv("PRIMUS_SYSTEM_PROMPT") ?: DEFAULT_SYSTEM_PROMPT

            val openaiReq = OpenAIChatReq(
                model = System.getenv("OPENAI_MODEL") ?: "gpt-5-nano",
                messages = buildList {
                    add(OpenAIMessage(role = "system", content = systemPrompt))
                    primusReq.messages.forEach { msg ->
                        add(OpenAIMessage(role = msg.role, content = msg.content))
                    }
                },
                maxTokens = primusReq.maxTokens,
                temperature = primusReq.temperature
            )

            val openaiResp = try {
                callOpenAI(openaiReq)
            } catch (t: Throwable) {
                t.printStackTrace()
                call.respond(
                    HttpStatusCode.BadGateway,
                    mapOf("error" to "upstream_failed", "detail" to (t.message ?: "unknown"))
                )
                return@post
            }

            val choice = openaiResp.choices.firstOrNull()
            val answer = choice?.message?.content ?: ""

            val primusResp = PrimusChatResp(
                id = openaiResp.id ?: "primus-${System.currentTimeMillis()}",
                obj = "chat.completion",
                choices = listOf(
                    PrimusChatChoice(
                        index = 0,
                        message = PrimusChatMessage(role = "assistant", content = answer),
                        finishReason = choice?.finishReason
                    )
                )
            )

            call.respond(primusResp)
        }
    }
}
