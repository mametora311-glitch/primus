package com.company.primus2.memory.summarize

import com.company.primus2.memory.db.entities.MessageEntity

class LightSummarizer {
    fun summarize(messages: List<MessageEntity>, maxChars: Int = 280): String {
        if (messages.isEmpty()) return "[SUM] (empty)"
        val sb = StringBuilder()
        for (m in messages.asReversed()) {
            val piece = "${m.role}: ${m.content}\n"
            if (sb.length + piece.length > maxChars) break
            sb.insert(0, piece)
        }
        return "[SUM]\n${sb.toString()}".trimEnd()
    }
}
