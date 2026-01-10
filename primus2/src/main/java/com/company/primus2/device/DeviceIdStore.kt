package com.company.primus2.device

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 安定 device_id を DataStore に保存・提供するユーティリティ。
 * 優先: ANDROID_ID → 取得不可なら UUID 生成（初回のみ）。
 */
private val Context.deviceDataStore by preferencesDataStore(name = "device_store")

object DeviceIdStore {
    private val KEY = stringPreferencesKey("device_id")

    /** 監視用（UI等で表示したい場合） */
    fun flow(context: Context): Flow<String?> =
        context.deviceDataStore.data.map { it[KEY] }

    /** 取得または生成して保存して返す（アプリ内で呼べばOK） */
    suspend fun getOrCreate(context: Context): String {
        // 既存を読む（Flow の firstOrNull を正式APIで使用）
        val current = flow(context).firstOrNull()
        if (!current.isNullOrBlank()) return current

        // 候補1: ANDROID_ID（端末初期化で変化する可能性あり）
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Throwable) { null }

        val newId = (androidId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())

        // 保存
        context.deviceDataStore.edit { it[KEY] = newId }
        return newId
    }
}
