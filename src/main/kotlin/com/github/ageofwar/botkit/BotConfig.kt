package com.github.ageofwar.botkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val token: String? = null,
    @SerialName("api_url") val apiUrl: String = "https://api.telegram.org"
)
