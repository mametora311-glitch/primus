package com.company.primus2.data

import kotlinx.serialization.Serializable

@Serializable
enum class Role { USER, AI }

@Serializable
data class ChatMessage(
    val role: Role,
    val text: String,
    val ts: Long = System.currentTimeMillis()
)