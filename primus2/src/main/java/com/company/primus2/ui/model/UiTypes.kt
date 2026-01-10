package com.company.primus2.ui.model

data class UiMessage(val role: Role, val text: String) {
    enum class Role { USER, AI }
}