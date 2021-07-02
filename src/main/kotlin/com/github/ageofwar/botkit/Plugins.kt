package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.*
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.BotCommand
import com.github.ageofwar.ktelegram.BotCommandScope
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.name

typealias Plugins = MutableMap<String, Plugin>

suspend fun Context.getAvailablePluginsNames() = pluginsDirectory.availablePluginsNames()
suspend fun Context.getAvailablePlugins() = pluginsDirectory.availablePlugins()
fun Context.searchPluginName(name: String) = plugins.searchName(name)
suspend fun Context.searchAvailablePlugin(name: String) = pluginsDirectory.searchAvailablePlugin(name)

suspend fun Context.enablePlugin(url: URL): Plugin? {
    val plugin = try {
        loadPlugin(url)
    } catch (e: PluginLoadException) {
        log(PluginLoadError(url.toString(), e))
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

suspend fun Context.enablePlugin(file: Path): Plugin? {
    val fileName = file.name
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
    return enablePlugin(plugin.url)
}

suspend fun Context.reloadPlugins(names: Iterable<String>): List<Plugin> {
    return names.mapNotNull { reloadPlugin(it) }
}

fun Plugins.search(name: String): Plugin? {
    val plugin = searchName(name)
    return if (plugin == null) null else get(plugin)
}

fun Plugins.searchName(name: String): String? {
    return keys.firstOrNull { it.startsWith(name, ignoreCase = true) }
}

suspend fun Plugin.init(context: Context): Boolean {
    return try {
        registerBotCommandsFromFile("commands")
        init()
        true
    } catch (e: Throwable) {
        context.log(PluginInitError(name, e))
        false
    }
}

suspend fun Plugin.close(context: Context): Boolean {
    return try {
        close()
        true
    } catch (e: Throwable) {
        context.log(PluginCloseError(name, e))
        false
    } finally {
        scope.cancel()
        (javaClass.classLoader as? AutoCloseable)?.close()
    }
}

suspend fun Plugin.registerBotCommandsFromFile(fileName: String) {
    if (!dataFolder.suspendExists()) return
    val defaultCommandsFile = dataFolder.resolve("$fileName.json")
    if (defaultCommandsFile.suspendExists()) {
        val defaultCommands = json.readFileAs<List<BotCommandWithScope>>(defaultCommandsFile) {
            error("An error occurred while deserializing bot commands", it)
            emptyList()
        }
        defaultCommands.forEach { (scope, commands) -> registerBotCommands(scope, *commands.toTypedArray()) }
    }
    dataFolder.suspendListDirectoryEntries("$fileName-??.json").forEach { path ->
        val languageCode = path.name.substring(fileName.length + 1, fileName.length + 3)
        val commands = json.readFileAs<List<BotCommandWithScope>>(path) {
            error("An error occurred while deserializing bot commands", it)
            emptyList()
        }
        commands.forEach { (scope, commands) -> registerBotCommands(languageCode, scope, *commands.toTypedArray()) }
    }
}

@Serializable
private data class BotCommandWithScope(val scope: BotCommandScope = BotCommandScope.Default, val commands: List<BotCommand> = emptyList())
