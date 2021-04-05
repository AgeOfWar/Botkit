package com.github.ageofwar.botkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BotkitConfig

@Serializable
@SerialName("long_polling")
data class LongPollingConfig(
    @SerialName("back_off")
    val backOff: Long = 50
) : BotkitConfig()
