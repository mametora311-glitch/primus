package com.company.primus2.net

import com.company.primus2.firebase.FirebaseAuthManager
import com.company.primus2.firebase.FirebaseConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/**
 * Firebase IDトークンを Authorization に適用する Ktor プラグイン（Context不要版）。
 *
 * - requireIdToken = true:
 *     トークン取得に失敗したら送信を中止して NetworkAuthException を投げる
 * - requireIdToken = false:
 *     取れたら置換、取れなければ既存ヘッダを維持
 */
class NetworkAuthException(message: String) : IllegalStateException(message)

val PrimusAuthPlugin = createClientPlugin("PrimusAuthPlugin", ::Config) {
    val require = pluginConfig.requireIdToken

    onRequest { request, _ ->
        try {
            // Firebase 初期化済みかどうかは「トークン取得可否」で判断（Context不要）
            val token = FirebaseAuthManager.getIdTokenIfInitialized(forceRefresh = false)

            if (token.isNullOrBlank()) {
                if (require || FirebaseConfig.REQUIRE_ID_TOKEN_FOR_REQUESTS) {
                    throw NetworkAuthException("Missing Firebase ID token (authentication required)")
                } else {
                    return@onRequest // 任意運用: 既存ヘッダのまま
                }
            }

            // ユーザーIDトークンを適用
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            request.headers.append("X-User-Token", token)
        } catch (e: NetworkAuthException) {
            throw e
        } catch (_: Throwable) {
            // 想定外は任意運用なら継続、必須なら中止
            if (require || FirebaseConfig.REQUIRE_ID_TOKEN_FOR_REQUESTS) {
                throw NetworkAuthException("Failed to prepare Firebase auth token")
            }
        }
    }
}

class Config {
    /** このクライアントで Firebase ID トークンを必須にするか（デフォルトfalse） */
    var requireIdToken: Boolean = false
}
