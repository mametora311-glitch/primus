package com.company.primus2.net

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.company.primus2.BuildConfig
import com.company.primus2.env.ProxyEnv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Primus プロキシへの薄いクライアント。
 * - 認証   : Bearer {serviceToken}
 * - API    : /auth, /config, /telemetry, /v1/chat/completions
 * - Timeout: connect=5s / request=30s / socket=20s
 * - ログ   : debug のみ（Authorization は常にマスク）
 */
@Serializable
    data class LicenseResp(
    @SerialName("plan") val plan: String? = null
)
class ProxyClient(
    private val baseUrl: String,
    private val serviceToken: String,
    timeoutMs: Long = 30_000L
) : AutoCloseable {

    private val client: HttpClient = HttpClient(OkHttp) {
        // JSON
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                }
            )
        }

        // Timeout
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = 5_000L
            socketTimeoutMillis = 20_000L
        }

        // 安全ログ（debug のみ）
        if (KtorLogging.isEnabled()) {
            install(Logging) {
                KtorLogging.enableIn(this)
            }
        }

        // 既定ヘッダ
        defaultRequest {
            header(HttpHeaders.Authorization, "Bearer $serviceToken")
            header(HttpHeaders.UserAgent, "PrimusAndroid/${BuildConfig.VERSION_NAME}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        install(PrimusAuthPlugin){
            requireIdToken = com.company.primus2.firebase.FirebaseConfig.REQUIRE_ID_TOKEN_FOR_REQUESTS
        }
    }

    /** /auth で疎通確認 */
    suspend fun authOk(): Boolean {
        val url = url("auth")
        return try {
            val resp: OkResp = client.get(url).body()
            resp.ok == true
        } catch (t: Throwable) {
            Log.w(TAG, "authOk failed: ${t.message}")
            false
        }
    }

    /** /config 取得 */
    @Serializable
    data class Config(
        val version: String? = null,
        val features: Map<String, Boolean> = emptyMap()
    )
    suspend fun fetchConfig(): Config? {
        val url = url("config")
        return try {
            client.get(url).body()
        } catch (t: Throwable) {
            Log.w(TAG, "fetchConfig failed: ${t.message}")
            null
        }
    }

    /** /telemetry 送信（イベント＋任意プロパティ） */
    suspend fun sendTelemetry(event: String, props: Map<String, String?> = emptyMap()): Boolean {
        val url = url("telemetry")
        val req = TelemetryReq(event = event, properties = props)
        return try {
            val resp: OkResp = client.post(url) { setBody(req) }.body()
            resp.ok == true
        } catch (t: Throwable) {
            Log.w(TAG, "sendTelemetry failed: ${t.message}")
            false
        }
    }

    /** /v1/chat/completions */
    suspend fun chatOnce(prompt: String, maxTokens: Int = 128): String? {
        val url = url("v1", "chat", "completions")
        val req = ChatReq(
            messages = listOf(Msg(role = "user", content = prompt)),
            maxTokens = maxTokens
        )
        return try {
            val resp: ChatResp = client.post(url) { setBody(req) }.body()
            resp.choices.firstOrNull()?.message?.content
        } catch (t: Throwable) {
            Log.w(TAG, "chatOnce failed: ${t.message}")
            null
        }
    }

    /** baseUrl にパスを安全に連結 */
    private fun url(vararg segments: String): String =
        URLBuilder(baseUrl).apply { appendPathSegments(*segments) }.buildString()

    override fun close() {
        try { client.close() } catch (_: Throwable) {}
    }

    suspend fun fetchLicense(): LicenseResp {
        // 例: /license をGET。サーバ側のパスが異なる場合はここだけ変えればOK
        val url = url("license")
        return client.get(url).body()
    }

    /** /health で単純な疎通確認 */
    suspend fun healthOk(): Boolean {
        val url = url("health")
        return try {
            client.get(url)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "healthOk failed: ${t.message}")
            false
        }
    }

    // ---- DTOs ----
    @Serializable private data class OkResp(val ok: Boolean? = null)

    @Serializable private data class TelemetryReq(
        val event: String,
        val properties: Map<String, String?> = emptyMap()
    )

    @Serializable private data class Msg(
        val role: String,
        val content: String
    )

    @Serializable private data class ChatReq(
        val messages: List<Msg>,
        @SerialName("max_tokens") val maxTokens: Int? = null
    )

    @Serializable private data class ChatResp(val choices: List<Choice>) {
        @Serializable data class Choice(val message: Msg? = null)
    }

    companion object {
        private const val TAG = "Primus/ProxyClient"

        fun default(): ProxyClient =
            ProxyClient(ProxyEnv.BASE_URL, ProxyEnv.SERVICE_TOKEN, timeoutMs = 30_000L)

        /** プロキシ疎通＆設定取得までまとめてチェック（起動時用） */
        suspend fun warmup(): Boolean {
            val client = default()
            return client.healthOk() && client.authOk() && client.fetchConfig() != null
        }

        @VisibleForTesting
        fun pingSync(): Boolean = try {
            runBlocking {
                val c = default()
                c.authOk() && c.fetchConfig() != null
            }
        } catch (_: Throwable) { false }

        @VisibleForTesting
        fun chatOnceSync(prompt: String, maxTokens: Int = 64): String = try {
            runBlocking { default().chatOnce(prompt, maxTokens) ?: "" }
        } catch (_: Throwable) { "" }
    }
}
