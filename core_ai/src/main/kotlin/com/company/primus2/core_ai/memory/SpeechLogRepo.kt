package com.company.primus2.core_ai.memory

import com.company.primus2.core_ai.model.SpeechLogEntry

interface SpeechLogRepo {
    fun append(entry: SpeechLogEntry)
    fun getAll(): List<SpeechLogEntry>
    fun clear()
}

class InMemorySpeechLogRepo : SpeechLogRepo {
    private val buf = mutableListOf<SpeechLogEntry>()
    override fun append(entry: SpeechLogEntry) { buf += entry }
    override fun getAll(): List<SpeechLogEntry> = buf.toList()
    override fun clear() { buf.clear() }
}
