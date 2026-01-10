package com.company.primus2.repository

import com.company.primus2.data.UserData

/**
 * Firebase 未リンク環境でもビルドが通るよう、
 * ここではダミー実装を提供。Firebase を導入後、実装を差し替えてください。
 */
class FirestoreUserRepository : UserRepository {
    override suspend fun loginWithEmail(email: String, password: String): UserData? {
        // FirebaseAuth 等を導入したら差し替え
        return null
    }

    override suspend fun loginWithGoogle(idToken: String): UserData? {
        // GoogleAuthProvider 等を導入したら差し替え
        return null
    }

    override fun logout() { /* no-op */ }
}