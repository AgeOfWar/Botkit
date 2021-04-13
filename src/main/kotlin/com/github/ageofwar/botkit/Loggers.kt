package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Loggers(
    @SerialName("log_format") private val logFormat: String,
    private vararg val loggers: Logger,
    val strings: Strings
) {
    suspend fun log(event: LoggerEvent) {
        log(event.message(strings)?.template(event), event.category, event.level)
        log(event.throwable?.stackTraceToString(), event.category, event.level)
    }
    
    suspend fun log(message: String, category: String, level: String) = supervisorScope {
        loggers.forEach {
            launch {
                val args = mapOf(
                    "message" to message,
                    "date" to java.util.Date(),
                    "category" to category,
                    "level" to level
                )
                it.log(logFormat.template(args))
            }
        }
    }
    
    suspend fun init() = coroutineScope {
        loggers.forEach {
            launch {
                it.init()
            }
        }
    }
    
    suspend fun close() = supervisorScope {
        loggers.forEach {
            launch {
                it.close()
            }
        }
    }
}

suspend fun Loggers.log(message: String?, category: String, level: String) {
    if (message != null) log(message, category, level)
}
