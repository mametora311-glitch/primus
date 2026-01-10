package com.company.primus2.firebase

import android.content.Context
import com.company.primus2.billing.LicenseReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Firestore の users/{uid} を監視して plan 文字列を受け取り、
 * FREE/PAID に正規化してアプリへ即反映する同期器。
 *
 * 要件:
 * - google-services.json が配置済み
 * - 匿名 or 既存ユーザーで FirebaseAuth に currentUser がいる
 *
 * ない場合は安全に no-op。
 */
object LicenseSync {

    /**
     * 監視を開始。scope が cancel されると自動停止。
     */
    fun start(context: Context, scope: CoroutineScope): Job {
        val appCtx = context.applicationContext
        val job = Job()
        scope.launch(job) {
            try {
                // Firebase 未設定なら何もしない
                if (!FirebaseAuthManager.isConfigured(appCtx)) {
                    return@launch
                }
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser ?: run {
                    // まだログインしていなければ匿名で確保（失敗してもno-op）
                    FirebaseAuthManager.ensureAnonymousSignIn(appCtx)
                }

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("users").document(uid)

                // SnapshotListener を張る。Job cancel 時に remove する。
                val registration = docRef.addSnapshotListener { snap, _ ->
                    val planFlag = snap?.getString("plan")
                    // 例: "paid" / "pro" / null など → FREE/PAID へ正規化して保存
                    scope.launch {
                        try {
                            LicenseReceiver.apply(appCtx, planFlag)
                        } catch (_: Throwable) {
                            // 反映失敗は握りつぶし（UIを壊さない）
                        }
                    }
                }

                // このジョブがキャンセルされたらListener解除
                job.invokeOnCompletion { registration.remove() }
            } catch (_: Throwable) {
                // 例外は握りつぶし（no-op）
            }
        }
        return job
    }
}
