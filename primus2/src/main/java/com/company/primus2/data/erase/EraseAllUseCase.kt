package com.company.primus2.domain.erase

import android.content.Context
import com.company.primus2.data.erase.EraseRemoteDataSource
import com.company.primus2.data.erase.EraseRepository

/**
 * UI から呼ぶユースケース。
 * DI未導入でも単体で使えるよう、内部で最小限の生成を行う。
 */
class EraseAllUseCase(
    context: Context
) {
    private val repo = EraseRepository(EraseRemoteDataSource(context))

    /**
     * 実行。成功なら Result.success(Unit)。
     * @param deviceId 任意（匿名運用時の識別）
     */
    suspend operator fun invoke(deviceId: String? = null): Result<Unit> =
        repo.eraseAll(deviceId)
}
