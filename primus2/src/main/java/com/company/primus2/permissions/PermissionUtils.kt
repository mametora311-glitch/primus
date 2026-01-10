package com.company.primus2.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 現在未許可のパーミッション一覧を返す。
 */
fun findMissingPermissions(context: Context, permissions: List<String>): List<String> {
    return permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 「今後表示しない」等により恒久拒否か判定。
 * - 未許可 かつ shouldShowRequestPermissionRationale == false → 恒久拒否
 */
fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
    val granted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    return !granted && !shouldShow
}

/**
 * アプリの詳細設定を開く（権限をユーザーに手動で許可してもらう）。
 */
fun openAppDetailsSettings(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * Context から Activity を取り出す拡張。
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 録音・通知に必要な権限リストをOSバージョンに応じて返す。
 */
fun requiredVoicePermissions(): List<String> {
    val list = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list += android.Manifest.permission.POST_NOTIFICATIONS
    }
    return list
}

/**
 * Demo/検証用の小さなヘルパー：安全なログタグ。
 */
const val PERM_LOG = "Primus/Perms"
