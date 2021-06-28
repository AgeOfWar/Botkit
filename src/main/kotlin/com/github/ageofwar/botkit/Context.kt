package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.setMyCommands
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.file.Path

data class Context(
    val api: TelegramApi,
    val logger: Loggers,
    val scope: CoroutineScope,
    val plugins: Plugins,
    val pluginsDirectory: Path,
    val commands: ConsoleCommands,
    val consoleCommandChannel: Channel<String>
)

fun Context.addDefaultConsoleCommands() = commands.putAll(arrayOf(
    "help" to HelpCommand(this),
    "stop" to StopCommand(this),
    "enable" to EnablePluginCommand(this),
    "disable" to DisablePluginCommand(this),
    "plugins" to PluginsCommand(this),
    "reload" to ReloadPluginsCommand(this),
    "debug" to DebugCommand(this),
    "file" to FileCommand(this)
))

suspend fun Context.log(event: LoggerEvent) = logger.log(event)

fun Context.reloadBotCommands() = scope.launch {
    try {
        api.updateMyCommands(plugins)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.log(CommandReloadError(e))
    }
}

private suspend fun TelegramApi.updateMyCommands(plugins: Plugins) {
    val defaultCommandScopes = plugins.flatMap { (_, plugin) -> plugin.defaultBotCommands.keys }
    defaultCommandScopes.forEach { scope ->
        setMyCommands(scope, null, plugins.flatMap { (_, plugin) -> plugin.defaultBotCommands[scope] ?: emptyList()  })
    }
    val commandLanguages = plugins.flatMap { (_, plugin) -> plugin.botCommands.keys }
    commandLanguages.forEach { languageCode ->
        val commandScopes = plugins.flatMap { (_, plugin) -> plugin.botCommands[languageCode]?.keys ?: emptyList() }
        commandScopes.forEach { scope ->
            setMyCommands(scope, languageCode, plugins.flatMap { (_, plugin) -> plugin.botCommands[languageCode]?.get(scope) ?: emptyList()  })
        }
    }
}

fun Context.pluginInfo(plugin: Plugin, message: String?) = scope.launch {
    logger.log(message, plugin.name, "INFO")
}

fun Context.pluginWarning(plugin: Plugin, message: String?) = scope.launch {
    logger.log(message, plugin.name, "WARNING")
}

fun Context.pluginError(plugin: Plugin, message: String?, throwable: Throwable?) = scope.launch {
    logger.log(message, plugin.name, "ERROR")
    logger.log(throwable?.stackTraceToString(), plugin.name, "ERROR")
}
