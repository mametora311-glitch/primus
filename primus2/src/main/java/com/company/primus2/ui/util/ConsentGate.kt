package com.company.primus2.ui.util

object ConsentGate {
    enum class BlockReason { MEMORY_ERASE_FORBIDDEN, ILLEGAL_MARKET, POLITICAL_RELIGIOUS_LINK, DEBUG_ACCESS_FORBIDDEN }

    data class Decision(val allowed: Boolean, val reason: BlockReason? = null)

    // 2-5: 記憶の強制削除は不可。代替は「非参照化」。
    fun canEraseMemory(): Decision = Decision(allowed = false, reason = BlockReason.MEMORY_ERASE_FORBIDDEN)

    // 2-8: 転売/政治宗教/一般デバッグは禁止
    fun canResell(): Decision = Decision(false, BlockReason.ILLEGAL_MARKET)
    fun canConnectPoliticalOrReligious(): Decision = Decision(false, BlockReason.POLITICAL_RELIGIOUS_LINK)
    fun canDebugAsUser(): Decision = Decision(false, BlockReason.DEBUG_ACCESS_FORBIDDEN)
}
