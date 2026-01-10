package com.company.primus2.net

import com.company.primus2.BuildConfig
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

/**
 * 互換レイヤ：
 *  - Debug のみログを有効化
 *  - ProxyClient からの既存呼び出し
 *      if (KtorLogging.isEnabled()) {
 *          install(Logging) { KtorLogging.enableIn(this) }
 *      }
 *    に対応する。
 */
object KtorLogging {

    /** Debug ビルドのみ有効化 */
    fun isEnabled(): Boolean = BuildConfig.DEBUG

    /** install(Logging) ブロック内で呼ばれる前提の設定 */
    fun enableIn(config: Logging.Config) {
        // BASIC 相当：URL/メソッド/ステータス等
        config.logger = Logger.DEFAULT
        config.level = LogLevel.INFO
    }
}
