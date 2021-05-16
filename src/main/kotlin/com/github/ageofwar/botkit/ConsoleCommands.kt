package com.github.ageofwar.botkit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.URL

typealias ConsoleCommands = MutableMap<String, ConsoleCommand>

interface ConsoleCommand {
    suspend fun handle(name: String, args: String)
    
    suspend fun Context.logUsage(name: String) = log(Usage(logger.strings.commands[name]?.usage))
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
}

class StopCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        context.consoleCommandChannel.close()
    }
}

class EnablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) = with(context) {
        if (args.isEmpty()) return logUsage(name)
        if (args == "*") return handle(name)
        val pluginFile = searchAvailablePlugin(args)
        val plugin = if (pluginFile != null) {
            enablePlugin(pluginFile)
        } else {
            try {
                val url = URL(args)
                enablePlugin(url)
            } catch (e: MalformedURLException) {
                return log(PluginNotAvailable(args))
            }
        }
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
}

class DisablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) = with(context) {
        if (args.isEmpty()) return logUsage(name)
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
}

class PluginsCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) = with(context) {
        log(ShowPlugins(plugins.keys, getAvailablePluginsNames()))
    }
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
                1 -> {
                    val plugin = plugins[0]
                    with(plugin.consoleCommands[name]!!) { plugin.handle(name, args) }
                }
                else -> log(Conflict(name, plugins.map { it.name }))
            }
        } else {
            val (pluginName, commandName) = nameParts
            if (pluginName == "Botkit") return commands[commandName]?.handle(name, args) ?: unknownCommand.handle(name, args)
            val plugin = plugins[pluginName] ?: return log(PluginNotEnabled(pluginName))
            val command = plugin.consoleCommands[commandName] ?: return unknownCommand.handle(name, args)
            with(command) { plugin.handle(name, args) }
        }
    }
}

class HelpCommand(
    private val context: Context
) : ConsoleCommand {
    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun handle(name: String, args: String) = with(context) {
        if (args.isEmpty()) {
            val commands = buildMap<String, Map<String, Strings.Command>> {
                put("Botkit", commands.mapValues { (name, _) -> logger.strings.commands[name] ?: Strings.Command() })
                plugins.forEach { (name, plugin) ->
                    put(name, plugin.consoleCommands.mapValues { (_, command) -> Strings.Command(command.usage, command.description) })
                }
            }
            return log(ShowCommands(commands))
        }
        val commands = buildMap<String, Strings.Command> {
            val botkitCommand = commands[args]
            if (botkitCommand != null) put("Botkit", logger.strings.commands[args] ?: Strings.Command())
            plugins.forEach { (name, plugin) ->
                val pluginCommand = plugin.consoleCommands[args]
                if (pluginCommand != null) put(name, Strings.Command(pluginCommand.usage, pluginCommand.description))
            }
        }
        log(ShowCommand(args, commands))
    }
}
