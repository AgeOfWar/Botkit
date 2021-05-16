package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.PluginLoadException
import com.github.ageofwar.ktelegram.DetailedBot
import com.github.ageofwar.ktelegram.Update

interface LoggerEvent {
    fun message(format: Strings): String?
    val category: String get() = "Botkit"
    val level: String get() = "INFO"
    val throwable: Throwable? get() = null
}

data class BotStart(val bot: DetailedBot) : LoggerEvent {
    override fun message(format: Strings) = format.botStart
}

data class BotStop(val bot: DetailedBot) : LoggerEvent {
    override fun message(format: Strings) = format.botStop
}

data class OldUpdate(val update: Update) : LoggerEvent {
    override fun message(format: Strings) = format.oldUpdate
}

data class NewUpdate(val update: Update) : LoggerEvent {
    override fun message(format: Strings) = format.newUpdate
}

data class PluginLoadError(val plugin: String, override val throwable: PluginLoadException) : LoggerEvent {
    override fun message(format: Strings) = format.pluginLoadError
    override val level = "ERROR"
}

data class PluginInitError(val plugin: String, override val throwable: Throwable) : LoggerEvent {
    override fun message(format: Strings) = format.pluginInitError
    override val category = plugin
    override val level = "ERROR"
}

data class PluginUpdateError(val plugin: String, override val throwable: Throwable, val update: Update) : LoggerEvent {
    override fun message(format: Strings) = format.pluginUpdateError
    override val category = plugin
    override val level = "ERROR"
}

data class PluginCloseError(val plugin: String, override val throwable: Throwable) : LoggerEvent {
    override fun message(format: Strings) = format.pluginCloseError
    override val category = plugin
    override val level = "ERROR"
}

data class CommandReceived(val input: String) : LoggerEvent {
    override fun message(format: Strings) = format.commandReceived
    override val category = "INPUT"
}

data class CommandError(val input: String, override val throwable: Throwable) : LoggerEvent {
    override fun message(format: Strings) = format.commandError
    override val level = "ERROR"
}

data class CommandReloadError(override val throwable: Throwable) : LoggerEvent {
    override fun message(format: Strings) = format.commandsReloadError
    override val level = "ERROR"
}

data class PluginAlreadyEnabled(val plugin: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginAlreadyEnabled
}

data class PluginNotEnabled(val plugin: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginNotEnabled
}

data class PluginNotAvailable(val plugin: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginNotAvailable
}

data class Usage(val usage: String?) : LoggerEvent {
    override fun message(format: Strings) = format.showUsage
}

data class UnknownCommandName(val name: String, val args: String) : LoggerEvent {
    override fun message(format: Strings) = format.unknownCommand
}

data class PluginEnabled(val plugin: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginEnabled
}

data class PluginsEnabled(val plugins: Iterable<String>) : LoggerEvent {
    override fun message(format: Strings) = format.pluginsEnabled
}

data class PluginDisabled(val plugin: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginDisabled
}

data class PluginsDisabled(val plugins: Iterable<String>) : LoggerEvent {
    override fun message(format: Strings) = format.pluginsDisabled
}

data class ShowPlugins(val enabled: Iterable<String>, val available: Iterable<String>) : LoggerEvent {
    override fun message(format: Strings) = format.showPlugins
}

data class PluginReloaded(val plugin: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginReloaded
}

data class PluginsReloaded(val plugins: Iterable<String>) : LoggerEvent {
    override fun message(format: Strings) = format.pluginsReloaded
}

data class Conflict(val name: String, val plugins: Iterable<String>) : LoggerEvent {
    override fun message(format: Strings) = format.commandConflict
}

data class ShowCommand(val name: String, val commands: Map<String, Strings.Command>) : LoggerEvent {
    override fun message(format: Strings) = format.showCommand
}

data class ShowCommands(val commands: Map<String, Map<String, Strings.Command>>) : LoggerEvent {
    override fun message(format: Strings) = format.showCommands
}