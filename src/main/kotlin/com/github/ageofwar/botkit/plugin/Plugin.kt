package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.Plugins
import com.github.ageofwar.botkit.updateMyCommands
import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.UpdateHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

abstract class Plugin {
    lateinit var name: String internal set
    lateinit var api: TelegramApi internal set
    lateinit var logger: PluginLogger internal set
    lateinit var dataFolder: File internal set
    lateinit var scope: CoroutineScope internal set
    
    internal lateinit var plugins: Plugins
    internal lateinit var file: File
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
    fun reloadCommands() = scope.launch {
        try {
            api.updateMyCommands(plugins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.error("An error occurred while reloading commands", e)
        }
    }
}

interface PluginLogger {
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String? = null, throwable: Throwable? = null)
}

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
