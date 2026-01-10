package com.company.primus2.engine

import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.UserInput

object ThinkingEngine {
    private val KEYWORDS_GRATITUDE = setOf("ありがとう", "感謝")
    private val KEYWORDS_INSULT = setOf("ばか", "バカ", "嫌い")
    private val KEYWORDS_CONCERN = setOf("元気?", "元気？", "大丈夫?", "大丈夫？")

    fun analyzeIntent(input: UserInput, emotion: EmotionState): String {
        val loweredText = input.text.lowercase()

        return when {
            KEYWORDS_GRATITUDE.any { loweredText.contains(it) } -> "gratitude"
            KEYWORDS_INSULT.any { loweredText.contains(it) } -> "insult"
            KEYWORDS_CONCERN.any { loweredText.contains(it) } -> "concern"
            emotion.mood < -0.7f -> "anger_response"
            emotion.mood > 0.7f -> "affection_response"
            else -> "neutral"
        }
    }
}