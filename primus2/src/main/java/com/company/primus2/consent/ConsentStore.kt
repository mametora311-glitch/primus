package com.company.primus2.consent

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("primus_prefs")
private val KEY_CONSENT = booleanPreferencesKey("consent_allowed")

class ConsentStore(private val appContext: Context) {
    val allowedFlow: Flow<Boolean> =
        appContext.dataStore.data.map { prefs -> prefs[KEY_CONSENT] ?: false }

    suspend fun setAllowed(allowed: Boolean) {
        appContext.dataStore.edit { it[KEY_CONSENT] = allowed }
    }
}
