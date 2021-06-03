package com.github.ageofwar.botkit.files

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.io.StringWriter
import java.lang.Thread.currentThread
import java.nio.charset.Charset
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.reflect.typeOf

suspend fun Path.suspendReadText() = withContext(Dispatchers.IO) { readText() }
suspend fun Path.suspendWriteText(text: String) = withContext(Dispatchers.IO) { writeText(text) }
suspend fun Path.suspendExists() = withContext(Dispatchers.IO) { exists() }
suspend fun Path.suspendCreateDirectories() = withContext(Dispatchers.IO) { createDirectories() }
suspend fun Path.suspendDeleteExisting() = withContext(Dispatchers.IO) { deleteExisting() }
suspend fun Path.suspendOutputStream() = withContext(Dispatchers.IO) { outputStream() }
suspend fun Path.suspendBufferedReader(charset: Charset, vararg options: OpenOption) = withContext(Dispatchers.IO) { bufferedReader(charset, options = options) }
suspend fun Path.suspendBufferedWriter(charset: Charset, vararg options: OpenOption) = withContext(Dispatchers.IO) { bufferedWriter(charset, options = options) }
suspend fun InputStream.suspendCopyTo(out: OutputStream) = withContext(Dispatchers.IO) { copyTo(out) }
suspend fun ClassLoader.suspendGetResourceAsStream(name: String): InputStream? = withContext(Dispatchers.IO) { getResourceAsStream(name) }

suspend inline fun <reified T> Json.readFileAs(file: Path, crossinline exceptionHandler: (Throwable) -> T): T {
    val text = try {
        file.suspendReadText()
    } catch (e: Throwable) {
        return exceptionHandler(e)
    }
    return try {
        decodeFromString(text)
    } catch (e: Throwable) {
        @OptIn(ExperimentalStdlibApi::class)
        exceptionHandler(SerializationException("An error occurred while deserializing $text to ${typeOf<T>()}", e))
    }
}

suspend inline fun <reified T> Json.readFileAs(
    file: Path,
    default: T,
    crossinline exceptionHandler: (Throwable) -> T
): T {
    return if (!file.suspendExists()) {
        val text = try {
            encodeToString(default)
        } catch (e: Throwable) {
            return exceptionHandler(SerializationException("An error occurred while serializing $default", e))
        }
        try {
            file.parent?.suspendCreateDirectories()
            file.suspendWriteText(text)
            default
        } catch (e: Throwable) {
            exceptionHandler(e)
        }
    } else {
        readFileAs(file, exceptionHandler)
    }
}

suspend inline fun <reified T> Json.readFileOrCopy(
    file: Path,
    defaultPath: String,
    classLoader: ClassLoader = currentThread().contextClassLoader,
    crossinline exceptionHandler: (Throwable) -> T
): T {
    if (!file.suspendExists()) {
        try {
            file.parent?.suspendCreateDirectories()
            classLoader.suspendGetResourceAsStream("config/$defaultPath")?.use {
                it.suspendCopyTo(file.suspendOutputStream())
            } ?: throw FileNotFoundException("cannot find resource on 'config/$defaultPath'")
        } catch (e: Throwable) {
            return exceptionHandler(e)
        }
    }
    return readFileAs(file, exceptionHandler)
}

suspend inline fun <reified T> Json.writeFile(
    file: Path,
    content: T,
    crossinline exceptionHandler: (Throwable) -> Unit
) {
    val text = try {
        encodeToString(content)
    } catch (e: Throwable) {
        return exceptionHandler(SerializationException("An error occurred while serializing $content", e))
    }
    try {
        file.parent?.suspendCreateDirectories()
        file.suspendWriteText(text)
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

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
