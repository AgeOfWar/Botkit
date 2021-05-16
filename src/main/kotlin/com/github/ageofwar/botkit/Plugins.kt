package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.*
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.botkit.plugin.readException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readLines

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
        registerCommands()
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
        (javaClass.classLoader as? AutoCloseable)?.close()
    }
}

suspend fun Plugin.registerCommands(fileName: String = "commands.txt") = withContext(Dispatchers.IO) {
    try {
        val commandsFile = dataFolder.resolve(fileName)
        if (commandsFile.exists()) {
            val commands = commandsFile.readLines().mapNotNull {
                val line = it.trim()
                if (line.isNotEmpty()) {
                    val parts = it.split(Regex("\\s*-\\s*"), limit = 2)
                    if (parts.size != 2) throw SerializationException("Invalid file format for '$fileName', missing - separator")
                    val (name, description) = parts
                    if (!name.all { c -> c.isJavaIdentifierPart() }) throw SerializationException("Invalid file format for '$fileName', invalid command name '$name'")
                    name to description
                } else null
            }
            commands.forEach { (name, description) -> registerCommand(name, description) }
        }
    } catch (e: Throwable) {
        readException(fileName, e)
    }
}
