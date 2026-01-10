package com.company.primus2.learn

import com.company.primus2.repository.PrimusRepository
import com.company.primus2.ui.util.TextGuards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepManager(private val repository: PrimusRepository) {

    @Volatile private var lastDay: String? = null

    suspend fun trySleepConsolidate(): Boolean = withContext(Dispatchers.IO) {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (day == lastDay) return@withContext false

        val all = repository.getLatestMessages(limit = 1000)
        if (all.size < 20) { lastDay = day; return@withContext false }

        val text = all.joinToString("\n") { it.content }
        val summary = TextGuards.summarizeWithoutEcho(text)
        if (summary.isNotBlank()) {
            // TODO: 要約メッセージを保存するロジックを実装する必要がある
            // repository.insertMessage(...)
            lastDay = day
            true
        } else false
    }
}
