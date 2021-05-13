package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.files.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.io.path.*

fun Plugin.serializeException(file: String, cause: SerializationException): Nothing = exception("Cannot serialize '$file'", cause)
fun Plugin.deserializeException(file: String, cause: SerializationException): Nothing = exception("Cannot deserialize '$file' (update or delete it)", cause)
fun Plugin.ioException(file: String, cause: IOException): Nothing = exception("Cannot access '$file'", cause)
fun Plugin.readException(file: String, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> deserializeException(file, cause)
    is IOException -> ioException(file, cause)
    else -> exception(cause.message, cause)
}
fun Plugin.writeException(file: String, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> serializeException(file, cause)
    is IOException -> ioException(file, cause)
    else -> exception(cause.message, cause)
}

fun Plugin.file(path: String): Path = dataFolder.resolve(path)

suspend fun exists(path: Path): Boolean = withContext(Dispatchers.IO) { path.exists() }
suspend fun Plugin.exists(path: String): Boolean = exists(file(path))
suspend fun createFile(path: Path): Path = withContext(Dispatchers.IO) { path.createFile() }
suspend fun Plugin.createFile(path: String): Path = createFile(file(path))
suspend fun createDirectory(path: Path): Path = withContext(Dispatchers.IO) { path.createDirectory() }
suspend fun Plugin.createDirectory(path: String): Path = createDirectory(file(path))
suspend fun createDirectories(path: Path): Path = withContext(Dispatchers.IO) { path.createDirectories() }
suspend fun Plugin.createDirectories(path: String): Path = createDirectories(file(path))
suspend fun listDirectoryEntries(path: Path, glob: String = "*"): List<Path> = withContext(Dispatchers.IO) { path.listDirectoryEntries(glob) }
suspend fun Plugin.listDirectoryEntries(path: String, glob: String = "*"): List<Path> = listDirectoryEntries(file(path), glob)
suspend fun forEachDirectoryEntry(path: Path, glob: String = "*", action: (Path) -> Unit): Unit = withContext(Dispatchers.IO) { path.forEachDirectoryEntry(glob, action) }
suspend fun Plugin.forEachDirectoryEntry(path: String, glob: String = "*", action: (Path) -> Unit): Unit = forEachDirectoryEntry(file(path), glob, action)

suspend inline fun <R> Plugin.readFile(
    file: Path,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file.name, it) },
    vararg options: OpenOption,
    crossinline block: Reader.() -> R
): R {
    return try {
        file.suspendBufferedReader(charset, options = options).use {
            withContext(Dispatchers.IO) {
                it.block()
            }
        }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun <R> Plugin.readFile(
    file: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file, it) },
    vararg options: OpenOption,
    crossinline block: Reader.() -> R
): R = readFile(dataFolder.resolve(file), charset, exceptionHandler, options = options, block)

suspend inline fun <R> Plugin.readFileOrCopy(
    file: Path,
    defaultPath: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file.name, it) },
    vararg options: OpenOption,
    crossinline block: Reader.() -> R
): R {
    return try {
        if (!file.exists()) {
            try {
                file.parent?.suspendCreateDirectories()
                javaClass.classLoader.getResourceAsStream("config/$defaultPath")?.use {
                    it.suspendCopyTo(file.suspendOutputStream())
                } ?: throw FileNotFoundException("cannot find resource on config/$defaultPath")
            } catch (e: Throwable) {
                return exceptionHandler(e)
            }
        }
        readFile(file, charset, exceptionHandler, options = options, block)
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun <R> Plugin.readFileOrCopy(
    file: String,
    defaultPath: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file, it) },
    vararg options: OpenOption,
    crossinline block: Reader.() -> R
): R = readFileOrCopy(dataFolder.resolve(file), defaultPath, charset, exceptionHandler, options = options, block)

suspend inline fun Plugin.writeFile(
    file: Path,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file.name, it) },
    vararg options: OpenOption,
    crossinline block: Writer.() -> Unit
) {
    try {
        file.suspendBufferedWriter(charset, options = options).use {
            withContext(Dispatchers.IO) {
                it.block()
            }
        }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun Plugin.writeFile(
    file: String,
    charset: Charset = Charsets.UTF_8,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) },
    vararg options: OpenOption,
    crossinline block: Writer.() -> Unit
): Unit = writeFile(dataFolder.resolve(file), charset, exceptionHandler, options = options, block)

suspend inline fun <reified T> Plugin.readJsonFileAs(
    file: Path,
    json: Json = this@readJsonFileAs.json,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = json.readFileAs(file, exceptionHandler)

suspend inline fun <reified T> Plugin.readJsonFileAs(
    file: String,
    json: Json = this@readJsonFileAs.json,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = readJsonFileAs(dataFolder.resolve(file), json, exceptionHandler)

suspend inline fun <reified T> Plugin.readJsonFileAs(
    file: Path,
    default: T,
    json: Json = this@readJsonFileAs.json,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = json.readFileAs(file, default, exceptionHandler)

suspend inline fun <reified T> Plugin.readJsonFileAs(
    file: String,
    default: T,
    json: Json = this@readJsonFileAs.json,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = readJsonFileAs(dataFolder.resolve(file), default, json, exceptionHandler)

suspend inline fun <reified T> Plugin.readJsonFileOrCopy(
    file: Path,
    defaultPath: String,
    json: Json = this@readJsonFileOrCopy.json,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = json.readFileOrCopy(file, defaultPath, javaClass.classLoader, exceptionHandler)

suspend inline fun <reified T> Plugin.readJsonFileOrCopy(
    file: String,
    defaultPath: String,
    json: Json = this@readJsonFileOrCopy.json,
    crossinline exceptionHandler: (Throwable) -> T = { readException(file, it) }
): T = readJsonFileOrCopy(dataFolder.resolve(file), defaultPath, json, exceptionHandler)

suspend inline fun <reified T> Plugin.writeJsonFile(
    file: Path,
    content: T,
    json: Json = this@writeJsonFile.json,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file.name, it) }
): Unit = json.writeFile(file, content, exceptionHandler)

suspend inline fun <reified T> Plugin.writeJsonFile(
    file: String,
    content: T,
    json: Json = this@writeJsonFile.json,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file, it) }
): Unit = writeJsonFile(dataFolder.resolve(file), content, json, exceptionHandler)