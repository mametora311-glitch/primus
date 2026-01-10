package com.company.primus2.billing

import android.content.Context

/**
 * /license 等のサーバ結果をアプリ内の課金プランへ反映するブリッジ。
 *
 * 想定するサーバ値の例（大小区別せず処理）:
 *  - "PAID", "paid", "pro", "premium" など → Plan.PAID
 *  - null, "", "FREE", "trial", その他 → Plan.FREE
 *
 * 使い方:
 *  suspend fun somewhere() {
 *      LicenseBridge.apply(appContext, serverFlag) // serverFlag はAPIの返却値など
 *  }
 */
object LicenseBridge {

    /** サーバからのフラグ文字列を Plan に正規化 */
    fun normalize(flag: String?): Plan {
        val f = flag?.trim()?.lowercase().orEmpty()
        return when (f) {
            "paid", "pro", "premium", "plus", "tier2", "tier3" -> Plan.PAID
            else -> Plan.FREE
        }
    }

    /** 直接 Plan を保存（UIへ即時配布される：PlanProvider 経由） */
    suspend fun setPlan(context: Context, plan: Plan) {
        PlanStore.set(context, plan)
    }

    /** サーバ値を受け取り → 正規化 → 保存 までを一括実行 */
    suspend fun apply(context: Context, serverFlag: String?) {
        val plan = normalize(serverFlag)
        setPlan(context, plan)
    }
}
