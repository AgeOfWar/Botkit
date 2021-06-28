package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.*
import com.github.ageofwar.ktelegram.BotCommand
import com.github.ageofwar.ktelegram.BotCommandScope
import com.github.ageofwar.ktelegram.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class Plugin {
    val api get() = context.api
    lateinit var dataFolder: Path internal set
    lateinit var name: String internal set
    lateinit var url: URL internal set
    lateinit var scope: CoroutineScope internal set
    val json = Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    internal lateinit var context: Context
    internal val oldUpdateHandlers = CopyOnWriteArrayList<PluginUpdateHandler>()
    internal val updateHandlers = CopyOnWriteArrayList<PluginUpdateHandler>()
    internal val defaultBotCommands = ConcurrentHashMap<BotCommandScope, MutableList<BotCommand>>()
    internal val botCommands = ConcurrentHashMap<String, MutableMap<BotCommandScope, MutableList<BotCommand>>>()
    internal val loggers = CopyOnWriteArrayList<PluginLogger>()
    internal val commands = ConcurrentHashMap<String, PluginCommand>()
    
    open suspend fun init() {}
    open suspend fun close() {}
    
    fun registerOldUpdateHandler(handler: PluginUpdateHandler) { oldUpdateHandlers.add(handler) }
    fun unregisterOldUpdateHandler(handler: PluginUpdateHandler) = oldUpdateHandlers.remove(handler)
    fun registerUpdateHandler(handler: PluginUpdateHandler) { updateHandlers.add(handler) }
    fun unregisterUpdateHandler(handler: PluginUpdateHandler) = updateHandlers.remove(handler)
    fun registerLogger(logger: PluginLogger) { loggers.add(logger) }
    fun unregisterLogger(logger: PluginLogger) = loggers.remove(logger)
    fun registerCommand(name: String, handler: PluginCommand) = commands.put(name, handler)
    fun unregisterCommand(name: String) = commands.remove(name)
    fun registerBotCommands(scope: BotCommandScope = BotCommandScope.Default, vararg commands: BotCommand) {
        if (commands.isEmpty()) return
        if (!defaultBotCommands.containsKey(scope)) defaultBotCommands[scope] = mutableListOf()
        defaultBotCommands[scope]?.addAll(commands)
    }
    fun registerBotCommands(languageCode: String, scope: BotCommandScope = BotCommandScope.Default, vararg commands: BotCommand) {
        if (commands.isEmpty()) return
        if (!botCommands.containsKey(languageCode)) botCommands[languageCode] = mutableMapOf()
        if (!botCommands[languageCode]!!.containsKey(scope)) botCommands[languageCode]!![scope] = mutableListOf()
        botCommands[languageCode]!![scope]?.addAll(commands)
    }
    fun unregisterBotCommands(scope: BotCommandScope = BotCommandScope.Default) = defaultBotCommands.remove(scope)
    fun unregisterBotCommands(languageCode: String, scope: BotCommandScope = BotCommandScope.Default) = botCommands[languageCode]?.remove(scope) ?: false
    fun unregisterBotCommands(languageCode: String) = botCommands.remove(languageCode)
    
    fun reloadBotCommands() = context.reloadBotCommands()
    
    suspend fun dispatchCommand(input: String) {
        context.consoleCommandChannel.send(input)
    }
    
    fun info(message: String?) { context.pluginInfo(this, message) }
    fun warning(message: String?) { context.pluginWarning(this, message) }
    fun error(message: String?, throwable: Throwable? = null) { context.pluginError(this, message, throwable) }
    
    private inline fun <T : Plugin, R> withPlugin(`class`: Class<out T>, block: T.() -> R): R {
        val plugin = context.plugins.values.filterIsInstance(`class`).firstOrNull()
            ?: throw PluginNotFoundException(`class`)
        return plugin.block()
    }
    fun <T : Plugin, R> withPlugin(`class`: KClass<out T>, block: T.() -> R) = withPlugin(`class`.java, block)
    inline fun <reified T : Plugin, R> withPlugin(noinline block: T.() -> R): R = withPlugin(T::class, block)
}

fun interface PluginLogger {
    suspend fun Plugin.log(message: String)
}

fun interface PluginCommand {
    suspend fun Plugin.handle(name: String, args: String)
    
    val usage: String? get() = null
    val description: String? get() = null
    
    fun Plugin.logUsage(name: String) {
        val usage = usage ?: return info("Incorrect usage")
        info("Usage: $name $usage")
    }
}

fun interface PluginUpdateHandler {
    suspend fun Plugin.handle(update: Update)
}

class PluginException(val plugin: Plugin, message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
class PluginNotFoundException(`class`: Class<*>) : Exception("Plugin $`class` not found")

fun Plugin.exception(message: String? = null, cause: Throwable? = null): Nothing = throw PluginException(this, message, cause)

fun Plugin.registerOldUpdateHandlers(vararg handlers: PluginUpdateHandler) = handlers.forEach { registerOldUpdateHandler(it) }
fun Plugin.unregisterOldUpdateHandlers(vararg handlers: PluginUpdateHandler) = handlers.forEach { unregisterOldUpdateHandler(it) }
fun Plugin.registerUpdateHandlers(vararg handlers: PluginUpdateHandler) = handlers.forEach { registerUpdateHandler(it) }
fun Plugin.unregisterUpdateHandlers(vararg handlers: PluginUpdateHandler) = handlers.forEach { unregisterUpdateHandler(it) }

fun Plugin.registerLoggers(vararg loggers: PluginLogger) = loggers.forEach { registerLogger(it) }
fun Plugin.unregisterLoggers(vararg loggers: PluginLogger) = loggers.forEach { unregisterLogger(it) }

fun Plugin.registerCommands(commands: Map<String, PluginCommand>) = commands.forEach { (name, handler) -> registerCommand(name, handler) }
fun Plugin.unregisterCommands(vararg names: String) = names.forEach { unregisterCommand(it) }
