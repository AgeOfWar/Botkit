package com.github.ageofwar.botkit.files

import com.github.ageofwar.botkit.Context
import com.github.ageofwar.botkit.PluginLoadError
import com.github.ageofwar.botkit.Plugins
import com.github.ageofwar.botkit.plugin.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLClassLoader
import java.util.*

suspend fun Plugins.loadPlugins(directory: File, context: Context) = withContext(Dispatchers.IO) {
    directory.mkdirs()
    val files = directory.listFiles() ?: error("$directory is not a directory!")
    files.filter { it.isFile && it.extension == "jar" }.forEach {
        try {
            val plugin = loadPlugin(it, context)
            put(plugin.name, plugin)
        } catch (e: PluginLoadException) {
            val error = context.logger.strings.pluginLoadError?.template(PluginLoadError(it.nameWithoutExtension, e.message))
            System.err.println(error)
        }
    }
}

suspend fun loadPlugin(file: File, context: Context): Plugin = withContext(Dispatchers.IO) {
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
        this.dataFolder = file.parentFile.resolve(this.name)
        this.file = file
        this.context = context
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
