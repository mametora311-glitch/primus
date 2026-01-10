package com.company.primus2.tts

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

/**
 * 端末ネイティブの TextToSpeech を使った TTS 実装。
 * VoiceVox サーバーには依存しない。
 *
 * 内部で TTSManager (Google TTS) を利用しており、
 * 既存の ITts インターフェイスに適合させるアダプタ。
 */
class NativeTts(
    context: Context
) : ITts, Closeable {

    // Application コンテキストを使って TTSManager を初期化
    private val core = TTSManager(context.applicationContext as android.app.Application)

    override val isSpeaking: StateFlow<Boolean>
        get() = core.isSpeaking

    override suspend fun speak(text: String): Result<Unit> =
        runCatching { core.speak(text) }

    override fun stop() {
        core.stop()
    }

    override fun close() {
        core.shutdown()
    }
}
