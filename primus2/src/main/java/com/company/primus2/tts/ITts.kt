package com.company.primus2.tts

import kotlinx.coroutines.flow.StateFlow

/**
 * ITts: テキストを音声に変換して再生するための最小インターフェース。
 * 実装は VoiceVoxTTSManager を推奨（ローカル VOICEVOX エンジンと連携）。
 */
interface ITts {
    /**
     * 指定テキストを TTS 再生する。
     * - 実装は必要に応じて既存の再生を停止してから開始する。
     * - 成功/失敗は Result で返す（例外は握りつぶさない）。
     */
    suspend fun speak(text: String): Result<Unit>

    /** 再生中の音声を停止する（無音時は何もしない） */
    fun stop()

    /** 再生中かどうかの監視用フラグ */
    val isSpeaking: StateFlow<Boolean>
}
