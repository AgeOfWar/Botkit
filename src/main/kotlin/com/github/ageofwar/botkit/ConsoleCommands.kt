package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.PluginLoadException
import com.github.ageofwar.botkit.files.findPluginsNames
import com.github.ageofwar.botkit.files.loadPlugin
import com.github.ageofwar.ktelegram.TelegramApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

typealias ConsoleCommands = MutableMap<String, ConsoleCommand>

interface ConsoleCommand {
    suspend fun handle(name: String, args: String)
}

suspend fun listenCommands(logger: Loggers, commands: ConsoleCommands, unknownCommand: ConsoleCommand = UnknownCommand(logger)) {
    while (true) {
        val input = withContext(Dispatchers.IO) {
            readLine()
        }?.trim() ?: break
        val parts = input.split(Regex("\\s+"), limit = 2)
        logger.log(CommandReceived(input))
        val name = parts[0]
        val args = if (parts.size > 1) parts[1] else ""
        val command = commands[name] ?: unknownCommand
        try {
            command.handle(name, args)
        } catch (_: StopRequest) {
            break
        } catch (e: Throwable) {
            logger.log(CommandError(input, e))
        }
    }
}

object StopRequest : Throwable()

fun defaultCommands(
    logger: Loggers,
    pluginDirectory: File,
    plugins: Plugins,
    scope: CoroutineScope,
    api: TelegramApi
) = mutableMapOf(
    "stop" to StopCommand(logger),
    "enable" to EnablePluginCommand(logger, pluginDirectory, plugins, scope, api),
    "disable" to DisablePluginCommand(logger, plugins, scope, api),
    "plugins" to PluginsCommand(logger, pluginDirectory, plugins),
    "reload" to ReloadPluginsCommand(logger, plugins, scope, api)
)

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

class EnablePluginCommand(
    private val logger: Loggers,
    private val directory: File,
    private val plugins: Plugins,
    private val scope: CoroutineScope,
    private val api: TelegramApi
) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            logger.log(Usage(name))
            return
        }
        val pluginName = args.removeSuffix(".jar")
        val plugin = try {
            plugins.loadPlugin(directory.resolve("$pluginName.jar"), logger, api, scope)
        } catch (e: PluginLoadException) {
            logger.log(PluginLoadError(pluginName, e.message))
            return
        }
        if (plugin.name in plugins) {
            logger.log(AlreadyEnabled(args))
            return
        }
        if (plugin.init(logger)) {
            plugins[pluginName] = plugin
            logger.log(Enabled(pluginName))
            scope.launch { api.updateMyCommands(plugins) }
        }
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class AlreadyEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.alreadyEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class Enabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.enable.enabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class DisablePluginCommand(
    private val logger: Loggers,
    private val plugins: Plugins,
    private val scope: CoroutineScope,
    private val api: TelegramApi
) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            logger.log(Usage(name))
            return
        }
        val plugin = plugins[args]
        if (plugin == null) {
            logger.log(NotEnabled(args))
            return
        }
        plugins.remove(args)
        plugin.close(logger)
        logger.log(Disabled(args))
        scope.launch { api.updateMyCommands(plugins) }
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class NotEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.notEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class Disabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.disable.disabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class PluginsCommand(
    private val logger: Loggers,
    private val directory: File,
    private val plugins: com.github.ageofwar.botkit.Plugins
) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        logger.log(Plugins(plugins.keys, findPluginsNames(directory).toList()))
    }
    
    data class Plugins(val enabled: Iterable<String>, val available: Iterable<String>) : LoggerEvent {
        override fun message(format: Strings) = format.commands.plugins.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class ReloadPluginsCommand(
    private val logger: Loggers,
    private val plugins: Plugins,
    private val scope: CoroutineScope,
    private val api: TelegramApi
) : ConsoleCommand {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty() || args == "*") {
            handle(name)
        } else {
            val plugin = plugins[args] ?: return logger.log(NotEnabled(args))
            plugins -= args
            plugin.close(logger)
            val reloadedPlugin = try {
                plugins.loadPlugin(plugin.file, logger, api, scope)
            } catch (e: PluginLoadException) {
                return logger.log(PluginLoadError(plugin.file.nameWithoutExtension, e.message))
            }
            if (reloadedPlugin.init(logger)) {
                plugins[args] = reloadedPlugin
                logger.log(PluginReloaded(args))
            }
        }
        scope.launch { api.updateMyCommands(plugins) }
    }
    
    private suspend fun handle(name: String) {
        val enabledPlugins = plugins.mapValues { it.value.file }
        enabledPlugins.forEach { (name, _) ->
            val plugin = plugins[name]
            if (plugin != null) {
                plugins -= name
                plugin.close(logger)
            }
        }
        val reloadedPlugins = enabledPlugins.filter { (name, file) ->
            val plugin = try {
                plugins.loadPlugin(file, logger, api, scope)
            } catch (e: PluginLoadException) {
                logger.log(PluginLoadError(file.nameWithoutExtension, e.message))
                return@filter false
            }
            if (plugin.init(logger)) {
                plugins[name] = plugin
                true
            } else {
                false
            }
        }
        logger.log(PluginsReloaded(reloadedPlugins.keys))
    }
    
    data class NotEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: Strings) = format.commands.reload.notEnabled
        override val category = "Botkit"
        override val level = "INFO"
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
