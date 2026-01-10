package com.company.primus2.core_ai.learn

import com.company.primus2.core_ai.model.UserInput

object Learner {

    data class Report(val noted: List<String> = emptyList())

    fun observe(input: UserInput): Report {
        val text = input.text
        val findings = mutableListOf<String>()

        // 1. 「好き」「好み」を検出する (既存のロジック)
        Regex("""(好き|好み)[は|:：]\s*([^\n]+)""").findAll(text).forEach { match ->
            val value = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (value.isNotEmpty()) findings.add("user_like=$value")
        }

        // ▼▼▼ ここからが新しい信念学習ロジック ▼▼▼

        // 2. 「私の〇〇は××です」という形式の事実を検出
        Regex("""私の(.+?)[は|:：]\s*(.+?)(です|だよ|だ)""").findAll(text).forEach { match ->
            val key = match.groupValues.getOrNull(1)?.trim()?.replace(" ", "_") // "猫 の 名前" -> "猫_の_名前"
            val value = match.groupValues.getOrNull(2)?.trim()
            if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) {
                findings.add("belief:user_$key=$value")
            }
        }

        // 3. 「〇〇に住んでいます」という形式の事実を検出
        Regex("""(.+?)に住んでいます""").findAll(text).forEach { match ->
            val value = match.groupValues.getOrNull(1)?.trim()
            if (!value.isNullOrEmpty()) {
                findings.add("belief:user_location=$value")
            }
        }

        // ▲▲▲ 修正ここまで ▲▲▲

        return Report(noted = findings)
    }
}