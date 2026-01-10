package com.company.primus2.billing

import android.content.Context
import com.company.primus2.net.ProxyClient

/**
 * /license を叩いて plan 文字列を取得し、FREE/PAID に正規化 → PlanStore に保存するユースケース。
 * ProxyClient のインスタンスは呼び出し側から渡す（既存生成を流用）。
 */
class LicenseFetchUseCase(
    private val appContext: Context,
    private val client: ProxyClient
) {
    suspend operator fun invoke(): Result<Plan> = runCatching {
        val resp = client.fetchLicense()      // -> LicenseResp(plan:String?)
        val plan = LicenseReceiver.apply(appContext, resp.plan)
        plan
    }
}
