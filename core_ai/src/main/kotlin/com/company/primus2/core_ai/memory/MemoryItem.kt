// File: core_ai/src/main/kotlin/com/company/primus2/core_ai/memory/MemoryItem.kt
package com.company.primus2.core_ai.memory

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 記憶レコード（純データ）。Renderer/Selector/Mapper に非依存。
 *
 * 不変条件:
 *  - importance は 0.0..1.0
 *  - kind == SEALED の場合は sealedReason を必須
 */
@Serializable
data class MemoryItem(
    val id: String,
    val kind: MemoryKind,
    val content: String,
    val tags: Set<String> = emptySet(),
    val importance: Double,   // 0.0〜1.0
    val emotions: Map<String, Double>,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long? = null,
    val references: List<String> = emptyList(),
    val sealedReason: String? = null
) {
    // ここでバリデーション（←トップレベルに if を置かないこと）
    init {
        require(importance in 0.0..1.0) { "importance must be within [0.0, 1.0]" }
        if (kind == MemoryKind.SEALED) {
            require(!sealedReason.isNullOrBlank()) { "SEALED memory requires sealedReason" }
        }
    }

    /** SEALED へ遷移（既にSEALEDならそのまま） */
    fun seal(reason: String): MemoryItem =
        if (kind == MemoryKind.SEALED) this else copy(kind = MemoryKind.SEALED, sealedReason = reason)

    /** 更新時刻だけ更新 */
    fun touch(nowEpochMs: Long): MemoryItem = copy(updatedAtEpochMs = nowEpochMs)

    /** 感情値を減衰（0..1 にクランプ）。decayRate: 0(変化なし)〜1(即0) */
    fun decayEmotions(decayRate: Double): MemoryItem {
        val r = decayRate.coerceIn(0.0, 1.0)
        return copy(
            emotions = emotions.mapValues { e -> (e.value * (1.0 - r)).coerceIn(0.0, 1.0) }
        )
    }

    /** importance を [0.0, 1.0] にクランプ反映 */
    fun withImportance(newImportance: Double): MemoryItem =
        copy(importance = newImportance.coerceIn(0.0, 1.0))

    /** JSON化（pretty可） */
    fun toJson(pretty: Boolean = false): String =
        if (pretty) Json { prettyPrint = true }.encodeToString(this)
        else Json.encodeToString(this)
}
