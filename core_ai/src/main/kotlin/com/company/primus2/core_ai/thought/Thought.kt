// core_ai/src/main/kotlin/com/company/primus2/core_ai/thought/Thought.kt
package com.company.primus2.core_ai.thought

data class Thought(
    val id: String,
    val text: String,
    val score: Double = 0.5,                 // 0..1
    val rationale: String = "",              // 根拠
    val contextKeys: Set<String> = emptySet(),
    val proposedActions: List<String> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    init { require(score in 0.0..1.0) { "score must be within [0.0, 1.0]" } }

    fun withScore(newScore: Double): Thought =
        copy(score = newScore.coerceIn(0.0, 1.0))

    fun mergeContext(other: Thought): Thought =
        copy(contextKeys = this.contextKeys + other.contextKeys)

    fun addActions(vararg actions: String): Thought =
        copy(proposedActions = proposedActions + actions.filter { it.isNotBlank() })
}
