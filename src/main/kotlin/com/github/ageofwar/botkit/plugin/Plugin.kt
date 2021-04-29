package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.*
import com.github.ageofwar.botkit.files.readFileAs
import com.github.ageofwar.botkit.files.readFileOrCopy
import com.github.ageofwar.botkit.files.writeFile
import com.github.ageofwar.ktelegram.UpdateHandler
import freemarker.template.Configuration
import freemarker.template.Template
import kotlinx.serialization.json.Json
import java.io.File
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

abstract class Plugin {
    val api get() = context.api
    lateinit var name: String internal set
    lateinit var dataFolder: File internal set
    lateinit var file: File internal set
    val json = Json {
        isLenient = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    internal lateinit var context: Context
    internal val oldUpdateHandlers = CopyOnWriteArrayList<UpdateHandler>()
    internal val updateHandlers = CopyOnWriteArrayList<UpdateHandler>()
    internal val commands = ConcurrentHashMap<String, String>()
    internal val loggers = CopyOnWriteArrayList<PluginLogger>()
    internal val consoleCommands = ConcurrentHashMap<String, PluginCommand>()
    
    open suspend fun init() {}
    open suspend fun close() {}
    
    fun registerOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.add(handler)
    fun unregisterOldUpdateHandler(handler: UpdateHandler) = oldUpdateHandlers.remove(handler)
    fun registerUpdateHandler(handler: UpdateHandler) = updateHandlers.add(handler)
    fun unregisterUpdateHandler(handler: UpdateHandler) = updateHandlers.remove(handler)
    fun registerCommand(name: String, description: String) = commands.put(name, description)
    fun unregisterCommand(name: String) = commands.remove(name)
    fun registerLogger(logger: PluginLogger) = loggers.add(logger)
    fun unregisterLogger(logger: PluginLogger) = loggers.remove(logger)
    fun registerConsoleCommand(name: String, handler: PluginCommand) = consoleCommands.put(name, handler)
    fun unregisterConsoleCommand(name: String) = consoleCommands.remove(name)
    
    fun reloadCommands() = context.reloadCommands()
    
    suspend fun dispatchConsoleCommand(input: String) {
        context.consoleCommandChannel.send(input)
    }
    
    fun log(message: String?) = context.pluginLog(this, message)
    fun warning(message: String?) = context.pluginWarning(this, message)
    fun error(message: String?, throwable: Throwable? = null) = context.pluginError(this, message, throwable)
    
    private inline fun <T : Plugin, R> withPlugin(`class`: Class<out T>, block: T.() -> R): R {
        val plugin = context.plugins.values.filterIsInstance(`class`).firstOrNull()
            ?: throw PluginNotFoundException(`class`)
        return plugin.block()
    }
    fun <T : Plugin, R> withPlugin(`class`: KClass<out T>, block: T.() -> R) = withPlugin(`class`.java, block)
    inline fun <reified T : Plugin, R> withPlugin(noinline block: T.() -> R): R = withPlugin(T::class, block)
    
    suspend inline fun <reified T> readFileAs(
        file: String,
        crossinline exceptionHandler: (Throwable) -> T
    ): T = json.readFileAs(dataFolder.resolve(file), exceptionHandler)
    suspend inline fun <reified T> readFileAs(
        file: String,
        default: T,
        crossinline exceptionHandler: (Throwable) -> T
    ): T = json.readFileAs(dataFolder.resolve(file), default, exceptionHandler)
    suspend inline fun <reified T> readFileOrCopy(
        file: String,
        defaultPath: String,
        crossinline exceptionHandler: (Throwable) -> T
    ): T = json.readFileOrCopy(dataFolder.resolve(file), defaultPath, javaClass.classLoader, exceptionHandler)
    suspend inline fun <reified T> writeFile(
        file: String,
        content: T,
        crossinline exceptionHandler: (Throwable) -> Unit
    ): Unit = json.writeFile(dataFolder.resolve(file), content, exceptionHandler)
}

interface PluginLogger {
    suspend fun log(message: String)
}

interface PluginCommand {
    suspend fun handle(name: String, args: String)
}

class PluginException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
class PluginNotFoundException(`class`: Class<*>) : Exception("Plugin $`class` not found")

fun Plugin.registerOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { registerOldUpdateHandler(it) }
fun Plugin.unregisterOldUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { unregisterOldUpdateHandler(it) }
fun Plugin.registerUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { registerUpdateHandler(it) }
fun Plugin.unregisterUpdateHandlers(vararg handlers: UpdateHandler) = handlers.forEach { unregisterUpdateHandler(it) }
fun Plugin.registerCommands(commands: Map<String, String>) = commands.forEach { (name, description) -> registerCommand(name, description) }
fun Plugin.unregisterCommands(vararg commands: String) = commands.forEach { unregisterCommand(it) }
fun Plugin.registerAndReloadCommands(commands: Map<String, String>) {
    commands.forEach { (name, description) -> registerCommand(name, description) }
    reloadCommands()
}
fun Plugin.registerLoggers(vararg loggers: PluginLogger) = loggers.forEach { registerLogger(it) }
fun Plugin.unregisterLoggers(vararg loggers: PluginLogger) = loggers.forEach { unregisterLogger(it) }
fun Plugin.registerConsoleCommands(commands: Map<String, PluginCommand>) =
    commands.forEach { (name, handler) -> registerConsoleCommand(name, handler) }
fun Plugin.unregisterConsoleCommands(vararg names: String) = names.forEach { unregisterConsoleCommand(it) }

fun String.template(vararg args: Pair<String, Any?>): String {
    val reader = reader()
    val writer = StringWriter()
    val template = Template("Botkit", reader, Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS))
    template.process(args.toMap(), writer)
    return writer.toString()
}

fun exception(message: String? = null, cause: Throwable? = null): Nothing = throw PluginException(message, cause)
