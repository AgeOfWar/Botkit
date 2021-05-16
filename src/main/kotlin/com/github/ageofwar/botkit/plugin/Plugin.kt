package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.*
import com.github.ageofwar.ktelegram.UpdateHandler
import kotlinx.serialization.json.Json
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class Plugin {
    val api get() = context.api
    val dataFolder: Path get() = context.pluginsDirectory.resolve(name)
    lateinit var name: String internal set
    lateinit var url: URL internal set
    val json = Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    internal lateinit var context: Context
    internal val oldUpdateHandlers = CopyOnWriteArrayList<UpdateHandler>()
    internal val updateHandlers = CopyOnWriteArrayList<UpdateHandler>()
    internal val commands = ConcurrentHashMap<String, String>()
    internal val loggers = CopyOnWriteArrayList<PluginLogger>()
    internal val consoleCommands = ConcurrentHashMap<String, PluginCommand>()
    
    open suspend fun init() {}
    open suspend fun close() {}
    
    fun registerOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.add(handler)
    fun unregisterOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.remove(handler)
    fun registerUpdateHandler(handler: UpdateHandler) = updateHandlers.add(handler)
    fun unregisterUpdateHandler(handler: UpdateHandler) = updateHandlers.remove(handler)
    fun registerCommand(name: String, description: String) = commands.put(name, description)
    fun unregisterCommand(name: String) = commands.remove(name)
    fun registerLogger(logger: PluginLogger) = loggers.add(logger)
    fun unregisterLogger(logger: PluginLogger) = loggers.remove(logger)
    fun registerConsoleCommand(name: String, handler: PluginCommand) = consoleCommands.put(name, handler)
    fun unregisterConsoleCommand(name: String) = consoleCommands.remove(name)
    
    fun reloadCommands() = context.reloadCommands()
    
    suspend fun dispatchConsoleCommand(input: String) {
        context.consoleCommandChannel.send(input)
    }
    
    fun log(message: String?) { context.pluginLog(this, message) }
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

interface PluginLogger {
    suspend fun log(message: String)
}

interface PluginCommand {
    suspend fun Plugin.handle(name: String, args: String)
    val usage: String? get() = null
    val description: String? get() = null
    
    fun Plugin.logUsage(name: String) {
        val usage = usage ?: return log("Incorrect usage")
        log("Usage: $name $usage")
    }
}

class PluginException(val plugin: Plugin, message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
class PluginNotFoundException(`class`: Class<*>) : Exception("Plugin $`class` not found")

fun Plugin.exception(message: String? = null, cause: Throwable? = null): Nothing = throw PluginException(this, message, cause)

fun Plugin.registerOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { registerOldUpdateHandler(it) }
fun Plugin.unregisterOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { unregisterOldUpdateHandler(it) }
fun Plugin.registerUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { registerUpdateHandler(it) }
fun Plugin.unregisterUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { unregisterUpdateHandler(it) }
fun Plugin.registerCommands(commands: Map<String, String>) = commands.forEach { (name, description) -> registerCommand(name, description) }
fun Plugin.unregisterCommands(vararg commands: String) = commands.forEach { unregisterCommand(it) }
fun Plugin.registerAndReloadCommands(commands: Map<String, String>) {
    commands.forEach { (name, description) -> registerCommand(name, description) }
    reloadCommands()
}
fun Plugin.registerLoggers(vararg loggers: PluginLogger) = loggers.forEach { registerLogger(it) }
fun Plugin.unregisterLoggers(vararg loggers: PluginLogger) = loggers.forEach { unregisterLogger(it) }
fun Plugin.registerConsoleCommands(commands: Map<String, PluginCommand>) =
    commands.forEach { (name, handler) -> registerConsoleCommand(name, handler) }
fun Plugin.unregisterConsoleCommands(vararg names: String) = names.forEach { unregisterConsoleCommand(it) }
