package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.lang.System.currentTimeMillis

@Serializable
data class SerializableLoggers(
    @SerialName("log_format") val logFormat: String,
    @SerialName("verbose_errors") val verboseErrors: Boolean,
    val loggers: List<Logger>
)

class Loggers(
    private val logFormat: String,
    private val loggers: List<Logger>,
    var verboseErrors: Boolean,
    val strings: Strings
) {
    suspend fun log(event: LoggerEvent) {
        if (verboseErrors) {
            log(event.message(strings)?.template("event" to event), event.category, event.level)
            log(event.throwable?.stackTraceToString(), event.category, event.level)
        } else {
            val message = event.message(strings)?.template("event" to event)
            val error = event.throwable?.message ?: event.throwable?.cause?.message
            when {
                message != null && error != null -> log("$message: $error", event.category, event.level)
                message != null -> log(message, event.category, event.level)
                error != null -> log(event.throwable!!.javaClass.simpleName + ": $error", event.category, event.level)
            }
        }
    }
    
    suspend fun log(message: String, category: String, level: String) = supervisorScope {
        loggers.forEach {
            launch {
                it.log(logFormat.template(
                    "message" to message,
                    "date" to currentTimeMillis(),
                    "category" to category,
                    "level" to level
                ))
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
