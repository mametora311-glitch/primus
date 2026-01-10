package com.company.primus2.data

import android.annotation.SuppressLint
import android.content.Context
import com.company.primus2.core_ai.model.EmotionState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EmotionStorage {
    private const val PREF_NAME = "primus_pref"
    private const val KEY_EMOTION_STATE = "emotion_state"

    @SuppressLint("UseKtx")
    fun saveEmotionState(context: Context, state: EmotionState) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // オブジェクトをJSON文字列に一行で変換
        val jsonString = Json.encodeToString(state)
        prefs.edit().putString(KEY_EMOTION_STATE, jsonString).apply()
    }

    fun loadEmotionState(context: Context): EmotionState {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_EMOTION_STATE, null)
        return try {
            // JSON文字列を一行でオブジェクトに復元
            jsonString?.let { Json.decodeFromString<EmotionState>(it) } ?: EmotionState()
        } catch (e: Exception) {
            EmotionState() // パース失敗時はデフォルト値を返す
        }
    }
}