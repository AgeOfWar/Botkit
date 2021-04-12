package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.BotCommand
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.setMyCommands

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

suspend fun Plugin.init(logger: Loggers): Boolean {
    return try {
        registerCommands()
        init()
        true
    } catch (e: Throwable) {
        logger.log(PluginInitError(name, e))
        false
    }
}

suspend fun Plugin.close(logger: Loggers): Boolean {
    return try {
        close()
        true
    } catch (e: Throwable) {
        logger.log(PluginCloseError(name, e))
        false
    } finally {
        (javaClass.classLoader as? AutoCloseable)?.close()
    }
}

suspend fun TelegramApi.updateMyCommands(plugins: Plugins) {
    val commands = plugins.flatMap { (_, plugin) ->
        plugin.commands.map { BotCommand(it.key, it.value) }
    }
    setMyCommands(commands)
}

fun Plugin.registerCommands(fileName: String = "commands.txt") {
    val commandsFile = dataFolder.resolve(fileName)
    if (commandsFile.exists()) {
        val commands = commandsFile.readLines().mapNotNull {
            val line = it.trim()
            if (line.isNotEmpty()) {
                val parts = it.split(Regex("\\s*-\\s*"), limit = 2)
                check(parts.size == 2) { "Invalid file format for '$fileName', missing - separator" }
                val (name, description) = parts
                check(name.all { c -> c.isJavaIdentifierPart() }) { "Invalid file format for '$fileName', invalid command name '$name'" }
                name to description
            } else null
        }
        commands.forEach { (name, description) -> registerCommand(name, description) }
    }
}
