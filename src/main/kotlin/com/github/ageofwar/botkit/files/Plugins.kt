package com.github.ageofwar.botkit.files

import com.github.ageofwar.botkit.Context
import com.github.ageofwar.botkit.PluginLoadError
import com.github.ageofwar.botkit.log
import com.github.ageofwar.botkit.plugin.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

val SUPPORTED_API_VERSIONS = arrayOf("0.3")

suspend fun Context.loadPlugins(directory: Path) = withContext(Dispatchers.IO) {
    directory.createDirectories()
    val files = directory.listDirectoryEntries()
    files.forEach {
        if (it.isRegularFile() && it.extension == "jar") {
            try {
                val plugin = loadPlugin(it)
                plugins[plugin.name] = plugin
            } catch (e: PluginLoadException) {
                log(PluginLoadError(it.nameWithoutExtension, e))
            }
        }
    }
}

suspend fun Context.loadPlugin(file: Path): Plugin = withContext(Dispatchers.IO) {
    if (!file.exists()) throw PluginLoadException("Plugin not found")
    val url = file.toUri().toURL()
    val loader = URLClassLoader(arrayOf(url))
    val (name, pluginClassName, apiVersion) = loader.getResourceAsStream("botkit.properties")?.readPluginInfo()
        ?: throw PluginLoadException("Missing 'botkit.properties'")
    val plugin = loader.loadClass(pluginClassName)?.getConstructor()?.newInstance()
            as? Plugin ?: throw PluginLoadException("Cannot access '$pluginClassName' as ${Plugin::class.java.name} implementation")
    if (apiVersion !in SUPPORTED_API_VERSIONS) throw PluginLoadException("API version '$apiVersion' is not supported by this Botkit version (supported versions: ${SUPPORTED_API_VERSIONS.contentToString()})")
    plugin.apply {
        this.name = name
        this.file = file
        this.context = this@loadPlugin
    }
}

suspend fun Path.availablePlugins(): List<Path> = withContext(Dispatchers.IO) {
    listDirectoryEntries().filter { it.isRegularFile() && it.extension == "jar" }
}

suspend fun Path.availablePluginsNames(): List<String> = availablePlugins().map { it.name }

suspend fun Path.searchAvailablePlugin(name: String): Path? {
    val plugin = availablePluginsNames().firstOrNull { it.startsWith(name.removeSuffix(".jar"), ignoreCase = true) }
    return if (plugin == null) null else resolve("$plugin.jar")
}

class PluginLoadException(override val message: String) : Exception(message)

data class PluginInfo(
    val name: String,
    val pluginClassName: String,
    val apiVersion: String
)

private fun InputStream.readPluginInfo() = use {
    val pluginInfo = Properties()
    pluginInfo.load(this)
    val name = pluginInfo.getProperty("name")
        ?: throw PluginLoadException("Missing 'name' property in 'botkit.properties'")
    val pluginClassName = pluginInfo.getProperty("pluginClassName")
        ?: throw PluginLoadException("Missing 'pluginClassName' property in 'botkit.properties'")
    val apiVersion = pluginInfo.getProperty("apiVersion") ?: "Unknown"
    PluginInfo(name, pluginClassName, apiVersion)
}
