package com.company.primus2.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendConfig(
    val features: Features,
    val limits: Limits,
    val version: String
) {
    @Serializable
    data class Features(val autonomy: Boolean = false, val tts: Boolean = false)
    @Serializable
    data class Limits(
        @SerialName("daily_suggestions") val dailySuggestions: Int = 0,
        @SerialName("cooldown_sec") val cooldownSec: Int = 0
    )
}
