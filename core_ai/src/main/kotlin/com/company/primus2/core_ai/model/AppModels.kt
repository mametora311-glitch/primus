package com.company.primus2.core_ai.model

import android.content.Context
import com.company.primus2.core_ai.memory.MemoryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
    private const val PREFS_NAME = "primus_kb"
    private const val PREFS_KEY = "facts_json"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Fact(val source: String, val key: String, val summary: String, val importance: Double)

    @Serializable
    private data class FactSnapshot(val facts: Map<String, List<Fact>>)

    fun snapshot(context: Context): Map<String, List<Fact>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(PREFS_KEY, null) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(FactSnapshot.serializer(), raw).facts
        }.getOrDefault(emptyMap())
    }

    fun add(context: Context, source: String, key: String, summary: String, importance: Double) {
        if (source.isBlank() || key.isBlank() || summary.isBlank()) return
        val current = snapshot(context).toMutableMap()
        val updated = (current[source] ?: emptyList())
            .filterNot { it.key == key }
            .toMutableList()
        updated.add(Fact(source = source, key = key, summary = summary, importance = importance))
        current[source] = updated.sortedByDescending { it.importance }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val payload = json.encodeToString(FactSnapshot.serializer(), FactSnapshot(current))
        prefs.edit().putString(PREFS_KEY, payload).apply()
    }
}
data class SelectedMemory(val items: List<MemoryItem>) {
    // ▼▼▼ ここのアンダースコアを削除しました ▼▼▼
    companion object { val EMPTY = SelectedMemory(emptyList()) }
}
data class DebugTrace(val selectedIds: List<String>)
data class SpeechLogEntry(val input: String, val output: String, val selectedIds: List<String>, val ts: Long)
data class RefineLog(val summary: String, val newFacts: List<String>, val personaShift: String, val confidence: Float)
