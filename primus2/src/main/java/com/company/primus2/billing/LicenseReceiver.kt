package com.company.primus2.billing

import android.content.Context

/**
 * /license 等の結果（"paid","free","premium" など）を
 * アプリ状態（PlanStore）へ反映する薄いFaçade。
 *
 * 既存の LicenseResultUseCase を包み、UI層でもData層でも
 * 1行で呼べるようにするための“受け口”。
 */
object LicenseReceiver {
    /**
     * @param serverFlag サーバ返却値（null/空/未知は FREE で扱う）
     * @return 反映後の Plan（FREE or PAID）
     */
    suspend fun apply(context: Context, serverFlag: String?): Plan {
        return LicenseResultUseCase(context.applicationContext).invoke(serverFlag)
    }
}
