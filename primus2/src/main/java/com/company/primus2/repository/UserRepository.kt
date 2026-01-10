package com.company.primus2.repository

import com.company.primus2.data.UserData

interface UserRepository {
    suspend fun loginWithEmail(email: String, password: String): UserData?
    suspend fun loginWithGoogle(idToken: String): UserData?
    fun logout()
}


