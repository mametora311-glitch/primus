package com.company.primus2.engine

import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.ReasoningResult

class ResponseEngine {
    private val policy: ClarificationPolicy = ClarificationPolicy()

    fun compose(result: ReasoningResult, emotion: EmotionState): String {
        return when (result.intent) {
            "answer_with_web" -> {
                val s = result.slots["summary"].orEmpty()
                if (s.isNotBlank()) s
                else "Wikipediaに該当する要約が見つかりませんでした。検索語を少し変えて再試行してください。"
            }
            "web_search" -> {
                "外部知識を探索します。"
            }
            "fix_build" -> {
                "ビルド問題ですね。次の順で進めます：${result.plan.joinToString(" → ")}。"
            }
            "dialog_forward" -> {
                policy.decideOne(result, emotion) ?: "この件で到達したい具体的なゴールを一文で教えてください。"
            }
            else -> {
                // ▼▼▼ Nullの可能性があるため、安全な呼び出しに修正 ▼▼▼
                (result.summary ?: "").ifBlank { "続けます。次にどう進めますか？" }
            }
        }
    }
}

class ClarificationPolicy {
    fun decideOne(result: ReasoningResult, emotion: EmotionState): String? {
        if (emotion.mood < -0.6f) return null
        return result.nextActions.firstOrNull() // missing -> nextActions に変更
    }
}