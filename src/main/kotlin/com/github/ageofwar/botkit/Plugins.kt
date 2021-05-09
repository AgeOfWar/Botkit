package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.botkit.plugin.readException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlin.io.path.exists
import kotlin.io.path.readLines

typealias Plugins = MutableMap<String, Plugin>

suspend fun Plugins.init(logger: Loggers) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val (name, plugin) = iterator.next()
        try {
            plugin.registerCommands()
            plugin.init()
        } catch (e: Throwable) {
            iterator.remove()
            logger.log(PluginInitError(name, e))
        }
    }
}

suspend fun Plugins.close(logger: Loggers) {
    forEach { (name, plugin) ->
        try {
            plugin.close()
        } catch (e: Throwable) {
            logger.log(PluginCloseError(name, e))
        } finally {
            (plugin.javaClass.classLoader as? AutoCloseable)?.close()
        }
    }
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
