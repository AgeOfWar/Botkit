package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.*
import kotlinx.coroutines.coroutineScope

suspend fun botkit(
    api: TelegramApi,
    logger: Loggers,
    config: BotkitConfig,
    plugins: Map<String, Plugin>
) {
    when (config) {
        is LongPollingConfig -> longPollingBot(api, logger, config, plugins)
    }
}

suspend fun longPollingBot(
    api: TelegramApi,
    logger: Loggers,
    config: LongPollingConfig,
    plugins: Map<String, Plugin>
) = coroutineScope {
    val (backOff) = config
    val exceptionHandlers = arrayOf<ExceptionHandler>(ErrorLogger(logger))
    val oldUpdates = getPreviousUpdates(api, exceptionHandlers = exceptionHandlers)
    oldUpdates.handle(
        OldUpdateLogger(logger),
        OldUpdatePluginsHandler(logger, plugins),
        exceptionHandlers = exceptionHandlers
    )
    val newUpdates = getUpdates(api, null, { backOff }, exceptionHandlers = exceptionHandlers)
    newUpdates.handle(
        NewUpdateLogger(logger),
        NewUpdatePluginsHandler(logger, plugins),
        exceptionHandlers = exceptionHandlers
    )
}
