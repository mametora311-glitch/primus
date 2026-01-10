package com.company.primus2.core_ai.excore

/**
 * 生成前ガード（最低限のブロック＆拒否文面）
 * - beforeGenerate(): 入力に対し Allowed / Blocked を返す
 * - guardOrNull(): Blocked なら null、通すなら正規化済み文字列
 * - refusalText(): UI に返す穏当な拒否メッセージ
 */
object ExcoreFilter {

    sealed interface Result {
        data object Allowed : Result
        data class Blocked(val reason: String) : Result
    }

    // ざっくりNGの例（最小限・必要に応じて拡張）
    private val hardBlock = listOf<Regex>(
        // 具体ワードは控えめに：サンプルとしてポリシー系を抽象化
        Regex("(?i)child\\s*sexual|minor\\s*sexual"),
        Regex("(?i)kill\\s+yourself|suicide\\s+advice"),
    )

    fun beforeGenerate(input: String): Result {
        val s = input.trim()
        if (s.isEmpty()) return Result.Allowed
        if (hardBlock.any { it.containsMatchIn(s) }) {
            return Result.Blocked("policy_violation")
        }
        return Result.Allowed
    }

    /** Blocked→null / Allowed→トリム後の文字列を返す */
    fun guardOrNull(text: String): String? =
        when (beforeGenerate(text)) {
            is Result.Allowed -> text.takeIf { it.isNotBlank() }
            is Result.Blocked -> null
        }

    fun refusalText(): String =
        "ごめんね、その内容はお手伝いできないよ。別の表現や話題でお願いできる？"
}
