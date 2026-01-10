package com.company.primus2.firebase

/**
 * Firebase 設定の必須化コントロール。
 *
 * 今は Firebase 認証まわりを「無効でもアプリが動く」前提にする。
 * 将来、ユーザー単位のクラウド同期や課金連携を入れるときに true に戻す。
 */
object FirebaseConfig {

    /**
     * google-services.json が無い／壊れている場合に
     * アプリ起動をブロックするかどうか。
     *
     * 現フェーズでは Firebase 未設定端末でも Primus を動かしたいので false。
     */
    const val ENFORCE: Boolean = false

    /**
     * ネットワークリクエストで Firebase ID トークンを必須にするか。
     *
     * true にすると、IDトークンが取れない環境では全APIが NetworkAuthException で落ちる。
     * 現状そこまでの認証要件はないので false。
     */
    const val REQUIRE_ID_TOKEN_FOR_REQUESTS: Boolean = false
}
