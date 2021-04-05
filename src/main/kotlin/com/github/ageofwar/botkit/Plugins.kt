package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.TelegramApi
import kotlinx.coroutines.CoroutineScope

typealias Plugins = MutableMap<String, Plugin>

suspend fun Plugins.init(scope: CoroutineScope, api: TelegramApi, logger: Loggers) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val (name, plugin) = iterator.next()
        try {
            plugin.init(api, logger.toPluginLogger(name, scope))
        } catch (e: Throwable) {
            iterator.remove()
            logger.log(PluginInitError(name, e))
        }
    }
}

suspend fun Plugins.close(scope: CoroutineScope, api: TelegramApi, logger: Loggers) {
    forEach { (name, plugin) ->
        try {
            plugin.close(api, logger.toPluginLogger(name, scope))
        } catch (e: Throwable) {
            logger.log(PluginCloseError(name, e))
        }
    }
}

suspend fun Pair<String, Plugin>.init(scope: CoroutineScope, api: TelegramApi, logger: Loggers): Boolean {
    val (name, plugin) = this
    return try {
        plugin.init(api, logger.toPluginLogger(name, scope))
        true
    } catch (e: Throwable) {
        logger.log(PluginInitError(name, e))
        false
    }
}

suspend fun Pair<String, Plugin>.close(scope: CoroutineScope, api: TelegramApi, logger: Loggers): Boolean {
    val (name, plugin) = this
    return try {
        plugin.close(api, logger.toPluginLogger(name, scope))
        true
    } catch (e: Throwable) {
        logger.log(PluginCloseError(name, e))
        false
    }
}
