package com.company.primus2.billing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * /license 等のサーバ返却値（フラグ文字列）を受け取り、
 *  - 正規化（FREE/PAID）
 *  - PlanStore へ保存（UIに即反映）
 * を一括で行うユースケース。
 *
 * 既存の LicenseBridge を薄く包んでいるだけなので、
 * 呼び出し側はこの UseCase を使えば良い。
 *
 * 使い方：
 *   val uc = LicenseResultUseCase(appContext)
 *   uc("paid")  // → Plan.PAID に保存
 *   uc(null)    // → Plan.FREE に保存
 */
class LicenseResultUseCase(
    private val appContext: Context
) {
    /**
     * @param serverFlag サーバから返るライセンス状態（例："paid","pro","premium" など）
     *                   null/空/未知の値は FREE として扱う（LicenseBridge.normalize に準拠）
     * @return 保存後に確定した Plan（UI は PlanProvider/LocalPlan 経由で即反映される）
     */
    suspend operator fun invoke(serverFlag: String?): Plan = withContext(Dispatchers.IO) {
        val plan = LicenseBridge.normalize(serverFlag)
        LicenseBridge.setPlan(appContext, plan)
        plan
    }
}
