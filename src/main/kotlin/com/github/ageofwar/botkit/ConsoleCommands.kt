package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.PluginLoadException
import com.github.ageofwar.botkit.files.findPluginsNames
import com.github.ageofwar.botkit.files.loadPlugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias ConsoleCommands = MutableMap<String, ConsoleCommand>

interface ConsoleCommand {
    suspend fun handle(name: String, args: String)
}

suspend fun listenCommands(context: Context, unknownCommand: ConsoleCommand = UnknownCommand(context.logger)) {
    while (true) {
        val input = withContext(Dispatchers.IO) {
            readLine()
        }?.trim() ?: break
        try {
            handleCommand(input, context, unknownCommand)
        } catch (_: StopRequest) {
            break
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            context.log(CommandError(input, e))
        }
    }
}

suspend fun handleCommand(input: String, context: Context, unknownCommand: ConsoleCommand = UnknownCommand(context.logger)) {
    val parts = input.split(Regex("\\s+"), limit = 2)
    context.log(CommandReceived(input))
    val name = parts[0]
    val args = if (parts.size > 1) parts[1] else ""
    val command = context.commands[name] ?: unknownCommand
    try {
        command.handle(name, args)
    } catch (e: StopRequest) {
        throw e
    } catch (e: Throwable) {
        context.log(CommandError(input, e))
    }
}

object StopRequest : Throwable()

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

class StopCommand(private val logger: Loggers) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        logger.log(StopCommand)
        throw StopRequest
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
            context.log(Usage(name))
            return
        }
        val pluginName = args.removeSuffix(".jar")
        val plugin = try {
            loadPlugin(context.pluginsDirectory.resolve("$pluginName.jar"), context)
        } catch (e: PluginLoadException) {
            context.log(PluginLoadError(pluginName, e.message))
            return
        }
        if (plugin.name in context.plugins) {
            context.log(PluginAlreadyEnabled(args))
            return
        }
        if (plugin.init(context)) {
            context.plugins[pluginName] = plugin
            context.log(Enabled(pluginName))
            context.reloadCommands()
        }
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class Enabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.enabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class DisablePluginCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            context.log(Usage(name))
            return
        }
        val plugin = context.plugins[args]
        if (plugin == null) {
            context.log(PluginNotEnabled(args))
            return
        }
        context.plugins -= args
        plugin.close(context)
        context.log(Disabled(args))
        context.reloadCommands()
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class Disabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.disabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class PluginsCommand(private val context: Context) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        context.log(Plugins(context.plugins.keys, findPluginsNames(context.pluginsDirectory).toList()))
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
            val plugin = context.plugins[args] ?: return context.log(PluginNotEnabled(args))
            context.plugins -= args
            plugin.close(context)
            val reloadedPlugin = try {
                loadPlugin(plugin.file, context)
            } catch (e: PluginLoadException) {
                return context.log(PluginLoadError(plugin.file.nameWithoutExtension, e.message))
            }
            if (reloadedPlugin.init(context)) {
                context.plugins[args] = reloadedPlugin
                context.log(PluginReloaded(args))
            }
        }
        context.reloadCommands()
    }
    
    private suspend fun handle(name: String) {
        val enabledPlugins = context.plugins.mapValues { it.value.file }
        enabledPlugins.forEach { (name, _) ->
            val plugin = context.plugins[name]
            if (plugin != null) {
                context.plugins -= name
                plugin.close(context)
            }
        }
        val reloadedPlugins = enabledPlugins.filter { (name, file) ->
            val plugin = try {
                loadPlugin(file, context)
            } catch (e: PluginLoadException) {
                context.log(PluginLoadError(file.nameWithoutExtension, e.message))
                return@filter false
            }
            if (plugin.init(context)) {
                context.plugins[name] = plugin
                true
            } else {
                false
            }
        }
        context.log(PluginsReloaded(reloadedPlugins.keys))
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
