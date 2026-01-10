package com.company.primus2.tts

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import com.company.primus2.core_ai.model.DispositionState
import com.company.primus2.core_ai.model.EmotionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TTSManager(app: Application) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(app, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.JAPANESE
            isTtsInitialized = true
            Log.i("Primus/TTS", "Google TTS engineの初期化に成功しました。")
        } else {
            Log.e("Primus/TTS", "Google TTS engineの初期化に失敗しました。")
        }
    }

    fun updateVoiceSettings(disposition: DispositionState, emotion: EmotionState) {
        if (!isTtsInitialized) return
        val basePitch = 0.8f + (disposition.warmth * 0.4f)
        val finalPitch = basePitch + (emotion.mood * 0.2f)
        val finalRate = 0.8f + (disposition.energy * 0.4f)
        tts?.setPitch(finalPitch.coerceIn(0.5f, 1.5f))
        tts?.setSpeechRate(finalRate.coerceIn(0.5f, 1.5f))
        Log.d("Primus/TTS", "Voice settings updated: pitch=$finalPitch, rate=$finalRate")
    }

    fun setEnabled(v: Boolean) {
        _enabled.value = v
        if (!v) stop()
    }

    fun speak(text: String) {
        if (!_enabled.value || text.isBlank() || !isTtsInitialized || _isSpeaking.value) return
        _isSpeaking.value = true
        val utteranceId = this.hashCode().toString()
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
    }
}