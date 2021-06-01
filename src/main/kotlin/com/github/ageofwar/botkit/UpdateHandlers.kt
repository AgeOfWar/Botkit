package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.plugin.Plugin
import com.github.ageofwar.ktelegram.ExceptionHandler
import com.github.ageofwar.ktelegram.Update
import com.github.ageofwar.ktelegram.UpdateHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.CancellationException

class ErrorLogger(private val logger: Loggers) : ExceptionHandler {
    override suspend fun handle(exception: Throwable) {
        logger.log(LongPollingError(exception))
    }
}

class OldUpdateLogger(private val logger: Loggers) : UpdateHandler {
    override suspend fun handle(update: Update) {
        logger.log(OldUpdate(update))
    }
}

class NewUpdateLogger(private val logger: Loggers) : UpdateHandler {
    override suspend fun handle(update: Update) {
        logger.log(NewUpdate(update))
    }
}

class OldUpdatePluginsHandler(
    private val logger: Loggers,
    private val plugins: Map<String, Plugin>
) : UpdateHandler {
    override suspend fun handle(update: Update) {
        for ((name, plugin) in plugins) {
            val handlers = plugin.oldUpdateHandlers
            supervisorScope {
                handlers.forEach {
                    launch(CoroutineName("update handler - $name")) {
                        try {
                            it.handle(update)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            logger.log(PluginUpdateError(name, e, update))
                        }
                    }
                }
            }
        }
    }
}

class NewUpdatePluginsHandler(
    private val logger: Loggers,
    private val plugins: Map<String, Plugin>
) : UpdateHandler {
    override suspend fun handle(update: Update) {
        for ((name, plugin) in plugins) {
            val handlers = plugin.updateHandlers
            supervisorScope {
                handlers.forEach {
                    launch(CoroutineName("update handler - $name")) {
                        try {
                            it.handle(update)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            logger.log(PluginUpdateError(name, e, update))
                        }
                    }
                }
            }
        }
    }
}
