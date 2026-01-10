package com.company.primus2.autonomy

import com.company.primus2.consent.ConsentStore
import kotlinx.coroutines.flow.first

/** Gate は suspend で問い合わせる */
interface ConsentGate {
    suspend fun isAllowed(): Boolean
}

/** DataStore バッキング実装 */
class StoreBackedConsentGate(
    private val store: ConsentStore
) : ConsentGate {
    override suspend fun isAllowed(): Boolean = store.allowedFlow.first()
}
