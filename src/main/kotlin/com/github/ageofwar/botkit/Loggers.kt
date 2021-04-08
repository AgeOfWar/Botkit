package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import com.github.ageofwar.botkit.plugin.PluginLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoggerFormat(
    @SerialName("bot_start") val botStart: String?,
    @SerialName("bot_stop") val botStop: String?,
    @SerialName("old_update") val oldUpdate: String?,
    @SerialName("new_update") val newUpdate: String?,
    @SerialName("plugin_init_error") val pluginInitError: String?,
    @SerialName("plugin_update_error") val pluginUpdateError: String?,
    @SerialName("plugin_close_error") val pluginCloseError: String?,
    @SerialName("command_received") val commandReceived: String?,
    @SerialName("command_error") val commandError: String?,
    val commands: Commands
) {
    @Serializable
    data class Commands(
        val unknown: Unknown,
        val stop: Stop,
        val enable: Enable,
        val disable: Disable,
        val plugins: Plugins,
        val reload: Reload
    ) {
        @Serializable
        data class Unknown(
            val message: String?
        )
        
        @Serializable
        data class Stop(
            val message: String?
        )
        
        @Serializable
        data class Enable(
            val usage: String?,
            @SerialName("already_enabled") val alreadyEnabled: String?,
            @SerialName("cannot_load") val cannotLoad: String?,
            @SerialName("cannot_enable") val cannotEnable: String?,
            val enabled: String?
        )
        
        @Serializable
        data class Disable(
            val usage: String?,
            @SerialName("not_enabled") val notEnabled: String?,
            @SerialName("cannot_disable") val cannotDisable: String?,
            @SerialName("disabled") val disabled: String?
        )
        
        @Serializable
        data class Plugins(
            val message: String?
        )
        
        @Serializable
        data class Reload(
            @SerialName("not_enabled") val notEnabled: String?,
            @SerialName("cannot_load") val cannotLoad: String?,
            @SerialName("cannot_enable") val cannotEnable: String?,
            @SerialName("cannot_disable") val cannotDisable: String?,
            @SerialName("plugin_reloaded") val pluginReloaded: String?,
            @SerialName("plugins_reloaded") val pluginsReloaded: String?
        )
    }
}

@Serializable
class Loggers(
    @SerialName("log_format") private val logFormat: String,
    private val format: LoggerFormat,
    private vararg val loggers: Logger
) {
    suspend fun log(event: LoggerEvent) {
        log(event.message(format)?.template(event), event.category, event.level)
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
    
    suspend fun init() = supervisorScope {
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

suspend inline fun Loggers.use(block: (Loggers) -> Unit) {
    init()
    block(this)
    close()
}

fun Loggers.toPluginLogger(name: String, scope: CoroutineScope) = object : PluginLogger {
    override fun info(message: String) {
        scope.launch { log(message, category = name, level = "INFO") }
    }
    
    override fun warning(message: String) {
        scope.launch { log(message, category = name, level = "WARNING") }
    }
    
    override fun error(message: String) {
        scope.launch { log(message, category = name, level = "ERROR") }
    }
}