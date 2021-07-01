package com.github.ageofwar.botkit.files

import com.github.ageofwar.botkit.Context
import com.github.ageofwar.botkit.plugin.Plugin
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

val SUPPORTED_API_VERSIONS = arrayOf("2.3")

suspend fun Context.loadPlugin(url: URL): Plugin = withContext(Dispatchers.IO) {
    val loader = try {
        URLClassLoader(arrayOf(url))
    } catch (e: Throwable) {
        throw PluginLoadException(e.message ?: "Cannot access url '$url'", e)
    }
    val (name, pluginClassName, apiVersion) = loader.getResourceAsStream("botkit.properties")?.readPluginInfo()
        ?: throw PluginLoadException("Missing 'botkit.properties'")
    val plugin = loader.loadClass(pluginClassName)?.getConstructor()?.newInstance()
            as? Plugin ?: throw PluginLoadException("Cannot access '$pluginClassName' as ${Plugin::class.java.name} implementation")
    if (apiVersion !in SUPPORTED_API_VERSIONS) throw PluginLoadException("API version '$apiVersion' is not supported by this Botkit version (supported versions: ${SUPPORTED_API_VERSIONS.contentToString()})")
    plugin.apply {
        this.name = name
        this.url = url
        this.context = this@loadPlugin
        this.scope = CoroutineScope(CoroutineName(name) + SupervisorJob())
        this.dataFolder = pluginsDirectory.resolve(name)
    }
}

suspend fun Context.loadPlugin(file: Path): Plugin = loadPlugin(file.toUri().toURL())

suspend fun Path.availablePlugins(): List<Path> = withContext(Dispatchers.IO) {
    listDirectoryEntries("*.jar").filter { it.isRegularFile() }
}

suspend fun Path.availablePluginsNames(): List<String> = availablePlugins().map { it.name }

suspend fun Path.searchAvailablePlugin(name: String): Path? {
    val plugin = availablePluginsNames().firstOrNull { it.startsWith(name, ignoreCase = true) }
    return if (plugin == null) null else resolve(plugin)
}

class PluginLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

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
