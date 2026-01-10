package com.company.primus2.autonomy.emotion

import com.company.primus2.core_ai.learn.Learner
import com.company.primus2.core_ai.model.EmotionState

class EmotionEngine {

    private val positive = setOf("ありがとう", "助かる", "最高", "嬉しい", "good", "great", "nice")
    private val negative = setOf("無理", "最悪", "ダメ", "エラー", "失敗", "困った", "怒")

    fun appraise(text: String, report: Learner.Report): EmotionState {
        val t = text.trim()
        val exclam = t.count { it == '!' || it == '！' }
        val qmark = t.count { it == '?' || it == '？' }
        val pos = positive.count { t.contains(it, ignoreCase = true) }
        val neg = negative.count { t.contains(it, ignoreCase = true) }

        val mood = (pos - neg).coerceIn(-3, 3) / 3f
        val arousal = ((exclam + qmark).coerceIn(0, 6)) / 6f

        return EmotionState(
            mood = mood,
            arousal = arousal
        )
    }
}