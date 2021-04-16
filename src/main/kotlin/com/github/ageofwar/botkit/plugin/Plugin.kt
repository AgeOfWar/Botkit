package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.*
import com.github.ageofwar.botkit.files.readFileAs
import com.github.ageofwar.botkit.files.readFileOrCopy
import com.github.ageofwar.ktelegram.UpdateHandler
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class Plugin {
    val api get() = context.api
    lateinit var name: String internal set
    lateinit var dataFolder: File internal set
    lateinit var file: File internal set
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
    
    open suspend fun init() {}
    open suspend fun close() {}
    
    fun registerOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.add(handler)
    fun unregisterOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.remove(handler)
    fun registerUpdateHandler(handler: UpdateHandler) = updateHandlers.add(handler)
    fun unregisterUpdateHandler(handler: UpdateHandler) = updateHandlers.remove(handler)
    fun registerCommand(name: String, description: String) = commands.put(name, description)
    fun reloadCommands() = context.reloadCommands()
    
    suspend fun dispatchConsoleCommand(input: String) {
        handleCommand(input, context)
    }
    
    fun log(message: String?) = context.pluginLog(this, message)
    fun warning(message: String?) = context.pluginWarning(this, message)
    fun error(message: String?, throwable: Throwable? = null) = context.pluginError(this, message, throwable)
    
    private inline fun <T : Plugin, R> withPlugin(`class`: Class<out T>, block: T.() -> R): R {
        val plugin = context.plugins.values.filterIsInstance(`class`).firstOrNull()
            ?: throw PluginNotFoundException(`class`)
        return plugin.block()
    }
    fun <T : Plugin, R> withPlugin(`class`: KClass<out T>, block: T.() -> R) = withPlugin(`class`.java, block)
    inline fun <reified T : Plugin, R> withPlugin(noinline block: T.() -> R): R = withPlugin(T::class, block)
    
    suspend inline fun <reified T> readFileAs(
        file: String,
        crossinline exceptionHandler: (Throwable) -> T
    ): T = json.readFileAs(dataFolder.resolve(file), exceptionHandler)
    suspend inline fun <reified T> readFileAs(
        file: String,
        default: T,
        crossinline exceptionHandler: (Throwable) -> T
    ): T = json.readFileAs(dataFolder.resolve(file), default, exceptionHandler)
    suspend inline fun <reified T> readFileOrCopy(
        file: String,
        defaultPath: String,
        crossinline exceptionHandler: (Throwable) -> T
    ): T = json.readFileOrCopy(dataFolder.resolve(file), defaultPath, javaClass.classLoader, exceptionHandler)
}

class PluginNotFoundException(`class`: Class<*>) : Exception("Plugin $`class` not found")

fun Plugin.registerOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { registerOldUpdateHandler(it) }
fun Plugin.unregisterOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { unregisterOldUpdateHandler(it) }
fun Plugin.registerUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { registerUpdateHandler(it) }
fun Plugin.unregisterUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { unregisterUpdateHandler(it) }
fun Plugin.registerCommands(commands: Map<String, String>) = commands.forEach { (name, description) -> registerCommand(name, description) }
fun Plugin.registerCommands(vararg commands: Pair<String, String>) = commands.forEach { (name, description) -> registerCommand(name, description) }
fun Plugin.registerAndReloadCommands(commands: Map<String, String>) {
    commands.forEach { (name, description) -> registerCommand(name, description) }
    reloadCommands()
}
