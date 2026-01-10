package com.company.primus2.net

/**
 * プロキシ接続状態
 */
enum class ProxyStatus {
    UNKNOWN,   // まだ未確認
    CHECKING,  // 接続チェック中
    OK,        // 接続OK（auth/config取得OK）
    ERROR      // 接続NG
}
