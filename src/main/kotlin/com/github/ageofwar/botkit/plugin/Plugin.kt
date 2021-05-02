package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.*
import com.github.ageofwar.botkit.files.readFileAs
import com.github.ageofwar.botkit.files.readFileOrCopy
import com.github.ageofwar.botkit.files.writeFile
import com.github.ageofwar.ktelegram.*
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.*
import java.io.File
import java.nio.charset.Charset
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
}

interface PluginLogger {
    suspend fun log(message: String)
}

interface PluginCommand {
    suspend fun handle(name: String, args: String)
}

class PluginException(val plugin: Plugin, message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
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
    try {
        template.process(args.toMap(), writer)
    } catch (e: TemplateException) {
        return this
    }
    return writer.toString()
}

fun Plugin.exception(message: String? = null, cause: Throwable? = null): Nothing = throw PluginException(this, message, cause)
fun Plugin.serializeException(file: String, cause: SerializationException): Nothing = exception("Cannot serialize '$file'", cause)
fun Plugin.deserializeException(file: String, cause: SerializationException): Nothing = exception("Cannot deserialize '$file' (update or delete it)", cause)
fun Plugin.ioException(file: String, cause: IOException): Nothing = exception("Cannot access '$file'", cause)
fun Plugin.readException(file: String, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> deserializeException(file, cause)
    is IOException -> ioException(file, cause)
    else -> exception(cause = cause)
}
fun Plugin.writeException(file: String, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> serializeException(file, cause)
    is IOException -> ioException(file, cause)
    else -> exception(cause = cause)
}

suspend fun Plugin.registerCommandHandler(
    vararg commandName: String,
    ignoreCase: Boolean = true,
    disableNotification: Boolean = false,
    allowSendingWithoutReply: Boolean = true,
    fileName: String = "${commandName.first()}.json",
    serializer: KSerializer<MessageContent<*>> = MessageContent.MarkdownSerializer
) = withContext(Dispatchers.IO) {
    try {
        val startFile = dataFolder.resolve(fileName)
        if (startFile.exists()) {
            val contents = json.decodeFromString(ListSerializer(serializer), startFile.readText())
            registerUpdateHandler(UpdateHandler { update ->
                update.handleCommand(api, *commandName, ignoreCase = ignoreCase) { message, _, _ ->
                    contents.forEach {
                        api.sendMessage(message.messageId, it, disableNotification = disableNotification, allowSendingWithoutReply = allowSendingWithoutReply)
                    }
                }
            })
        }
    } catch (e: Throwable) {
        readException(fileName, e)
    }
}

suspend inline fun <R> Plugin.readFile(
    file: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file, it) },
    crossinline block: Reader.() -> R
): R = withContext(Dispatchers.IO) {
    try {
        dataFolder.resolve(file).reader(charset).use { it.block() }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun Plugin.writeFile(
    file: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) },
    crossinline block: Writer.() -> Unit
) = withContext(Dispatchers.IO) {
    try {
        dataFolder.resolve(file).writer(charset).use { it.block() }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun <reified T> Plugin.readFileAs(
    file: String,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = json.readFileAs(dataFolder.resolve(file), exceptionHandler)

suspend inline fun <reified T> Plugin.readFileAs(
    file: String,
    default: T,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = json.readFileAs(dataFolder.resolve(file), default, exceptionHandler)

suspend inline fun <reified T> Plugin.readFileOrCopy(
    file: String,
    defaultPath: String,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = json.readFileOrCopy(dataFolder.resolve(file), defaultPath, javaClass.classLoader, exceptionHandler)

suspend inline fun <reified T> Plugin.writeFile(
    file: String,
    content: T,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) }
): Unit = json.writeFile(dataFolder.resolve(file), content, exceptionHandler)
