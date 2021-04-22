package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.loadPlugins
import com.github.ageofwar.botkit.files.readFileOrCopy
import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.getMe
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

fun main(vararg args: String) = runBlocking {
    val (overrideToken) = parseArgs(*args)
    val plugins = ConcurrentHashMap<String, Plugin>()
    val json = buildJson(plugins)
    val logger = loadLogger(plugins, json)
    val (token, apiUrl) = loadBotConfig(overrideToken, json)
    token ?: error("Insert 'bot_token' field into bot.json")
    val botkit = loadBotkitConfig(json)
    val api = TelegramApi(token, apiUrl)
    val bot = api.getMe()
    val pluginsDirectory = File("plugins")
    val commands = ConcurrentHashMap<String, ConsoleCommand>()
    val consoleCommandChannel = Channel<String>()
    val context = Context(api, logger, this, plugins, pluginsDirectory, commands, consoleCommandChannel)
    context.addDefaultConsoleCommands()
    plugins.loadPlugins(context)
    logger.use {
        plugins.init(logger)
        context.reloadCommands()
        logger.log(BotStart(bot))
        val job = launch { botkit(api, logger, botkit, plugins) }
        launch { sendCommands(context) }
        listenCommands(context)
        logger.log(BotStop(bot))
        job.cancelAndJoin()
        plugins.close(logger)
    }
}

private suspend fun Plugins.loadPlugins(context: Context) {
    print("Loading plugins... ")
    loadPlugins(File("plugins"), context)
    when (size) {
        0 -> println("No plugin found")
        1 -> println("1 plugin found (${keys.single()})")
        else -> println("$size plugins found (${keys.joinToString(", ") { "'$it'" }})")
    }
}

private suspend fun loadLogger(plugins: Plugins, json: Json): Loggers {
    print("Loading loggers... ")
    val logger = json.readFileOrCopy<SerializableLoggers>(File("loggers.json"), "loggers.json") {
        if (it is SerializationException) {
            error("Invalid loggers.json file. Please update loggers.json or delete it")
        } else {
            throw RuntimeException("An error occurred while loading loggers", it)
        }
    }
    val loggers = logger.loggers + PluginsLogger(plugins)
    println("Done")
    return Loggers(logger.logFormat, loggers, logger.strings)
}

private suspend fun loadBotConfig(overrideToken: String?, json: Json): BotConfig {
    print("Loading bot information... ")
    val config = json.readFileOrCopy<BotConfig>(File("bot.json"), "bot.json") {
        if (it is SerializationException) {
            error("Invalid bot.json file. Please update your bot.json or delete it")
        } else {
            throw RuntimeException("An error occurred while loading bot information", it)
        }
    }
    val token = overrideToken ?: config.token
    if (token != null) println("${token.censureToken()} (${config.apiUrl})")
    return config.copy(token = token)
}

private suspend fun loadBotkitConfig(json: Json): BotkitConfig {
    print("Loading bot configuration... ")
    val botkit = json.readFileOrCopy<BotkitConfig>(File("botkit.json"), "botkit.json") {
        if (it is SerializationException) {
            error("Invalid botkit.json file. Please update your botkit.json or delete it")
        } else {
            throw RuntimeException("An error occurred while loading bot configuration", it)
        }
    }
    println(botkit)
    return botkit
}

private suspend inline fun Loggers.use(block: () -> Unit) {
    print("Initializing loggers... ")
    init()
    println("Done")
    block()
    print("Closing loggers... ")
    close()
    println("Done")
}

private fun error(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}

private fun String.censureToken() = mapIndexed { i, c ->
    if (c == ':' || i > lastIndex - 3) c else '*'
}.joinToString("")

private fun buildJson(plugins: Plugins) = Json {
    isLenient = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(PluginsSerializer(plugins))
    }
}
