package com.company.primus2

/**
 * 会話のスタイル（口調）を担う簡易パーソナリティモデル。
 */
data class Personality(
    val style: Style = Style.Friendly
) {
    enum class Style { Friendly, Cool, Tsundere }

    fun isFriendly() = style == Style.Friendly
    fun isCool() = style == Style.Cool
    fun isTsundere() = style == Style.Tsundere
}