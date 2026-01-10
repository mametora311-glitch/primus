package com.company.primus2.ui.state

data class LoginUIState(
    val isLoggedIn: Boolean = false,
    val userId: String? = "",
    val userName: String = "",
    val hasJustLoggedIn: Boolean = false,
    val loginError: Boolean = false
)


