package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.*
import com.github.ageofwar.ktelegram.UpdateHandler
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class Plugin {
    val api get() = context.api
    lateinit var name: String internal set
    lateinit var dataFolder: File internal set
    lateinit var file: File internal set
    
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
    
    suspend fun dispatchConsoleCommand(input: String, unknownCommand: ConsoleCommand = UnknownCommand(context.logger)) {
        handleCommand(input, context, unknownCommand)
    }
    
    fun log(message: String?) = context.pluginLog(this, message)
    fun warning(message: String?) = context.pluginWarning(this, message)
    fun error(message: String?, throwable: Throwable? = null) = context.pluginError(this, message, throwable)
    
    fun <T : Plugin, R> withPlugin(`class`: Class<out T>, block: T.() -> R): R {
        val plugin = context.plugins.values.filterIsInstance(`class`).firstOrNull()
            ?: throw PluginNotFoundException(`class`)
        return plugin.block()
    }
    fun <T : Plugin, R> withPlugin(`class`: KClass<out T>, block: T.() -> R) = withPlugin(`class`.java, block)
    inline fun <reified T : Plugin, R> withPlugin(noinline block: T.() -> R): R = withPlugin(T::class, block)
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
