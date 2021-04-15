package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.PluginLoadException
import com.github.ageofwar.botkit.files.findPluginsNames
import com.github.ageofwar.botkit.files.loadPlugin
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.BotCommand
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.setMyCommands
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

data class Context(
    val api: TelegramApi,
    val logger: Loggers,
    val scope: CoroutineScope,
    val plugins: Plugins,
    val pluginsDirectory: File,
    val commands: ConsoleCommands
)

suspend fun Context.getAvailablePlugins() = findPluginsNames(pluginsDirectory)

suspend fun Context.enablePlugin(name: String): Boolean {
    val plugin = try {
        loadPlugin(pluginsDirectory.resolve("$name.jar"), this)
    } catch (e: PluginLoadException) {
        log(PluginLoadError(name, e.message))
        return false
    }
    if (plugin.name in plugins) {
        log(PluginAlreadyEnabled(plugin.name))
        return false
    }
    if (plugin.init(this)) {
        plugins[plugin.name] = plugin
        return true
    }
    return false
}

suspend fun Context.enablePlugins(vararg names: String): List<String> {
    return names.filter { enablePlugin(it) }
}

suspend fun Context.disablePlugin(name: String): Boolean {
    val plugin = plugins[name]
    if (plugin == null) {
        log(PluginNotEnabled(name))
        return false
    }
    plugins -= name
    plugin.close(this)
    return true
}

suspend fun Context.disablePlugins(vararg names: String): List<String> {
    return names.filter { disablePlugin(it) }
}

fun Context.addDefaultConsoleCommands() = commands.putAll(arrayOf(
    "stop" to StopCommand(logger),
    "enable" to EnablePluginCommand(this),
    "disable" to DisablePluginCommand(this),
    "plugins" to PluginsCommand(this),
    "reload" to ReloadPluginsCommand(this)
))

suspend fun Context.log(event: LoggerEvent) = logger.log(event)

fun Context.reloadCommands() = scope.launch {
    suspend fun TelegramApi.updateMyCommands(plugins: Plugins) {
        val commands = plugins.flatMap { (_, plugin) ->
            plugin.commands.map { BotCommand(it.key, it.value) }
        }
        setMyCommands(commands)
    }
    
    try {
        api.updateMyCommands(plugins)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.log(CommandReloadError(e))
    }
}

fun Context.pluginLog(plugin: Plugin, message: String?) = scope.launch {
    logger.log(message, plugin.name, "INFO")
}

fun Context.pluginWarning(plugin: Plugin, message: String?) = scope.launch {
    logger.log(message, plugin.name, "WARNING")
}

fun Context.pluginError(plugin: Plugin, message: String?, throwable: Throwable?) = scope.launch {
    logger.log(message, plugin.name, "ERROR")
    logger.log(throwable?.stackTraceToString(), plugin.name, "ERROR")
}
