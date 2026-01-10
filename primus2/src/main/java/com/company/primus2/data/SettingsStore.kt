package com.company.primus2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    // --- 設定項目のキーを定義 ---
    private val isFirstLaunchKey = booleanPreferencesKey("is_first_launch")
    private val voiceIdKey = intPreferencesKey("voice_id")
    private val isSpeechEnabledKey = booleanPreferencesKey("is_speech_enabled")
    private val isTextVisibleKey = booleanPreferencesKey("is_text_visible")
    private val isLearningEnabledKey = booleanPreferencesKey("is_learning_enabled")

    private val languageTagKey = stringPreferencesKey("language_tag") // ★追加

    // --- 初回起動に関するFlowと関数 ---
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[isFirstLaunchKey] ?: true // デフォルトはtrue
        }

    suspend fun completeFirstLaunch() {
        context.dataStore.edit { settings ->
            settings[isFirstLaunchKey] = false
        }
    }

    // --- 各設定値を監視するためのFlow ---
    val voiceId: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[voiceIdKey] ?: 1 // デフォルトは話者ID:1
    }

    val isSpeechEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isSpeechEnabledKey] ?: true // デフォルトはON
    }

    val isTextVisible: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isTextVisibleKey] ?: true // デフォルトはON
    }

    // 学習の有効/無効（デフォルトはOFF）
    val isLearningEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isLearningEnabledKey] ?: false
    }

    // 表示言語（"ja", "en" など）。デフォルトは日本語。
    val languageTag: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[languageTagKey] ?: "ja"
    }

    // --- 各設定値を更新するための関数 ---
    suspend fun setVoiceId(id: Int) {
        context.dataStore.edit { settings ->
            settings[voiceIdKey] = id
        }
    }

    suspend fun setSpeechEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[isSpeechEnabledKey] = isEnabled
        }
    }

    suspend fun setTextVisible(isVisible: Boolean) {
        context.dataStore.edit { settings ->
            settings[isTextVisibleKey] = isVisible
        }
    }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { settings ->
            settings[languageTagKey] = tag
        }
    }

    suspend fun setLearningEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[isLearningEnabledKey] = isEnabled
        }
    }
}
