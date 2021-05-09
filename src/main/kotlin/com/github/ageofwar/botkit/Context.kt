package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.*
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.BotCommand
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.setMyCommands
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

data class Context(
    val api: TelegramApi,
    val logger: Loggers,
    val scope: CoroutineScope,
    val plugins: Plugins,
    val pluginsDirectory: Path,
    val commands: ConsoleCommands,
    val consoleCommandChannel: Channel<String>
)

suspend fun Context.getAvailablePluginsNames() = pluginsDirectory.availablePluginsNames()
suspend fun Context.getAvailablePlugins() = pluginsDirectory.availablePlugins()
fun Context.searchPlugin(name: String) = plugins.search(name)
fun Context.searchPluginName(name: String) = plugins.searchName(name)
suspend fun Context.searchAvailablePlugin(name: String) = pluginsDirectory.searchAvailablePlugin(name)

suspend fun Context.enablePlugin(file: Path): Plugin? {
    val fileName = file.nameWithoutExtension
    if (!file.isRegularFile()) {
        log(PluginLoadError(fileName, PluginLoadException("'$fileName' is not a valid plugin")))
        return null
    }
    val plugin = try {
        loadPlugin(file)
    } catch (e: PluginLoadException) {
        log(PluginLoadError(fileName, e))
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

suspend fun Context.enablePlugins(files: Iterable<Path>): List<Plugin> {
    return files.mapNotNull { enablePlugin(it) }
}

suspend fun Context.disablePlugin(name: String): Plugin? {
    val plugin = plugins[name]
    if (plugin == null) {
        log(PluginNotEnabled(name))
        return null
    }
    plugins -= plugin.name
    plugin.close(this)
    return plugin
}

suspend fun Context.disablePlugins(names: Iterable<String>): List<Plugin> {
    return names.mapNotNull { disablePlugin(it) }
}

suspend fun Context.reloadPlugin(name: String): Plugin? {
    val plugin = disablePlugin(name) ?: return null
    return enablePlugin(plugin.file)
}

suspend fun Context.reloadPlugins(names: Iterable<String>): List<Plugin> {
    return names.mapNotNull { reloadPlugin(it) }
}

fun Context.addDefaultConsoleCommands() = commands.putAll(arrayOf(
    "help" to HelpCommand(this),
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
