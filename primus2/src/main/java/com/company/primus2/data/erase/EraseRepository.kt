package com.company.primus2.data.erase

/**
 * DataSource を包んで Result を返す薄いレイヤ。
 */
class EraseRepository(
    private val remote: EraseRemoteDataSource
) {
    suspend fun eraseAll(deviceId: String? = null): Result<Unit> = try {
        if (remote.eraseAll(deviceId)) Result.success(Unit)
        else Result.failure(IllegalStateException("eraseAll: server returned non-success code"))
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
