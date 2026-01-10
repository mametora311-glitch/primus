// ========================
// File: core_ai/memory/MemoryStore.kt
// Package: com.company.primus2.core_ai.memory
// ========================
package com.company.primus2.core_ai.memory


/**
 * メモリ永続/取得の境界インターフェース。core_ai では実装に依存しない。
 * app層のRoom実装/Realm実装はこのIFで差し替え可能。
 */
interface MemoryStore {
    fun upsert(item: MemoryItem)
    fun get(id: String): MemoryItem?
    fun remove(id: String): Boolean
    fun all(): List<MemoryItem>
    fun queryByTopics(topics: List<String>): List<MemoryItem>
}