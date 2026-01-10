package com.company.primus2.firebase

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Firebase IDトークンを定期的に取得して温存する軽量リフレッシャ。
 * - google-services.json が無い環境では内部で自動 no-op
 * - 失敗しても握りつぶし、既存挙動に影響しない
 */
object TokenRefresher {
    /** 50分間隔（Firebaseの推奨更新目安に合わせる／必要なら調整） */
    private const val INTERVAL_MINUTES: Long = 50

    fun start(context: Context, scope: CoroutineScope): Job =
        scope.launch {
            // 起動時に一度サインインを確保
            try { FirebaseAuthManager.ensureAnonymousSignIn(context) } catch (_: Throwable) {}
            while (isActive) {
                try {
                    // 失効を避けるため定期的にトークンを更新（forceRefresh=true）
                    FirebaseAuthManager.getIdToken(context, forceRefresh = true)
                } catch (_: Throwable) { /* ignore */ }
                // 次回まで待機
                delay(INTERVAL_MINUTES * 60 * 1000)
            }
        }
}
