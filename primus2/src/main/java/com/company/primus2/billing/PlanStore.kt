package com.company.primus2.billing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.planDataStore by preferencesDataStore(name = "plan_store")

/**
 * プランの恒常化（FREE/PAID）。未設定時は FREE 既定。
 */
object PlanStore {
    private val KEY_PLAN = stringPreferencesKey("plan")

    fun observe(context: Context): Flow<Plan> =
        context.planDataStore.data.map { prefs ->
            when (prefs[KEY_PLAN]) {
                "PAID" -> Plan.PAID
                else   -> Plan.FREE
            }
        }

    suspend fun set(context: Context, plan: Plan) {
        context.planDataStore.edit { prefs ->
            prefs[KEY_PLAN] = if (plan == Plan.PAID) "PAID" else "FREE"
        }
    }
}
