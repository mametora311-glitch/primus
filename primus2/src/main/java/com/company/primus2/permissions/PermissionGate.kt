package com.company.primus2.permissions

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 指定したパーミッション群を満たすまでガードし、UIを自動遷移させるコンポーザ。
 *
 * 動作:
 * 1) 初回コンポーズ時に不足権限を検出
 * 2) 理由説明が必要なら RationaleDialog を表示 → 再リクエスト
 * 3) 「今後表示しない」等で恒久拒否なら PermanentlyDeniedDialog を表示 → 設定へ
 * 4) 全許可になったら [content] を表示
 */
@Composable
fun PermissionGate(
    permissions: List<String>,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var uiState by remember { mutableStateOf<PermissionUiState>(PermissionUiState.Requesting) }
    var lastRequested by remember { mutableStateOf<List<String>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val missing = result.filterValues { granted -> !granted }.keys.toList()
        if (missing.isEmpty()) {
            uiState = PermissionUiState.AllGranted
            Log.i(PERM_LOG, "All granted")
        } else {
            // 恒久拒否判定
            val permanently = if (activity != null) {
                missing.filter { isPermanentlyDenied(activity, it) }
            } else emptyList()

            uiState = if (permanently.isNotEmpty()) {
                PermissionUiState.PermanentlyDenied(missing = missing)
            } else {
                PermissionUiState.NeedsRationale(missing = missing)
            }
            Log.w(PERM_LOG, "Denied: $missing, permanently=${permanently.isNotEmpty()}")
        }
    }

    // 初回：不足分を検出して要求
    LaunchedEffect(Unit) {
        val missing = findMissingPermissions(context, permissions)
        if (missing.isEmpty()) {
            uiState = PermissionUiState.AllGranted
        } else {
            // いきなりリクエスト（ダイアログ前に rationale が必要な場合は次段で拾う）
            lastRequested = missing
            uiState = PermissionUiState.Requesting
            launcher.launch(missing.toTypedArray())
        }
    }

    when (val state = uiState) {
        PermissionUiState.AllGranted -> content()
        PermissionUiState.Requesting -> {
            // 何も描画しない（すぐに結果コールバックが戻る）
        }
        is PermissionUiState.NeedsRationale -> {
            RationaleDialog(
                onDismiss = { /* 何もしない＝待機 */ },
                onRequestAgain = {
                    lastRequested = state.missing
                    launcher.launch(state.missing.toTypedArray())
                }
            )
        }
        is PermissionUiState.PermanentlyDenied -> {
            PermanentlyDeniedDialog(
                onDismiss = { /* 何もしない＝待機 */ },
                onOpenSettings = {
                    openAppDetailsSettings(context)
                }
            )
        }
    }
}
