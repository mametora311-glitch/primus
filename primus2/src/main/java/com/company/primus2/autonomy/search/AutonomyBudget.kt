package com.company.primus2.autonomy.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStoreのインスタンスをトップレベルで定義
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autonomy_settings")

class AutonomyBudget(private val context: Context) {

    private val budgetKey = intPreferencesKey("autonomy_budget")

    suspend fun check(): Boolean {
        val currentBudget = context.dataStore.data
            .map { preferences ->
                preferences[budgetKey] ?: 10 // デフォルト値
            }.first()
        return currentBudget > 0
    }

    suspend fun consume(cost: Int) {
        context.dataStore.edit { settings ->
            val currentBudget = settings[budgetKey] ?: 10
            settings[budgetKey] = currentBudget - cost
        }
    }
}
