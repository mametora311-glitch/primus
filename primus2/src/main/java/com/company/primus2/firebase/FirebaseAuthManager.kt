package com.company.primus2.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {

    /** google-services.json があり初期化できているか */
    fun isConfigured(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) return true
            FirebaseApp.initializeApp(context) != null
        } catch (_: Throwable) {
            // 設定がおかしくても「未設定扱い」に倒す（クラッシュさせない）
            false
        }
    }

    fun isAvailable(context: Context): Boolean = isConfigured(context)

    /**
     * 匿名サインイン。
     * 失敗しても null を返すだけで、絶対に例外は表に出さない。
     */
    suspend fun ensureAnonymousSignIn(context: Context): FirebaseUser? {
        if (!isAvailable(context)) return null
        return try {
            val auth = FirebaseAuth.getInstance()
            val current = auth.currentUser
            if (current != null) return current
            auth.signInAnonymously().await().user
        } catch (_: Throwable) {
            // APIキー不整合など、どんな理由でも「サインインなし」で続行
            null
        }
    }

    /**
     * IDトークン取得。失敗時は null を返す。
     */
    suspend fun getIdToken(context: Context, forceRefresh: Boolean = false): String? {
        if (!isAvailable(context)) return null
        return try {
            val user = ensureAnonymousSignIn(context) ?: return null
            user.getIdToken(forceRefresh).await().token
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 既にサインイン済みならトークンを取る。未サインインなら何もしない。
     */
    suspend fun getIdTokenIfInitialized(forceRefresh: Boolean = false): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return null
            user.getIdToken(forceRefresh).await().token
        } catch (_: Throwable) {
            null
        }
    }
}
