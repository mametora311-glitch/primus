package com.company.primus2.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * UMP（ユーザー同意）ラッパ。
 * 必要地域のみ同意フォームを表示。未対象/不要/表示済みなら no-op。
 */
object UmpConsentManager {

    suspend fun requestIfRequired(context: Context, activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(context)

        // 最新の同意状態を取得（コールバック必須 → suspend 化）
        val updated = requestUpdate(consentInfo, activity, params)
        if (!updated) return  // 失敗時は静かに抜ける

        // フォームが必要ならロード→表示
        if (consentInfo.isConsentFormAvailable &&
            consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
        ) {
            val form = loadForm(context) ?: return
            suspendCancellableCoroutine<Unit> { cont ->
                form.show(activity) {
                    // 表示後に結果は SDK 内で保持される。ここでは復帰のみ。
                    cont.resume(Unit)
                }
            }
        }
    }

    /** requestConsentInfoUpdate を suspend 化 */
    private suspend fun requestUpdate(
        consentInfo: ConsentInformation,
        activity: Activity,
        params: ConsentRequestParameters
    ): Boolean = suspendCancellableCoroutine { cont ->
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            { cont.resume(true) },   // onSuccess
            { _ -> cont.resume(false) } // onError
        )
    }

    /** 同意フォームのロードを suspend 化 */
    private suspend fun loadForm(context: Context) =
        suspendCancellableCoroutine<com.google.android.ump.ConsentForm?> { cont ->
            UserMessagingPlatform.loadConsentForm(
                context,
                { form -> cont.resume(form) },
                { _ -> cont.resume(null) }
            )
        }
}
