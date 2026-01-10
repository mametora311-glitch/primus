package com.company.primus2.memory

import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.response.ResponseData

/**
 * 単純なインメモリ永続（アプリ再起動で消える）。
 */
object MemoryUnit {
    private val history = ArrayDeque<Pair<UserInput, ResponseData>>()
    private const val MAX = 200

    fun storeInteraction(input: UserInput, response: ResponseData) {
        if (history.size >= MAX) history.removeFirst()
        history.addLast(input to response)
    }

    fun dump(): List<Pair<UserInput, ResponseData>> = history.toList()
}