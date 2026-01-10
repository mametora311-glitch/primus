package com.company.primus2.env

import com.company.primus2.BuildConfig

object ProxyEnv {
    val BASE_URL: String get() = BuildConfig.PROXY_BASE_URL
    val SERVICE_TOKEN: String get() = BuildConfig.SERVICE_TOKEN
}
