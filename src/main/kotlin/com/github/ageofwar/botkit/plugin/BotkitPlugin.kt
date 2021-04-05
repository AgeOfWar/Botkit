package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.ktelegram.TelegramApi
import com.github.ageofwar.ktelegram.UpdateHandler
import java.util.concurrent.CopyOnWriteArrayList

abstract class Plugin {
    internal val oldUpdateHandlers = CopyOnWriteArrayList<UpdateHandler>()
    internal val updateHandlers = CopyOnWriteArrayList<UpdateHandler>()
    
    open suspend fun init(api: TelegramApi, logger: PluginLogger) {}
    open suspend fun close(api: TelegramApi, logger: PluginLogger) {}
    
    fun registerOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.add(handler)
    fun registerOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach {
        registerOldUpdateHandler(it)
    }
    fun unregisterOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.remove(handler)
    fun unregisterOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach {
        unregisterOldUpdateHandler(it)
    }
    
    fun registerUpdateHandler(handler: UpdateHandler) = updateHandlers.add(handler)
    fun registerUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach {
        registerUpdateHandler(it)
    }
    fun unregisterUpdateHandler(handler: UpdateHandler) = updateHandlers.remove(handler)
    fun unregisterUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach {
        unregisterUpdateHandler(it)
    }
}

interface PluginLogger {
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String)
}
