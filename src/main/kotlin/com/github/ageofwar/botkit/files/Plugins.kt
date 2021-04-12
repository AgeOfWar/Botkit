package com.github.ageofwar.botkit.files

import com.github.ageofwar.botkit.Loggers
import com.github.ageofwar.botkit.PluginLoadError
import com.github.ageofwar.botkit.Plugins
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.botkit.toPluginLogger
import com.github.ageofwar.ktelegram.TelegramApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLClassLoader
import java.util.*

suspend fun Plugins.loadPlugins(directory: File, logger: Loggers, api: TelegramApi, scope: CoroutineScope) = withContext(Dispatchers.IO) {
    directory.mkdirs()
    val files = directory.listFiles() ?: error("$directory is not a directory!")
    files.filter { it.isFile && it.extension == "jar" }.forEach {
        try {
            val plugin = loadPlugin(it, logger, api, scope)
            put(plugin.name, plugin)
        } catch (e: PluginLoadException) {
            System.err.println(logger.strings.pluginLoadError?.template(PluginLoadError(it.nameWithoutExtension, e.message)))
        }
    }
}

suspend fun Plugins.loadPlugin(file: File, logger: Loggers, api: TelegramApi, scope: CoroutineScope): Plugin = withContext(Dispatchers.IO) {
    val url = file.toURI().toURL()
    val loader = URLClassLoader(arrayOf(url))
    val pluginInfo = Properties().apply {
        loader.getResourceAsStream("botkit.properties")?.use {
            load(it)
        } ?: throw PluginLoadException("Missing 'botkit.properties' in file ${file.nameWithoutExtension}")
    }
    val name = pluginInfo.getProperty("name")
        ?: throw PluginLoadException("Missing name property in 'botkit.properties' in ${file.nameWithoutExtension}")
    val pluginClassName = pluginInfo.getProperty("pluginClassName")
        ?: throw PluginLoadException("Missing pluginClassName property in 'botkit.properties' in ${file.nameWithoutExtension}")
    val plugin = loader.loadClass(pluginClassName)?.getConstructor()?.newInstance()
            as? Plugin ?: throw PluginLoadException("Cannot find ${Plugin::class.java.name} implementation in ${file.nameWithoutExtension}")
    plugin.apply {
        this.name = name
        this.api = api
        this.logger = logger.toPluginLogger(name, scope)
        this.dataFolder = file.parentFile.resolve(this.name)
        this.plugins = this@loadPlugin
        this.scope = scope
        this.file = file
    }
}

suspend fun findPluginsNames(directory: File): Sequence<String> = withContext(Dispatchers.IO) {
    sequence {
        directory.listFiles()?.forEach {
            if (it.isFile && it.extension == "jar") {
                yield(it.nameWithoutExtension)
            }
        }
    }
}

class PluginLoadException(override val message: String) : Exception(message)
