package com.company.primus2.wipe

import android.content.Context
import androidx.room.withTransaction
import com.company.primus2.billing.Plan
import com.company.primus2.billing.PlanStore
import com.company.primus2.consent.ConsentStore
import com.company.primus2.memory.db.PrimusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 端末側の「ユーザーに見えている表面データのみ」を消去（初期化）するユースケース。
 * サーバ上の記憶データには一切触れない。
 *
 * 対象：
 *  - 同意状態：OFFへ
 *  - 課金プラン表示：FREEへ
 *  - Roomの表示系テーブル：messages → sessions → goals → beliefs → personality を全削除
 *
 * 対象外：
 *  - サーバ上の記憶データ：不変更
 *  - device_id：保持（匿名識別のため）
 */
class LocalWipeUseCase(
    private val appContext: Context
) {

    /**
     * 実行。成功なら Result.success(Unit)。
     */
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1) 同意をOFFへ
            ConsentStore(appContext).setAllowed(false)

            // 2) プラン表示をFREEへ（UI分岐の既定化）
            PlanStore.set(appContext, Plan.FREE)

            // 3) ローカルDBを初期化（ユーザに見える記憶のみ）— トランザクションで一括
            val db = PrimusDatabase.get(appContext)
            db.withTransaction {
                // 外部キー整合のため削除順序を固定
                db.messageDao().deleteAll()
                db.sessionDao().deleteAll()
                db.goalDao().deleteAll()
                db.beliefDao().deleteAll()
                db.personalityDao().deleteAll()
            }

            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
