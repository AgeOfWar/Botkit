package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.findPluginsNames
import com.github.ageofwar.botkit.files.loadPlugin
import com.github.ageofwar.ktelegram.TelegramApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias Commands = MutableMap<String, Command>

interface Command {
    suspend fun handle(name: String, args: String)
}

suspend fun listenCommands(logger: Loggers, commands: Commands, unknownCommand: Command = UnknownCommand(logger)) {
    while (true) {
        val input = withContext(Dispatchers.IO) {
            readLine()
        } ?: break
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
    pluginDirectory: String,
    plugins: Plugins,
    scope: CoroutineScope,
    api: TelegramApi
) = mutableMapOf(
    "stop" to StopCommand(logger),
    "enable" to EnablePluginCommand(logger, pluginDirectory, plugins, scope, api),
    "disable" to DisablePluginCommand(logger, plugins, scope, api),
    "plugins" to PluginsCommand(logger, pluginDirectory, plugins),
    "reload" to ReloadPluginsCommand(logger, pluginDirectory, plugins, scope, api)
)

class UnknownCommand(private val logger: Loggers) : Command {
    override suspend fun handle(name: String, args: String) {
        logger.log(UnknownCommand(name, args))
    }
    
    data class UnknownCommand(val name: String, val args: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.unknown.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class StopCommand(private val logger: Loggers) : Command {
    override suspend fun handle(name: String, args: String) {
        logger.log(StopCommand)
        throw StopRequest
    }
    
    object StopCommand : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.stop.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class EnablePluginCommand(
    private val logger: Loggers,
    private val directory: String,
    private val plugins: Plugins,
    private val scope: CoroutineScope,
    private val api: TelegramApi
) : Command {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            logger.log(Usage(name))
            return
        }
        val pluginName = args.removeSuffix(".jar")
        if (pluginName in plugins) {
            logger.log(AlreadyEnabled(args))
            return
        }
        val plugin = loadPlugin("$directory/${args.removeSuffix(".jar")}.jar")
        if (plugin == null) {
            logger.log(CannotLoad(pluginName))
            return
        }
        if ((pluginName to plugin).init(scope, api, logger)) {
            plugins[pluginName] = plugin
            logger.log(Enabled(pluginName))
        } else {
            logger.log(CannotEnable(pluginName))
        }
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.enable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class AlreadyEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.enable.alreadyEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class CannotLoad(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.enable.cannotLoad
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class CannotEnable(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.enable.cannotEnable
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class Enabled(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.enable.enabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class DisablePluginCommand(
    private val logger: Loggers,
    private val plugins: Plugins,
    private val scope: CoroutineScope,
    private val api: TelegramApi
) : Command {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            logger.log(Usage(name))
            return
        }
        val pluginName = args.removeSuffix(".jar")
        val plugin = plugins[pluginName]
        if (plugin == null) {
            logger.log(NotEnabled(args))
            return
        }
        plugins.remove(pluginName)
        if ((pluginName to plugin).close(scope, api, logger)) {
            logger.log(Disabled(pluginName))
        } else {
            logger.log(CannotDisable(pluginName))
        }
    }
    
    data class Usage(val name: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.disable.usage
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class NotEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.disable.notEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class CannotDisable(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.disable.cannotDisable
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class Disabled(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.disable.disabled
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class PluginsCommand(
    private val logger: Loggers,
    private val directory: String,
    private val plugins: com.github.ageofwar.botkit.Plugins
) : Command {
    override suspend fun handle(name: String, args: String) {
        val (enabled, disabled) = findPluginsNames(directory).partition {
            it in plugins
        }
        logger.log(Plugins(enabled, disabled))
    }
    
    data class Plugins(val enabled: List<String>, val disabled: List<String>) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.plugins.message
        override val category = "Botkit"
        override val level = "INFO"
    }
}

class ReloadPluginsCommand(
    private val logger: Loggers,
    private val directory: String,
    private val plugins: Plugins,
    private val scope: CoroutineScope,
    private val api: TelegramApi
) : Command {
    override suspend fun handle(name: String, args: String) {
        if (args.isEmpty()) {
            handle(name)
        } else {
            val pluginName = args.removeSuffix(".jar")
            val plugin = plugins[pluginName]
            if (plugin == null) {
                return logger.log(NotEnabled(pluginName))
            }
            plugins -= pluginName
            if (!(pluginName to plugin).close(scope, api, logger)) {
                logger.log(CannotDisable(pluginName))
            }
            val reloadedPlugin = loadPlugin("$directory/$pluginName.jar")
            if (reloadedPlugin == null) {
                return logger.log(CannotLoad(pluginName))
            }
            if ((pluginName to reloadedPlugin).init(scope, api, logger)) {
                plugins[pluginName] = reloadedPlugin
                logger.log(PluginReloaded(pluginName))
            } else {
                logger.log(CannotEnable(pluginName))
            }
        }
    }
    
    private suspend fun handle(name: String) {
        val enabledPlugins = HashSet(plugins.keys)
        enabledPlugins.forEach {
            val plugin = plugins[it]
            if (plugin != null) {
                plugins -= it
                if (!(it to plugin).close(scope, api, logger)) {
                    logger.log(CannotDisable(it))
                }
            }
        }
        val reloadedPlugins = enabledPlugins.filter {
            val plugin = loadPlugin("$directory/$it.jar")
            if (plugin == null) {
                logger.log(CannotLoad(it))
                return@filter false
            }
            if ((it to plugin).init(scope, api, logger)) {
                plugins[it] = plugin
                true
            } else {
                logger.log(CannotEnable(it))
                false
            }
        }
        logger.log(PluginsReloaded(reloadedPlugins))
    }
    
    data class NotEnabled(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.reload.notEnabled
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class CannotLoad(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.reload.cannotLoad
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class CannotEnable(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.reload.cannotEnable
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class CannotDisable(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.reload.cannotDisable
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginReloaded(val plugin: String) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.reload.pluginReloaded
        override val category = "Botkit"
        override val level = "INFO"
    }
    
    data class PluginsReloaded(val plugins: List<String>) : LoggerEvent {
        override fun message(format: LoggerFormat) = format.commands.reload.pluginsReloaded
        override val category = "Botkit"
        override val level = "INFO"
    }
}
