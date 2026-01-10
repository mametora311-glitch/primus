package com.company.primus2.summary

import android.util.Log
import com.company.primus2.memory.db.dao.MessageDao
import com.company.primus2.memory.db.entities.MessageEntity
import com.company.primus2.ui.util.TextGuards
import kotlin.math.min

class AutoSummarizer(
    private val dao: MessageDao,
    private val tailWindow: Int = 80,
    private val countThreshold: Int = 15,
    private val logger: (String) -> Unit = { msg -> Log.i("Primus", msg) }
) {
    suspend fun summarizeIfNeeded(sessionId: Long) {
        if (sessionId <= 0) return
        val all = dao.listBySession(sessionId)
        if (all.size < countThreshold) return

        val n = min(tailWindow, all.size)
        val text = all.take(n).reversed().joinToString("\n") { it.content }
        val summary = TextGuards.summarizeWithoutEcho(text)
        if (summary.isBlank()) return

        val now = System.currentTimeMillis()
        dao.insert(
            MessageEntity(
                sessionId = sessionId,
                role = "SUMMARY", // Role enumに合わせて大文字に
                content = summary,
                createdAt = now,
                updatedAt = now
            )
        )
        logger("summarize=ok sid=$sessionId len=${all.size} out=${summary.length}")
    }
}