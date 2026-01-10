package com.company.primus2.ui.model

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
)


