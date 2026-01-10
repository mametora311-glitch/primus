package com.company.primus2.reason

import android.util.Log
import com.company.primus2.core_ai.model.EmotionState
import com.company.primus2.core_ai.model.KnowledgeBase
import com.company.primus2.core_ai.model.ReasoningResult
import com.company.primus2.core_ai.model.UserInput

/**
 * 入力 → 意図/計画 への軽量ルール推定。
 */
class Reasoner(
    private val logger: (String) -> Unit = { s -> Log.d("Primus", s) }
) {

    fun reason(
        input: UserInput,
        emotion: EmotionState,
        kb: Map<String, List<KnowledgeBase.Fact>>?
    ): ReasoningResult {
        val text = input.text.trim()
        if (text.isBlank()) {
            return ReasoningResult(intent = "noop", summary = "empty input")
        }

        // 1. 明示的な検索指示を検出
        Regex("""^(調べて|検索|search)\s*[:：]?\s*(.+)$""").find(text)?.let { m ->
            val q = m.groupValues[2].trim()
            if (q.isNotEmpty()) {
                return ReasoningResult(
                    intent = "web_search",
                    slots = mapOf("query" to q, "lang" to pickLang(text)),
                    summary = "明示検索"
                )
            }
        }

        // 2. 疑問形で、かつ固有名詞や専門用語が含まれる場合に、暗黙的な検索意図を推論
        if (text.endsWith("？") || text.endsWith("?")) {
            // （簡易的な実装として、「〜とは？」という形式を検索トリガーとします）
            Regex("""(.+?)\s*(とは|について教えて)""").find(text)?.let { m ->
                val q = m.groupValues[1].trim()
                if (q.isNotEmpty()) {
                    return ReasoningResult(
                        intent = "web_search",
                        slots = mapOf("query" to q, "lang" to pickLang(text)),
                        summary = "暗黙的な知識検索"
                    )
                }
            }
        }

        // 3. ビルド/コード関連の意図を推論
        if (Regex("""(?i)\b(agp|gradle|android|ksp|compose|room|Unresolved|Cannot)\b""").containsMatchIn(text)) {
            return ReasoningResult(
                intent = "fix_build",
                plan = listOf("エラーブロックを抽出", "plugins/versionsの突合", "Sync→Rebuild"),
                summary = text.take(200)
            )
        }

        // 上記のいずれにも当てはまらない場合は、通常の対話継続
        return ReasoningResult(
            intent = "dialog_forward",
            summary = text.take(200)
        )
    }

    private fun pickLang(text: String): String =
        if (Regex("""\p{IsHan}|\p{IsHiragana}|\p{IsKatakana}""").containsMatchIn(text)) "ja" else "en"
}