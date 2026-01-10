package com.company.primus2.net

import kotlinx.coroutines.delay

/**
 * 超軽量の指数バックオフ。408/5xx/Timeout など一時障害に強くなる。
 */
suspend inline fun <T> withRetry(times: Int = 3, baseDelayMs: Long = 300, block: suspend () -> T): T {
    var last: Throwable? = null
    repeat(times) { attempt ->
        try {
            return block()
        } catch (t: Throwable) {
            last = t
            val delayMs = baseDelayMs shl attempt
            delay(delayMs)
        }
    }
    throw last ?: IllegalStateException("withRetry: unknown failure")
}
