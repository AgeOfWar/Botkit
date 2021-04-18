package com.github.ageofwar.botkit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

typealias ConsoleCommands = MutableMap<String, ConsoleCommand>

interface ConsoleCommand {
    suspend fun handle(name: String, args: String)
}

suspend fun listenCommands(context: Context, unknownCommand: ConsoleCommand = UnknownCommand(context.logger)) {
    for (input in context.consoleCommandChannel) {
        try {
            handleCommand(input, context, unknownCommand)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            context.log(CommandError(input, e))
        }
    }
}

suspend fun sendCommands(context: Context) {
    while (!context.consoleCommandChannel.isClosedForSend) {
        val input = withContext(Dispatchers.IO) {
            if (System.`in`.available() != 0) {
                readLine()
            } else {
                null
            }
        }
        if (input == null) {
            delay(100)
            continue
        }
        try {
            context.consoleCommandChannel.send(input)
        } catch (e: ClosedSendChannelException) {
            break
        }
    }
}

private suspend fun handleCommand(input: String, context: Context, unknownCommand: ConsoleCommand = UnknownCommand(context.logger)) {
    val parts = input.trim().split(Regex("\\s+"), limit = 2)
    if (parts[0].isEmpty()) return
    context.log(CommandReceived(input))
    val name = parts[0]
    val args = if (parts.size > 1) parts[1] else ""
    val command = context.commands[name] ?: unknownCommand
    try {
        command.handle(name, args)
    } catch (e: Throwable) {
        context.log(CommandError(input, e))
    }
}

class UnknownCommand(private val logger: Loggers) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        logger.log(UnknownCommand(name, args))
    }
    
    data class UnknownCommand(val name: String, val args: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.unknown.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class StopCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        context.consoleCommandChannel.close()
        context.log(StopCommand)
    }
    
    object StopCommand : LoggerEvent {
        override fun message(format: Strings) = format.commands.stop.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class EnablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            return context.log(Usage(name))
        }
        if (args == "*") {
            return handle(name)
        }
        val pluginName = args.removeSuffix(".jar")
        val plugin = context.enablePlugin(pluginName)
        if (plugin != null) {
            context.log(PluginEnabled(plugin.name))
            context.reloadCommands()
        }
    }
    
    private suspend fun handle(name: String) {
        val enabled = context.enablePlugins(*context.getAvailablePlugins().toList().toTypedArray())
        context.log(PluginsEnabled(enabled.map { it.name }))
        context.reloadCommands()
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.pluginEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginsEnabled(val plugins: Iterable<String>) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.pluginsEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class DisablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            return context.log(Usage(name))
        }
        if (args == "*") {
            return handle(name)
        }
        val plugin = context.disablePlugin(args)
        if (plugin != null) {
            context.log(PluginDisabled(plugin.name))
            context.reloadCommands()
        }
    }
    
    private suspend fun handle(name: String) {
        val disabled = context.disablePlugins(*context.plugins.keys.toTypedArray())
        context.log(PluginsDisabled(disabled.map { it.name }))
        context.reloadCommands()
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginDisabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.pluginDisabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginsDisabled(val plugins: Iterable<String>) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.pluginsDisabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class PluginsCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        context.log(Plugins(context.plugins.keys, context.getAvailablePlugins().toList()))
    }
    
    data class Plugins(val enabled: Iterable<String>, val available: Iterable<String>) : LoggerEvent {
        override fun message(format: Strings) = format.commands.plugins.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class ReloadPluginsCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty() || args == "*") {
            handle(name)
        } else {
            val plugin = context.reloadPlugin(args)
            if (plugin != null) {
                context.log(PluginReloaded(plugin.name))
            }
        }
        context.reloadCommands()
    }
    
    private suspend fun handle(name: String) {
        val reloadedPlugins = context.reloadPlugins(*context.plugins.keys.toTypedArray())
        context.log(PluginsReloaded(reloadedPlugins.map { it.name }))
    }
    
    data class PluginReloaded(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.reload.pluginReloaded
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginsReloaded(val plugins: Iterable<String>) : LoggerEvent {
        override fun message(format: Strings) = format.commands.reload.pluginsReloaded
        override val category = "Botkit"
        override val level = "INFO"
    }
}
