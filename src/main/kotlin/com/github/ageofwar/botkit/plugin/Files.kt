package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.files.readFileAs
import com.github.ageofwar.botkit.files.readFileOrCopy
import com.github.ageofwar.botkit.files.writeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.nio.charset.Charset

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

fun Plugin.file(path: String) = dataFolder.resolve(path)

suspend inline fun <R> Plugin.readFile(
    file: File,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file.name, it) },
    crossinline block: Reader.() -> R
): R = withContext(Dispatchers.IO) {
    try {
        file.reader(charset).use { it.block() }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun <R> Plugin.readFile(
    file: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file, it) },
    crossinline block: Reader.() -> R
): R = readFile(dataFolder.resolve(file), charset, exceptionHandler, block)

suspend inline fun Plugin.writeFile(
    file: File,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file.name, it) },
    crossinline block: Writer.() -> Unit
) = withContext(Dispatchers.IO) {
    try {
        file.writer(charset).use { it.block() }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun Plugin.writeFile(
    file: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) },
    crossinline block: Writer.() -> Unit
) = writeFile(dataFolder.resolve(file), charset, exceptionHandler, block)

suspend inline fun <reified T> Plugin.readFileAs(
    file: File,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = json.readFileAs(file, exceptionHandler)

suspend inline fun <reified T> Plugin.readFileAs(
    file: String,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = readFileAs(dataFolder.resolve(file), exceptionHandler)

suspend inline fun <reified T> Plugin.readFileAs(
    file: File,
    default: T,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = json.readFileAs(file, default, exceptionHandler)

suspend inline fun <reified T> Plugin.readFileAs(
    file: String,
    default: T,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = readFileAs(dataFolder.resolve(file), default, exceptionHandler)

suspend inline fun <reified T> Plugin.readFileOrCopy(
    file: File,
    defaultPath: String,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = json.readFileOrCopy(file, defaultPath, javaClass.classLoader, exceptionHandler)

suspend inline fun <reified T> Plugin.readFileOrCopy(
    file: String,
    defaultPath: String,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = readFileOrCopy(dataFolder.resolve(file), defaultPath, exceptionHandler)

suspend inline fun <reified T> Plugin.writeFile(
    file: File,
    content: T,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file.name, it) }
): Unit = json.writeFile(file, content, exceptionHandler)

suspend inline fun <reified T> Plugin.writeFile(
    file: String,
    content: T,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) }
): Unit = writeFile(dataFolder.resolve(file), content, exceptionHandler)