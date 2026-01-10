package com.company.primus2.core_ai

object StageFormatter {
    fun purpose(u: String) = "【目的】$u を達成する。"

    fun inputs(ctx: Map<String, String>): String {
        if (ctx.isEmpty()) return "【入力/素材】特に指定なし。必要に応じて追加入力を依頼。"
        val items = ctx.entries.joinToString("\n") { "- ${it.key}：${it.value}" }
        return "【入力/素材】\n$items"
    }

    fun constraints(): String = """
        【制約】
        - 安全/法令順守
        - 実行可能性を優先
        - 最小推測で厳密応答
        - 検索はデフォルト不使用
    """.trimIndent()

    fun confirm(): String = "【確認】不足があれば教えてください。次ステップへ進めます。"
}
