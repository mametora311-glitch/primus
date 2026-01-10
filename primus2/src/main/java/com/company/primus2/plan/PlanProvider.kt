package com.company.primus2.plan

data class PlanInfo(
    val plan: String,                 // "free" | "pro"
    val learningToggleEnabled: Boolean
)

/** 当面はスタブ固定。Play Billing実装後に置換 */
object PlanProvider {
    fun current(): PlanInfo = PlanInfo(plan = "free", learningToggleEnabled = false)
}
