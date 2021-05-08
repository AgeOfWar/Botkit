package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.template
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

typealias ConsoleCommands = MutableMap<String, ConsoleCommand>

interface ConsoleCommand {
    suspend fun handle(name: String, args: String)
    fun usage(format: Strings): String?
}

suspend fun Context.listenCommands(unknownCommand: ConsoleCommand = OtherPluginsCommand(this)) {
    for (input in consoleCommandChannel) {
        try {
            handleCommand(input, unknownCommand)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log(CommandError(input, e))
        }
    }
}

suspend fun Context.sendCommands() {
    while (!consoleCommandChannel.isClosedForSend) {
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
            consoleCommandChannel.send(input)
        } catch (e: ClosedSendChannelException) {
            break
        }
    }
}

private suspend fun Context.handleCommand(input: String, unknownCommand: ConsoleCommand) {
    val parts = input.trim().split(Regex("\\s+"), limit = 2)
    if (parts[0].isEmpty()) return
    log(CommandReceived(input))
    val name = parts[0]
    val args = if (parts.size > 1) parts[1] else ""
    val command = commands[name] ?: unknownCommand
    try {
        command.handle(name, args)
    } catch (e: Throwable) {
        log(CommandError(input, e))
    }
}

class UnknownCommand(private val logger: Loggers) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        logger.log(UnknownCommandName(name, args))
    }
    
    override fun usage(format: Strings): Nothing? = null
}

class StopCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        context.consoleCommandChannel.close()
    }
    
    override fun usage(format: Strings) = format.commands.stop
}

class EnablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) = with(context) {
        if (args.isEmpty()) return log(Usage(usage(logger.strings)))
        if (args == "*") return handle(name)
        val pluginFile = searchAvailablePlugin(args) ?: return log(PluginNotAvailable(args))
        val plugin = enablePlugin(pluginFile)
        if (plugin != null) {
            log(PluginEnabled(plugin.name))
            reloadCommands()
        }
    }
    
    private suspend fun handle(name: String): Unit = with(context) {
        val enabled = enablePlugins(getAvailablePlugins())
        log(PluginsEnabled(enabled.map { it.name }))
        reloadCommands()
    }
    
    override fun usage(format: Strings) = format.commands.enable
}

class DisablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) = with(context) {
        if (args.isEmpty()) return log(Usage(usage(logger.strings)))
        if (args == "*") return handle(name)
        val pluginName = searchPluginName(args) ?: return log(PluginNotEnabled(args))
        val plugin = disablePlugin(pluginName)
        if (plugin != null) {
            log(PluginDisabled(plugin.name))
            reloadCommands()
        }
    }
    
    private suspend fun handle(name: String): Unit = with(context) {
        val disabled = context.disablePlugins(plugins.keys)
        log(PluginsDisabled(disabled.map { it.name }))
        reloadCommands()
    }
    
    override fun usage(format: Strings) = format.commands.disable
}

class PluginsCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) = with(context) {
        log(ShowPlugins(plugins.keys, getAvailablePluginsNames()))
    }
    
    override fun usage(format: Strings) = format.commands.plugins
}

class ReloadPluginsCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String): Unit = with(context) {
        if (args.isEmpty() || args == "*") {
            handle(name)
        } else {
            val pluginName = searchPluginName(args) ?: return log(PluginNotEnabled(args))
            val plugin = reloadPlugin(pluginName)
            if (plugin != null) {
                log(PluginReloaded(plugin.name))
            }
        }
        reloadCommands()
    }
    
    private suspend fun handle(name: String): Unit = with(context) {
        val reloadedPlugins = reloadPlugins(plugins.keys)
        log(PluginsReloaded(reloadedPlugins.map { it.name }))
    }
    
    override fun usage(format: Strings) = format.commands.reload
}

class OtherPluginsCommand(
    private val context: Context,
    private val unknownCommand: ConsoleCommand = UnknownCommand(context.logger)
) : ConsoleCommand {
    override suspend fun handle(name: String, args: String): Unit = with(context) {
        val nameParts = name.split('/', limit = 2)
        if (nameParts.size == 1) {
            val plugins = plugins.values.filter { it.consoleCommands.containsKey(name) }
            when (plugins.size) {
                0 -> unknownCommand.handle(name, args)
                1 -> plugins[0].consoleCommands[name]?.handle(name, args)
                else -> log(Conflict(name, plugins.map { it.name }))
            }
        } else {
            val (pluginName, commandName) = nameParts
            if (pluginName == "Botkit") return commands[commandName]?.handle(name, args) ?: unknownCommand.handle(name, args)
            val plugin = plugins[pluginName] ?: return log(PluginNotEnabled(pluginName))
            plugin.consoleCommands[commandName]?.handle(name, args) ?: unknownCommand.handle(name, args)
        }
    }
    
    override fun usage(format: Strings): Nothing? = null
}

class HelpCommand(
    private val context: Context
) : ConsoleCommand {
    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun handle(name: String, args: String) = with(context) {
        if (args.isEmpty()) {
            val commands = buildMap<String, Map<String, String?>> {
                put("Botkit", commands.mapValues { (name, command) -> command.usage(logger.strings)?.template("name" to name) })
                plugins.forEach { (name, plugin) ->
                    put(name, plugin.consoleCommands.mapValues { (name, command) -> command.usage?.template("name" to name) })
                }
            }
            return log(ShowCommands(commands))
        }
        val commands = buildMap<String, String?> {
            val botkitCommand = commands[args]
            if (botkitCommand != null) put("Botkit", botkitCommand.usage(logger.strings)?.template("name" to args))
            plugins.forEach { (name, plugin) ->
                val pluginCommand = plugin.consoleCommands[args]
                if (pluginCommand != null) put(name, pluginCommand.usage?.template("name" to args))
            }
        }
        log(ShowCommand(args, commands))
    }
    
    override fun usage(format: Strings) = format.commands.help
}
