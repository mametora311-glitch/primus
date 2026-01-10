package com.company.primus2.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 理由説明（Rationale）ダイアログ。
 */
@Composable
fun RationaleDialog(
    onDismiss: () -> Unit,
    onRequestAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("権限が必要です") },
        text = {
            Text("音声入出力や通知のために権限が必要です。次のダイアログで『許可』を選んでください。")
        },
        confirmButton = {
            TextButton(onClick = onRequestAgain) { Text("続ける") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

/**
 * 恒久拒否（Permanently Denied）ダイアログ。
 */
@Composable
fun PermanentlyDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定から権限を許可してください") },
        text = {
            Text("『今後表示しない』等により権限ダイアログが出せません。アプリの設定画面を開いて権限を許可してください。")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("設定を開く") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    )
}
