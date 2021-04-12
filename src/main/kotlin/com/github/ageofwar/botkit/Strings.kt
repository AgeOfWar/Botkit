package com.github.ageofwar.botkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Strings(
    @SerialName("bot_start") val botStart: String?,
    @SerialName("bot_stop") val botStop: String?,
    @SerialName("old_update") val oldUpdate: String?,
    @SerialName("new_update") val newUpdate: String?,
    @SerialName("plugin_load_error") val pluginLoadError: String?,
    @SerialName("plugin_init_error") val pluginInitError: String?,
    @SerialName("plugin_update_error") val pluginUpdateError: String?,
    @SerialName("plugin_close_error") val pluginCloseError: String?,
    @SerialName("command_received") val commandReceived: String?,
    @SerialName("command_error") val commandError: String?,
    val commands: Commands
) {
    @Serializable
    data class Commands(
        val unknown: Unknown,
        val stop: Stop,
        val enable: Enable,
        val disable: Disable,
        val plugins: Plugins,
        val reload: Reload
    ) {
        @Serializable
        data class Unknown(
            val message: String?
        )
        
        @Serializable
        data class Stop(
            val message: String?
        )
        
        @Serializable
        data class Enable(
            val usage: String?,
            @SerialName("already_enabled") val alreadyEnabled: String?,
            val enabled: String?
        )
        
        @Serializable
        data class Disable(
            val usage: String?,
            @SerialName("not_enabled") val notEnabled: String?,
            @SerialName("disabled") val disabled: String?
        )
        
        @Serializable
        data class Plugins(
            val message: String?
        )
        
        @Serializable
        data class Reload(
            @SerialName("not_enabled") val notEnabled: String?,
            @SerialName("plugin_reloaded") val pluginReloaded: String?,
            @SerialName("plugins_reloaded") val pluginsReloaded: String?
        )
    }
}