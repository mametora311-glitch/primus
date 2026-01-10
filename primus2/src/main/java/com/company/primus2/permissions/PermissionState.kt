package com.company.primus2.permissions

/**
 * 権限UI状態の定義。
 * - AllGranted: すべて許可済み（本来の画面を表示）
 * - Requesting: システムダイアログを要求中
 * - NeedsRationale: 「なぜ必要か」の説明ダイアログを表示
 * - PermanentlyDenied: 「今後表示しない」等で恒久拒否 → 設定アプリへ誘導
 */
sealed interface PermissionUiState {
    data object AllGranted : PermissionUiState
    data object Requesting : PermissionUiState
    data class NeedsRationale(
        val missing: List<String>
    ) : PermissionUiState
    data class PermanentlyDenied(
        val missing: List<String>
    ) : PermissionUiState
}
