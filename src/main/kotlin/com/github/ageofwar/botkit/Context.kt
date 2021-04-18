package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.PluginLoadException
import com.github.ageofwar.botkit.files.findPluginsNames
import com.github.ageofwar.botkit.files.loadPlugin
import com.github.ageofwar.botkit.files.search
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.BotCommand
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.setMyCommands
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

data class Context(
    val api: TelegramApi,
    val logger: Loggers,
    val scope: CoroutineScope,
    val plugins: Plugins,
    val pluginsDirectory: File,
    val commands: ConsoleCommands,
    val consoleCommandChannel: Channel<String>
)

suspend fun Context.getAvailablePlugins() = pluginsDirectory.findPluginsNames()

suspend fun Context.enablePlugin(name: String): Plugin? {
    val file = pluginsDirectory.search(name)
    if (file == null) {
        log(PluginLoadError(name, "Plugin not found"))
        return null
    }
    val plugin = try {
        loadPlugin(file, this)
    } catch (e: PluginLoadException) {
        log(PluginLoadError(name, e.message))
        return null
    }
    if (plugin.name in plugins) {
        log(PluginAlreadyEnabled(plugin.name))
        return null
    }
    if (plugin.init(this)) {
        plugins[plugin.name] = plugin
        return plugin
    }
    return null
}

suspend fun Context.enablePlugins(vararg names: String): List<Plugin> {
    return names.mapNotNull { enablePlugin(it) }
}

suspend fun Context.disablePlugin(name: String): Plugin? {
    val plugin = plugins.search(name)
    if (plugin == null) {
        log(PluginNotEnabled(name))
        return null
    }
    plugins -= name
    plugin.close(this)
    return plugin
}

suspend fun Context.disablePlugins(vararg names: String): List<Plugin> {
    return names.mapNotNull { disablePlugin(it) }
}

suspend fun Context.reloadPlugin(name: String): Plugin? {
    val plugin = plugins.search(name)
    if (plugin == null) {
        log(PluginNotEnabled(name))
        return null
    }
    plugins -= name
    plugin.close(this)
    val reloadedPlugin = try {
        loadPlugin(plugin.file, this)
    } catch (e: PluginLoadException) {
        log(PluginLoadError(name, e.message))
        return null
    }
    if (plugin.name in plugins) {
        log(PluginAlreadyEnabled(plugin.name))
        return null
    }
    if (plugin.init(this)) {
        plugins[plugin.name] = plugin
        return reloadedPlugin
    }
    return null
}

suspend fun Context.reloadPlugins(vararg names: String): List<Plugin> {
    return names.mapNotNull { reloadPlugin(it) }
}

fun Context.addDefaultConsoleCommands() = commands.putAll(arrayOf(
    "stop" to StopCommand(this),
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
