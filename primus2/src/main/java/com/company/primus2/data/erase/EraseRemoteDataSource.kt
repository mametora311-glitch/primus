package com.company.primus2.data.erase

import android.content.Context
import com.company.primus2.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * /eraseAll を叩くだけの最小データソース。
 * 仕様は下記の定数で後から差し替え可能（HTTP/PATH/成功コード）。
 */
object EraseApiSpec {
    /** true=DELETE, false=POST に切替可 */
    const val USE_DELETE: Boolean = true

    /** サーバ側のエンドポイント（後から差し替え可） */
    const val PATH: String = "/api/v1/eraseAll"

    /** 成功判定に含めるHTTPステータス（200/204など） */
    val SUCCESS_CODES: Set<Int> = setOf(
        HttpStatusCode.OK.value,
        HttpStatusCode.NoContent.value
    )

    /** 任意ヘッダ名（端末IDなどを渡したい場合に使用） */
    const val HEADER_DEVICE_ID: String = "X-Device-Id"
}

class EraseRemoteDataSource(
    context: Context
) {
    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }
        // 既存のKtorLoggingに寄せたい場合は後日ここを統合
        install(Logging)
    }

    /**
     * すべてのユーザデータ削除をサーバに要求する。
     * @param deviceId 任意（匿名運用時の識別に使用）
     * @return 成功なら true
     */
    suspend fun eraseAll(deviceId: String? = null): Boolean {
        val base = BuildConfig.BASE_URL // 既存BuildConfigに合わせる（必要なら PROXY_BASE_URL へ差替）
        val url = base.trimEnd('/') + EraseApiSpec.PATH

        val response = if (EraseApiSpec.USE_DELETE) {
            client.delete {
                url(url)
                headers {
                    // 既存方針に合わせ、サービス固定トークンを認証として送る
                    append("Authorization", "Bearer ${BuildConfig.SERVICE_TOKEN}")
                    if (!deviceId.isNullOrBlank()) {
                        append(EraseApiSpec.HEADER_DEVICE_ID, deviceId)
                    }
                }
            }
        } else {
            client.post {
                url(url)
                headers {
                    append("Authorization", "Bearer ${BuildConfig.SERVICE_TOKEN}")
                    if (!deviceId.isNullOrBlank()) {
                        append(EraseApiSpec.HEADER_DEVICE_ID, deviceId)
                    }
                }
            }
        }

        return response.status.value in EraseApiSpec.SUCCESS_CODES
    }
}
