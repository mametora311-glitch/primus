// ========================
// File: core_ai/memory/InMemoryMemoryStore.kt
// Package: com.company.primus2.core_ai.memory
// ========================
package com.company.primus2.core_ai.memory


class InMemoryMemoryStore : MemoryStore {
    private val data: MutableMap<String, MemoryItem> = mutableMapOf()
    private val frozen: MutableSet<String> = mutableSetOf()


    override fun upsert(item: MemoryItem) {
        data[item.id] = item
    }


    override fun get(id: String): MemoryItem? = data[id]


    override fun remove(id: String): Boolean = data.remove(id) != null


    override fun all(): List<MemoryItem> = data.values.toList()


    /**
     * タグ/コンテンツに topics のいずれかが含まれるメモリを重要度順で返す。
     * Grok 由来の `first.weight` 参照を `importance` に正規化。
     */
    override fun queryByTopics(topics: List<String>): List<MemoryItem> {
        if (topics.isEmpty()) return emptyList()
        val lowers = topics.map { it.lowercase() }.toSet()


        return data.values
            .asSequence()
            .filter { item ->
                item.id !in frozen && (
                        lowers.any { t -> item.content.lowercase().contains(t) } ||
                                item.tags.any { tag -> lowers.any { t -> t in tag.lowercase() || tag.lowercase() in t } }
                        )
            }
            .map { item ->
                val overlap = item.tags.count { tag -> lowers.any { t -> t in tag.lowercase() || tag.lowercase() in t } }
                item to overlap
            }
            .sortedWith(compareByDescending<Pair<MemoryItem, Int>> { it.second }
                .thenByDescending { it.first.importance }
                .thenByDescending { it.first.updatedAtEpochMs })
            .map { it.first }
            .toList()
    }
}