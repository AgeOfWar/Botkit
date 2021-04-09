package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.*
import com.github.ageofwar.ktelegram.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

fun main(vararg args: String) = runBlocking {
    val (overrideToken, loggerName, workingDirectory) = parseArgs(*args)
    val plugins = loadPlugins(workingDirectory)
    val json = buildJson(workingDirectory)
    val logger = loadLogger(workingDirectory, loggerName, json)
    val (token, apiUrl) = loadBotConfig(workingDirectory, overrideToken, json)
    token ?: error("Insert 'bot_token' field into bot.json")
    val botkit = loadBotkitConfig(workingDirectory, json)
    logger.use {
        val api = TelegramApi(token, apiUrl)
        val bot = api.getMe()
        plugins.init(this, api, logger)
        logger.log(BotStart(bot))
        val job = launch { botkit(api, logger, botkit, plugins) }
        val commands = defaultCommands(logger, "$workingDirectory/plugins", plugins, this, api).toMap(ConcurrentHashMap())
        listenCommands(logger, commands)
        logger.log(BotStop(bot))
        job.cancelAndJoin()
        plugins.close(this, api, logger)
    }
}

private suspend fun loadPlugins(workingDirectory: String): Plugins {
    print("Loading plugins... ")
    val plugins = com.github.ageofwar.botkit.files.loadPlugins("$workingDirectory/plugins").toMap(ConcurrentHashMap())
    when (plugins.size) {
        0 -> println("No plugin found")
        1 -> println("1 plugin found (${plugins.keys.single()})")
        else -> println("${plugins.size} plugins found (${plugins.keys.joinToString(", ") { "'$it'" }})")
    }
    return plugins
}

private suspend fun loadLogger(workingDirectory: String, loggerName: String, json: Json): Loggers {
    print("Loading loggers... ")
    val loggerMap = json.readFileOrCopy<Map<String, Loggers>>("$workingDirectory/loggers.json", "loggers.json") {
        if (it is SerializationException) {
            error("Invalid loggers.json file. Please update loggers.json or delete it")
        } else {
            throw RuntimeException("An error occurred while loading loggers", it)
        }
    }
    val logger = loggerMap[loggerName] ?: error("Unknown logger '$loggerName'")
    println("Using logger '$loggerName'")
    return logger
}

private suspend fun loadBotConfig(workingDirectory: String, overrideToken: String?, json: Json): BotConfig {
    print("Loading bot information... ")
    val config = json.readFileOrCopy<BotConfig>("$workingDirectory/bot.json", "bot.json") {
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

private suspend fun loadBotkitConfig(workingDirectory: String, json: Json): BotkitConfig {
    print("Loading bot configuration... ")
    val botkit = json.readFileOrCopy<BotkitConfig>("$workingDirectory/botkit.json", "botkit.json") {
        if (it is SerializationException) {
            error("Invalid botkit.json file. Please update your botkit.json or delete it")
        } else {
            throw RuntimeException("An error occurred while loading bot configuration", it)
        }
    }
    println(botkit)
    return botkit
}

private fun error(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}

private fun String.censureToken() = mapIndexed { i, c ->
    if (c == ':' || i > lastIndex - 3) c else '*'
}.joinToString("")

private fun buildJson(workingDirectory: String) = Json {
    isLenient = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(PrefixSerializer("$workingDirectory/"))
    }
}
