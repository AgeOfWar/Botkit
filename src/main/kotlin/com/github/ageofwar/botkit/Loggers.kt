package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import com.github.ageofwar.botkit.plugin.PluginLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Loggers(
    private val logFormat: String,
    val strings: Strings,
    private vararg val loggers: Logger
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

@Serializable
data class SerializableLoggers(
    @SerialName("log_format") private val logFormat: String,
    private val loggers: Array<Logger>
) {
    fun toLoggers(strings: Strings) = Loggers(logFormat, strings, *loggers)
}

suspend fun Loggers.log(message: String?, category: String, level: String) {
    if (message != null) log(message, category, level)
}

fun Loggers.toPluginLogger(name: String, scope: CoroutineScope) = object : PluginLogger {
    override fun info(message: String) {
        scope.launch { log(message, category = name, level = "INFO") }
    }
    
    override fun warning(message: String) {
        scope.launch { log(message, category = name, level = "WARNING") }
    }
    
    override fun error(message: String?, throwable: Throwable?) {
        scope.launch {
            log(message, category = name, level = "ERROR")
            log(throwable?.stackTraceToString(), category = name, level = "ERROR")
        }
    }
}