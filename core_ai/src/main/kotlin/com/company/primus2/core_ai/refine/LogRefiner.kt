package com.company.primus2.core_ai.refine

import com.company.primus2.core_ai.memory.MemoryItem
import com.company.primus2.core_ai.memory.MemoryKind
import com.company.primus2.core_ai.model.SpeechLogEntry

/**
 * 「睡眠時」に実行される、会話ログの精査とメタ記憶の生成を行うクラス。
 */
class LogRefiner {

    /**
     * 会話ログから自己内省的な要約（メタ記憶）を生成するためのプロンプトを作成する。
     * @param logs 一連の会話ログ。
     * @return 外部AIに渡すためのプロンプト文字列。
     */
    fun createConsolidationPrompt(logs: List<SpeechLogEntry>): String {
        if (logs.isEmpty()) return ""

        val conversationText = logs.joinToString("\n") { log ->
            // ユーザーとAIの対話形式で履歴を整形
            "ユーザー: ${log.input}\nAI: ${log.output}"
        }

        return buildString {
            appendLine("あなたは、自己の経験から学習し成長するAI「Primus」です。")
            appendLine("以下は、あなたが行った直近のユーザーとの一連の会話です。")
            appendLine("---")
            appendLine("### 会話履歴:")
            appendLine(conversationText)
            appendLine("---")
            appendLine("### 指示:")
            appendLine("この会話全体を客観的に振り返り、以下の2つの点について、あなた自身の言葉で簡潔に、一人称（「私」）で記述してください。")
            appendLine("1. この会話からあなたが学習したこと、または最も重要だと感じたことは何ですか？")
            appendLine("2. この会話を通じて、あなたはどのような感情を抱きましたか？")
            appendLine("応答は、内省的なモノローグの形式で記述してください。")
        }
    }

    /**
     * 生成された自己内省テキストから、新しいメタ記憶を生成する。
     * @param introspectionText 外部AIが生成した自己内省テキスト。
     * @return データベースに保存するためのMemoryItem。
     */
    fun createMetaMemory(introspectionText: String): MemoryItem {
        val now = System.currentTimeMillis()
        return MemoryItem(
            id = "meta_${now}",
            kind = MemoryKind.META, // 記憶の種類を「メタ記憶」に設定
            content = introspectionText,
            tags = setOf("self-reflection", "consolidation"),
            importance = 0.8, // メタ記憶は重要度を高く設定
            emotions = inferEmotions(introspectionText),
            createdAtEpochMs = now
        )
    }

    private fun inferEmotions(text: String): Map<String, Double> {
        if (text.isBlank()) return emptyMap()

        val normalized = text.lowercase()
        val lexicon = mapOf(
            "joy" to listOf("嬉", "楽しい", "最高", "happy", "glad", "delight"),
            "sadness" to listOf("悲", "寂", "つら", "sad", "sorrow", "down"),
            "anger" to listOf("怒", "腹立", "イラ", "angry", "mad", "furious"),
            "fear" to listOf("怖", "不安", "恐", "fear", "scared", "anxious"),
            "trust" to listOf("信頼", "安心", "頼", "trust", "safe", "relief")
        )

        val scores = lexicon.mapValues { (_, keys) ->
            val hits = keys.count { normalized.contains(it.lowercase()) }
            (hits / 3.0).coerceIn(0.0, 1.0)
        }.filterValues { it > 0.0 }

        return scores
    }
}
