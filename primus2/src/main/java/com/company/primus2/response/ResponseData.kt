package com.company.primus2.response

data class ResponseData(
    val output: String,
    val intent: String = "",
    val slots: Map<String, String> = emptyMap(),
    val summary: String? = null
)
