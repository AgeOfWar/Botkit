package com.github.ageofwar.botkit

import com.github.ageofwar.ktelegram.DetailedBot
import com.github.ageofwar.ktelegram.Update

interface LoggerEvent {
    fun message(format: Strings): String?
    val category: String
    val level: String
    val throwable: Throwable? get() = null
}

data class BotStart(val bot: DetailedBot) : LoggerEvent {
    override fun message(format: Strings) = format.botStart
    override val category = "Botkit"
    override val level = "INFO"
}

data class BotStop(val bot: DetailedBot) : LoggerEvent {
    override fun message(format: Strings) = format.botStop
    override val category = "Botkit"
    override val level = "INFO"
}

data class OldUpdate(val update: Update) : LoggerEvent {
    override fun message(format: Strings) = format.oldUpdate
    override val category = "Botkit"
    override val level = "INFO"
}

data class NewUpdate(val update: Update) : LoggerEvent {
    override fun message(format: Strings) = format.newUpdate
    override val category = "Botkit"
    override val level = "INFO"
}

data class PluginLoadError(val plugin: String, val message: String) : LoggerEvent {
    override fun message(format: Strings) = format.pluginLoadError
    override val category = plugin
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
    override val level = "INFO"
}

data class CommandError(val input: String, override val throwable: Throwable) : LoggerEvent {
    override fun message(format: Strings) = format.commandError
    override val category = "Botkit"
    override val level = "ERROR"
}