package com.company.primus2.tts

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

/**
 * 互換レイヤ：既存コードで isSynthesizing / shutdown() を参照している場合の吸収。
 */

/** 旧名称への別名（エイリアス的に提供） */
val ITts.isSynthesizing: StateFlow<Boolean>
    get() = this.isSpeaking

/** リソース解放の互換 API。Closeable 実装なら close()、そうでなければ stop() のみ。 */
fun ITts.shutdown() {
    when (this) {
        is Closeable -> try { close() } catch (_: Throwable) {}
        else -> try { stop() } catch (_: Throwable) {}
    }
}
