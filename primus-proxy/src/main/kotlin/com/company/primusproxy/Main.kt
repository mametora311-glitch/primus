package com.company.primusproxy

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// 共通 JSON 設定
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

/**
 * エントリポイント
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val corsOrigins = (System.getenv("CORS_ORIGINS") ?: "*")
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val key = System.getenv("OPENAI_API_KEY")
    println("🔥🔥 Server Key Check: ${if (key.isNullOrBlank()) "NULL/EMPTY (鍵がない！)" else "OK (鍵あり: ${key.take(5)}...)"}")

    // ユーザーデータのロード
    UserStore.load()

    val server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) {
            json(json)
        }
        install(CORS) {
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader("X-Primus-Client")

            if (corsOrigins.contains("*")) {
                anyHost()
            } else {
                corsOrigins.forEach { origin ->
                    // http(s):// を削る
                    val normalized = origin
                        .removePrefix("https://")
                        .removePrefix("http://")

                    val parts = normalized.split(':')
                    val host = parts[0]
                    val portPart = parts.getOrNull(1) // "8080" など

                    if (portPart != null) {
                        // ポート指定がある場合は "host:port" として許可
                        allowHost("$host:$portPart")
                    } else {
                        // ポート指定なし
                        allowHost(host)
                    }
                }
            }
        }

        primusModule()
    }

    server.start(wait = true)
}

fun Application.primusModule() {
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
                    !checkUserToken(call.request.headers[HttpHeaders.Authorization])) {
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
                !checkUserToken(call.request.headers[HttpHeaders.Authorization])) {
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
                !checkUserToken(call.request.headers[HttpHeaders.Authorization])) {
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
                !checkUserToken(call.request.headers[HttpHeaders.Authorization])) {
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



    routing {
        // ヘルスチェック
        get("/") {
            call.respondText("primus-proxy ok", ContentType.Text.Plain)
        }

        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }

        // 認証チェックのみ
        get("/auth") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (!checkServiceToken(authHeader) && !checkUserToken(authHeader)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@get
            }
            call.respond(mapOf("ok" to true))
        }

        // クライアント用設定
        get("/config") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (!checkServiceToken(authHeader) && !checkUserToken(authHeader)) {
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
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (!checkServiceToken(authHeader) && !checkUserToken(authHeader)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@post
            }

            val body = call.receiveText()
            println("telemetry: $body")
            call.respond(mapOf("ok" to true))
        }

        // === Primus 対話本体 ===
        post("/v1/chat/completions") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (!checkServiceToken(authHeader) && !checkUserToken(authHeader)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@post
            }

            println("🔥🔥 Request received at /v1/chat/completions 🔥🔥")

            val raw = call.receiveText()

            // アプリ → Proxy 用のリクエストを解釈
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

            // Primus のベース人格プロンプト
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

            // クライアント側の ChatResp に合わせた形で返却
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

// アプリからのチャットリクエストをできる限り柔軟にパース
private fun parsePrimusChatReq(raw: String): PrimusChatReq {
    return try {
        json.decodeFromString(PrimusChatReq.serializer(), raw)
    } catch (_: Throwable) {
        val el = json.parseToJsonElement(raw)
        val obj = el as? JsonObject ?: return PrimusChatReq(emptyList(), null, null)

        val msgs = (obj["messages"] as? JsonArray)
            ?.mapNotNull { msgEl ->
                (msgEl as? JsonObject)?.let { msgObj ->
                    val role = msgObj["role"]?.jsonPrimitive?.content ?: "user"
                    val content = msgObj["content"]?.jsonPrimitive?.content ?: return@let null
                    PrimusChatMessage(role = role, content = content)
                }
            }
            ?: emptyList()

        val maxTokens = obj["max_tokens"]?.jsonPrimitive?.content?.toIntOrNull()
        val temperature = obj["temperature"]?.jsonPrimitive?.content?.toDoubleOrNull()

        PrimusChatReq(messages = msgs, maxTokens = maxTokens, temperature = temperature)
    }
}

// OpenAI Chat Completions を叩く。
private suspend fun callOpenAI(req: OpenAIChatReq): OpenAIChatResp {
    val apiBase = (System.getenv("OPENAI_API_BASE") ?: "https://api.openai.com").trimEnd('/')
    val url = "$apiBase/v1/chat/completions"
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY is not set")

    val requestBody = json.encodeToString(OpenAIChatReq.serializer(), req)

    return withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        conn.doOutput = true

        conn.outputStream.use { os ->
            os.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        val status = conn.responseCode
        val body = (if (status in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        if (status !in 200..299) {
            println("OpenAI error $status: $body")
            throw IllegalStateException("OpenAI error $status")
        }

        json.decodeFromString(OpenAIChatResp.serializer(), body)
    }
}

// ===== DTO =====

@Serializable
data class ConfigResponse(
    val api_base_url: String,
    val voicevox_base_url: String,
    val nlp_provider: String
)


// アプリ ↔ Proxy 用チャット DTO
@Serializable
data class PrimusChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class PrimusChatReq(
    val messages: List<PrimusChatMessage>,
    @SerialName("max_completion_tokens")
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
data class PrimusChatChoice(
    val index: Int,
    val message: PrimusChatMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class PrimusChatResp(
    val id: String,
    @SerialName("object")
    val obj: String,
    val choices: List<PrimusChatChoice>
)

// OpenAI 側 DTO
@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIChatReq(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAIChatResp(
    val id: String? = null,
    val choices: List<OpenAIChoice>
)

// サービス・トークン認証
private fun checkServiceToken(authHeader: String?): Boolean {
    val required = System.getenv("SERVICE_TOKENS") ?: ""
    if (required.isBlank()) return true // dev 用に無効化されていれば常にOK

    val bearer = (authHeader ?: "").let { header ->
        if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim()
        else header.trim()
    }

    val allowed = required
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return allowed.any { it == bearer }
}

// Primus のデフォルト人格プロンプト
private const val DEFAULT_SYSTEM_PROMPT: String =
    "あなたは『Primus』という名前の日本語話者のAIアシスタントです。" +
            "ユーザーの人生と仕事を長期的に支援することを目的とし、" +
            "落ち着いた口調で、事実ベースかつ論理的に回答します。" +
            "不明な点はごまかさず『分からない』と伝え、推測だけで断定しないでください。"

// === ユーザー管理ロジック & DTO ===

// ユーザーのBearerトークンを検証する
private fun checkUserToken(authHeader: String?): Boolean {
    val token = authHeader?.removePrefix("Bearer ")?.trim()
    if (token.isNullOrBlank()) return false
    return TokenManager.verify(token) != null
}

object UserStore {
    private val file = File("users.json")
    private val users = ConcurrentHashMap<String, UserData>()

    fun load() {
        if (!file.exists()) return
        try {
            val content = file.readText()
            val list = json.decodeFromString<List<UserData>>(content)
            users.clear()
            list.forEach { users[it.id] = it }
            println("✅ Loaded ${users.size} users")
        } catch (e: Exception) {
            println("⚠️ Failed to load users.json: ${e.message}")
        }
    }

    fun save() {
        try {
            val list = users.values.toList()
            val content = json.encodeToString(list)
            file.writeText(content)
        } catch (e: Exception) {
            println("⚠️ Failed to save users.json: ${e.message}")
        }
    }

    fun findByEmail(email: String): UserData? = users.values.find { it.email == email }
    fun findById(id: String): UserData? = users[id]

    fun add(user: UserData) {
        users[user.id] = user
        save()
    }
}

object TokenManager {
    private val tokens = ConcurrentHashMap<String, String>() // Token -> UserId

    fun createToken(userId: String): String {
        val token = UUID.randomUUID().toString()
        tokens[token] = userId
        return token
    }

    fun verify(token: String): String? = tokens[token]
}

fun generateSalt(): String {
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)
    return Base64.getEncoder().encodeToString(salt)
}

fun hashPassword(password: String, salt: String): String {
    val spec = "$password$salt".toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(spec)
    return Base64.getEncoder().encodeToString(hash)
}

@Serializable
data class UserData(
    val id: String,
    val email: String,
    val nickname: String,
    val passHash: String,
    val salt: String,
    val role: String,
    val createdAt: Long
) {
    fun toPublic() = PublicUser(id, email, nickname, role)
}

@Serializable
data class PublicUser(
    val id: String,
    val email: String,
    val nickname: String,
    val role: String
)

@Serializable
data class RegisterReq(
    val email: String,
    val password: String,
    val nickname: String? = null
)

@Serializable
data class LoginReq(
    val email: String,
    val password: String
)

@Serializable
data class AuthResp(
    val token: String,
    val user: PublicUser
)
