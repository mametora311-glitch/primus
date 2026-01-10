package com.company.primus2.core_ai.persona

import android.util.Log
import com.company.primus2.core_ai.learn.Learner
import com.company.primus2.core_ai.model.DispositionState
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.core_ai.repository.PersonaRepository

class PersonalityEngine(private val repository: PersonaRepository) {

    private var dispositionState = DispositionState(energy = 0.5f, warmth = 0.5f, empathy = 0.5f)
    private val logTag = "Primus/Personality"

    private fun clamp(v: Float) = v.coerceIn(0f, 1f)
    private fun step(cur: Float, delta: Float) = clamp(cur + delta)

    suspend fun loadPersona() {
        val savedState = repository.getPersonality()
        if (savedState != null) { // ▼▼▼ `nil` を `null` に修正 ▼▼▼
            dispositionState = savedState
        } else {
            repository.savePersonality(dispositionState)
        }
        Log.i(logTag, "人格傾向をロードしました: $dispositionState")
    }

    suspend fun savePersona() {
        repository.savePersonality(dispositionState)
        Log.i(logTag, "人格傾向を保存しました: $dispositionState")
    }

    fun analyzeAndUpdate(input: UserInput, aiResponse: String, report: Learner.Report): EmotionState {
        val pos = listOf("ありがとう", "嬉", "助か", "すごい").count { input.text.contains(it) || aiResponse.contains(it) }
        val neg = listOf("最悪", "ムカ", "嫌", "ダメ").count { input.text.contains(it) || aiResponse.contains(it) }
        val mood = (pos - neg) * 0.1f
        val emotionState = EmotionState(mood = mood, arousal = 0.1f)

        var updatedDisposition = dispositionState
        if (pos > 0) { updatedDisposition = updatedDisposition.copy(warmth = step(updatedDisposition.warmth, 0.005f)) }
        if (neg > 0) { updatedDisposition = updatedDisposition.copy(warmth = step(updatedDisposition.warmth, -0.01f)) }
        if (report.noted.any { it.startsWith("like=") }) {
            updatedDisposition = updatedDisposition.copy(
                warmth = step(updatedDisposition.warmth, 0.01f),
                empathy = step(updatedDisposition.empathy, 0.005f)
            )
        }
        if (dispositionState != updatedDisposition) {
            dispositionState = updatedDisposition
            Log.d(logTag, "人格傾向が更新されました: $dispositionState")
        }
        return emotionState
    }

    fun getCurrentDisposition(): DispositionState = dispositionState
}