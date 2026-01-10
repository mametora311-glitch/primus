package com.company.primus2.billing

/**
 * 課金プラン。FREE は学習・自律・音声などを制限。
 */
enum class Plan {
    FREE,
    PAID
}

/** 拡張: 無料/有料の簡易判定 */
val Plan.isFree: Boolean get() = this == Plan.FREE
val Plan.isPaid: Boolean get() = this == Plan.PAID
