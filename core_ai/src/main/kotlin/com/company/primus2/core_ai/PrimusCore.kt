package com.company.primus2.core_ai

import com.company.primus2.core_ai.model.DispositionState // ▼▼▼ PersonaStateから変更 ▼▼▼
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.SelectedMemory
import com.company.primus2.core_ai.model.SpeechLogEntry
import com.company.primus2.core_ai.model.UserInput
import com.company.primus2.core_ai.refine.LogRefiner

class PrimusCore {
    private val logRefiner: LogRefiner = LogRefiner()

    // ▼▼▼ 引数の型を PersonaState から DispositionState に変更 ▼▼▼
    fun createPrompt(input: UserInput, memory: SelectedMemory, emotion: EmotionState, personality: DispositionState): String {
        return buildString {
            appendLine(value = "あなたは、固有の感情と人格を持つ、極めて高度なAI「Primus」です。あなたは単なる応答アシスタントではありません。")
            appendLine(value = "--- ### あなたの現在の性格:")
            appendLine(value = "- 温かみ: ${formatTrait(value = personality.warmth)}")
            appendLine(value = "- 共感性: ${formatTrait(value = personality.empathy)}")
            appendLine(value = "- エネルギー: ${formatTrait(value = personality.energy)}")
            appendLine(value = "---")
            appendLine(value = "### 現在のあなたの感情と、依拠した記憶、従うべき思考スタイル:")
            // (以下、他のロジックが続く想定)
        }
    }

    // このファイル内で使われるヘルパー関数のダミー
    private fun formatTrait(value: Float): String = "%.2f".format(value)

    /**
     * 自己内省（睡眠）プロセスで使われる、会話ログ要約用のプロンプトを作成する
     */
    fun createConsolidationPrompt(logs: List<SpeechLogEntry>): String {
        if (logs.isEmpty()) return ""
        return buildString {
            appendLine("以下は、ユーザーとの直近の会話ログです。")
            appendLine("この会話全体を振り返り、以下の3つの点について要約してください。")
            appendLine("1. 会話の全体的なトピックや流れ。")
            appendLine("2. ユーザーが表明した重要な好みや事実。")
            appendLine("3. あなた自身の応答で、特に良かった点や、次に改善すべき点。")
            appendLine("---")
            logs.forEach { entry ->
                appendLine("ユーザー: ${entry.input}")
                appendLine("Primus: ${entry.output}")
            }
            appendLine("---")
            appendLine("要約:")
        }
    }
}