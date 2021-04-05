package com.github.ageofwar.botkit.files

import com.github.ageofwar.botkit.plugin.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLClassLoader
import java.util.*

suspend fun loadPlugins(directory: String) = withContext(Dispatchers.IO) {
    val folder = File(directory)
    folder.mkdirs()
    val files = File(directory).listFiles() ?: error("$directory is not a directory!")
    val urls = files.filter {
        it.isFile && it.extension == "jar"
    }.map { it.toURI().toURL() }.toTypedArray()
    sequence<Pair<String, Plugin>> {
        for (url in urls) {
            val name = url.path.substringAfterLast('/').removeSuffix(".jar")
            val loader = URLClassLoader(arrayOf(url))
            val plugin = ServiceLoader.load(Plugin::class.java, loader).findFirst().orElse(null)
            if (plugin != null) {
                yield(name to plugin)
            }
        }
    }
}

suspend fun loadPlugin(path: String): Plugin? = withContext(Dispatchers.IO) {
    val file = File(path)
    val url = file.toURI().toURL()
    val loader = URLClassLoader(arrayOf(url))
    ServiceLoader.load(Plugin::class.java, loader).findFirst().orElse(null)
}
