package com.company.primus2.data.mapper

import com.company.primus2.core_ai.memory.MemoryItem
import com.company.primus2.core_ai.memory.MemoryKind
import com.company.primus2.core_ai.model.SelectedMemory
import com.company.primus2.memory.db.entities.MessageEntity

object SelectedMemoryMapper {
    fun fromMessages(list: List<MessageEntity>): SelectedMemory {
        val items = list.map { m ->
            MemoryItem(
                id = m.id.toString(),
                kind = MemoryKind.SHORT_TERM,
                content = m.content,             // ← text→content
                tags = emptySet(),
                importance = 1.0,
                emotions = emptyMap(),
                createdAtEpochMs = m.createdAt,
                updatedAtEpochMs = m.createdAt,
                references = listOf("session:${m.sessionId}")
            )
        }
        return SelectedMemory(items)
    }
}
