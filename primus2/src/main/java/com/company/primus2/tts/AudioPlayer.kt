package com.company.primus2.tts

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * WAV/PCM のローカルファイルをシンプルに再生する小さなラッパー。
 * - MediaPlayer のライフサイクルを安全に扱い、二重再生を防ぐ。
 * - 再生完了/停止時に isSpeaking を false に更新する。
 */
class AudioPlayer(private val appContext: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    /** 既存の再生を停止して解放 */
    fun stop() {
        _isSpeaking.value = false
        mediaPlayer?.let { mp ->
            try {
                mp.stop()
            } catch (_: Throwable) { /* no-op */ }
            try {
                mp.reset()
                mp.release()
            } catch (_: Throwable) { /* no-op */ }
        }
        mediaPlayer = null
    }

    /**
     * 指定ファイルを再生する。ファイルは WAV など MediaPlayer が認識できる形式とする。
     */
    fun playFile(file: File): Result<Unit> = runCatching {
        stop()
        val mp = MediaPlayer()
        mediaPlayer = mp
        mp.setDataSource(file.absolutePath)
        mp.setOnCompletionListener {
            _isSpeaking.value = false
            stop()
        }
        mp.setOnPreparedListener {
            _isSpeaking.value = true
            it.start()
        }
        mp.prepareAsync()
    }
}
