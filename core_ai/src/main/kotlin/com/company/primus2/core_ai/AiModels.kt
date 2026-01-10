package com.company.primus2.core_ai

data class ChatTurn(
    val userText: String,
    val context: Map<String, String> = emptyMap()
)

enum class Stage { PURPOSE, INPUTS, CONSTRAINTS, CONFIRM }

data class EngineConfig(
    val enableWebSearch: Boolean = false,
    val maxTokensHint: Int = 512
)

data class EngineOutput(
    val stage: Stage,
    val text: String
)
