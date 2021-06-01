package com.github.ageofwar.botkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Strings(
    @SerialName("bot_start") val botStart: String?,
    @SerialName("bot_stop") val botStop: String?,
    @SerialName("long_polling_error") val longPollingError: String?,
    @SerialName("old_update") val oldUpdate: String?,
    @SerialName("new_update") val newUpdate: String?,
    @SerialName("plugin_load_error") val pluginLoadError: String?,
    @SerialName("plugin_init_error") val pluginInitError: String?,
    @SerialName("plugin_update_error") val pluginUpdateError: String?,
    @SerialName("plugin_close_error") val pluginCloseError: String?,
    @SerialName("plugin_already_enabled") val pluginAlreadyEnabled: String?,
    @SerialName("plugin_not_enabled") val pluginNotEnabled: String?,
    @SerialName("plugin_not_available") val pluginNotAvailable: String?,
    @SerialName("command_received") val commandReceived: String?,
    @SerialName("command_error") val commandError: String?,
    @SerialName("commands_reload_error") val commandsReloadError: String?,
    @SerialName("plugin_enabled") val pluginEnabled: String?,
    @SerialName("plugins_enabled") val pluginsEnabled: String?,
    @SerialName("plugin_disabled") val pluginDisabled: String?,
    @SerialName("plugins_disabled") val pluginsDisabled: String?,
    @SerialName("plugin_reloaded") val pluginReloaded: String?,
    @SerialName("plugins_reloaded") val pluginsReloaded: String?,
    @SerialName("unknown_command") val unknownCommand: String?,
    @SerialName("show_plugins") val showPlugins: String?,
    @SerialName("command_conflict") val commandConflict: String?,
    @SerialName("show_command") val showCommand: String?,
    @SerialName("show_commands") val showCommands: String?,
    @SerialName("show_usage") val showUsage: String?,
    @SerialName("debug_mode") val debugMode: String?,
    val commands: Map<String, Command>
) {
    @Serializable
    data class Command(
        val usage: String? = null,
        val description: String? = null
    )
}