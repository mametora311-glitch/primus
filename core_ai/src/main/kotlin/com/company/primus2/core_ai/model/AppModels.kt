package com.company.primus2.core_ai.model

import android.content.Context
import com.company.primus2.core_ai.memory.MemoryItem

// --- 人格・感情モデル ---
data class DispositionState(val energy: Float, val warmth: Float, val empathy: Float)
data class EmotionState(val mood: Float = 0f, val arousal: Float = 0f)

// --- AIからの最終出力 ---
data class FinalOutput(
    val text: String,
    val trace: DebugTrace,
    val disposition: DispositionState,
    val emotion: EmotionState
)

// --- その他のモデル ---
data class ChatMessage(val role: String, val content: String)
data class UserInput(val text: String, val history: List<ChatMessage> = emptyList())
data class ReasoningResult(val intent: String, val summary: String? = null, val slots: Map<String, String> = emptyMap(), val plan: List<String> = emptyList(), val confidence: Float = 1.0f, val trace: String? = null, val nextActions: List<String> = emptyList())
object KnowledgeBase {
    data class Fact(val source: String, val key: String, val summary: String, val importance: Double)
    fun snapshot(_context: Context): Map<String, List<Fact>> = emptyMap()
    fun add(_context: Context, _source: String, _key: String, _summary: String, _importance: Double) { /* TODO */ }
}
data class SelectedMemory(val items: List<MemoryItem>) {
    // ▼▼▼ ここのアンダースコアを削除しました ▼▼▼
    companion object { val EMPTY = SelectedMemory(emptyList()) }
}
data class DebugTrace(val selectedIds: List<String>)
data class SpeechLogEntry(val input: String, val output: String, val selectedIds: List<String>, val ts: Long)
data class RefineLog(val summary: String, val newFacts: List<String>, val personaShift: String, val confidence: Float)