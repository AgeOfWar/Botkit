package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.botkit.files.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.reflect.typeOf

fun serializeException(file: String, cause: SerializationException): Nothing = exception("Cannot serialize file '$file'", cause)
fun deserializeException(file: String, cause: SerializationException): Nothing = exception("Cannot deserialize file '$file' (update or delete it)", cause)
fun ioException(file: String, cause: IOException): Nothing = exception("Cannot access file '$file'", cause)
fun readException(file: String, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> deserializeException(file, cause)
    is IOException -> ioException(file, cause)
    else -> exception(cause.message, cause)
}
fun writeException(file: String, cause: Throwable): Nothing = when (cause) {
    is SerializationException -> serializeException(file, cause)
    is IOException -> ioException(file, cause)
    else -> exception(cause.message, cause)
}

fun Plugin.file(path: String): Path = dataFolder.resolve(path)

suspend inline fun <R> readFile(
    file: Path,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file.name, it) },
    vararg options: OpenOption,
    crossinline block: InputStream.() -> R
): R {
    return try {
        file.suspendInputStream(options = options).use {
            withContext(Dispatchers.IO) {
                it.block()
            }
        }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun <R> readFileOrCopy(
    file: Path,
    defaultPath: String,
    crossinline exceptionHandler: (Throwable) -> R = { readException(file.name, it) },
    vararg options: OpenOption,
    crossinline block: InputStream.() -> R
): R {
    return try {
        if (!file.exists()) {
            try {
                file.parent?.suspendCreateDirectories()
                object {}.javaClass.classLoader.getResourceAsStream("config/$defaultPath")?.use {
                    it.suspendCopyTo(file.suspendOutputStream())
                } ?: throw FileNotFoundException("cannot find resource on config/$defaultPath")
            } catch (e: Throwable) {
                return exceptionHandler(e)
            }
        }
        readFile(file, exceptionHandler, options = options, block)
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun writeFile(
    file: Path,
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file.name, it) },
    vararg options: OpenOption,
    crossinline block: OutputStream.() -> Unit
) {
    try {
        file.suspendOutputStream(options = options).use {
            withContext(Dispatchers.IO) {
                it.block()
            }
        }
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

suspend inline fun <reified T> Json.readJsonFileAs(
    file: Path,
    deserializer: DeserializationStrategy<T> = serializersModule.serializer(),
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = readFileAs(file, deserializer, exceptionHandler)

suspend inline fun <reified T> Json.readJsonFileAs(
    file: Path,
    default: T,
    serializer: KSerializer<T> = serializersModule.serializer(),
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = readFileAs(file, default, serializer, exceptionHandler)

suspend inline fun <reified T> Json.readJsonFileOrCopy(
    file: Path,
    defaultPath: String,
    deserializer: DeserializationStrategy<T> = serializersModule.serializer(),
    crossinline exceptionHandler: (Throwable) -> T = { readException(file.name, it) }
): T = readFileOrCopy(file, defaultPath, deserializer, object {}.javaClass.classLoader, exceptionHandler)

suspend inline fun <reified T> Json.writeJsonFile(
    file: Path,
    content: T,
    serializer: SerializationStrategy<T> = serializersModule.serializer(),
    crossinline exceptionHandler: (Throwable) -> Unit = { writeException(file.name, it) }
): Unit = writeFile(file, content, serializer, exceptionHandler)

@OptIn(ExperimentalStdlibApi::class)
suspend inline fun <T> readDirectoryAsList(
    directory: Path,
    crossinline predicate: (Path) -> Boolean,
    crossinline exceptionHandler: (Path, Throwable) -> T = { file, t -> readException(file.name, t) },
    vararg options: OpenOption,
    crossinline block: InputStream.() -> T
): List<T> = buildList {
    directory.suspendListDirectoryEntries().forEach { file ->
        if (file.isRegularFile() && predicate(file)) {
            add(readFile(file, { t -> exceptionHandler(file, t) }, options = options, block))
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend inline fun <K : Any, V> readDirectoryAsMap(
    directory: Path,
    crossinline transform: (Path) -> K?,
    crossinline exceptionHandler: (Path, Throwable) -> V = { file, t -> readException(file.name, t) },
    vararg options: OpenOption,
    crossinline block: InputStream.() -> V
): Map<K, V> = buildMap {
    directory.suspendListDirectoryEntries().forEach { file ->
        if (file.isRegularFile()) {
            val key = transform(file)
            if (key != null) {
                val value = readFile(file, { t -> exceptionHandler(file, t) }, options = options, block)
                put(key, value)
            }
        }
    }
}

suspend inline fun <reified T> Json.readJsonDirectoryAsList(
    directory: Path,
    deserializer: DeserializationStrategy<T> = serializersModule.serializer(),
    crossinline exceptionHandler: (Path, Throwable) -> T = { file, t -> readException(file.name, t) },
    crossinline predicate: (Path) -> Boolean,
): List<T> = readDirectoryAsList(directory, predicate, exceptionHandler) {
    val text = reader().readText()
    try {
        decodeFromString(deserializer, text)
    } catch (e: Throwable) {
        @OptIn(ExperimentalStdlibApi::class)
        throw SerializationException("An error occurred while deserializing $text to ${typeOf<T>()}", e)
    }
}

suspend inline fun <K : Any, reified V> Json.readJsonDirectoryAsMap(
    directory: Path,
    deserializer: DeserializationStrategy<V> = serializersModule.serializer(),
    crossinline exceptionHandler: (Path, Throwable) -> V = { file, t -> readException(file.name, t) },
    crossinline transform: (Path) -> K?,
): Map<K, V> = readDirectoryAsMap(directory, transform, exceptionHandler) {
    val text = reader().readText()
    try {
        decodeFromString(deserializer, text)
    } catch (e: Throwable) {
        @OptIn(ExperimentalStdlibApi::class)
        throw SerializationException("An error occurred while deserializing $text to ${typeOf<V>()}", e)
    }
}

suspend inline fun <reified V> Json.readJsonDirectoryAsMap(
    directory: Path,
    defaultFileName: String,
    deserializer: DeserializationStrategy<V> = serializersModule.serializer(),
    crossinline exceptionHandler: (Path, Throwable) -> V = { file, t -> readException(file.name, t) }
): Map<String, V> = readJsonDirectoryAsMap(directory, deserializer, exceptionHandler) {
    val fileName = it.nameWithoutExtension
    val extension = it.extension
    when {
        fileName == defaultFileName && extension == "json" -> ""
        fileName.startsWith("$defaultFileName-") && extension == "json" -> fileName.substring(defaultFileName.length + 1)
        else -> null
    }
}
