package com.github.ageofwar.botkit.files

import freemarker.template.Configuration
import freemarker.template.Template
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.StringWriter
import java.lang.Thread.currentThread
import java.nio.file.Files
import java.nio.file.Paths

suspend inline fun <reified T> Json.readFileAs(file: File, crossinline exceptionHandler: (Throwable) -> T): T {
    return withContext(Dispatchers.IO) {
        try {
            decodeFromString(file.readText())
        } catch (e: Throwable) {
            exceptionHandler(e)
        }
    }
}

suspend inline fun <reified T> Json.readFileAs(
    file: File,
    default: T,
    crossinline exceptionHandler: (Throwable) -> T
): T = withContext(Dispatchers.IO) {
    if (!file.exists()) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(encodeToString(default))
            default
        } catch (e: Throwable) {
            exceptionHandler(e)
        }
    } else {
        readFileAs(file, exceptionHandler)
    }
}

suspend inline fun <reified T> Json.readFileOrCopy(
    file: File,
    defaultPath: String,
    classLoader: ClassLoader = currentThread().contextClassLoader,
    crossinline exceptionHandler: (Throwable) -> T
): T = withContext(Dispatchers.IO) {
    if (!file.exists()) {
        try {
            file.parentFile?.mkdirs()
            classLoader.getResourceAsStream("config/$defaultPath")?.use {
                Files.copy(it, Paths.get(file.path))
            } ?: throw FileNotFoundException("cannot find resource on config/$defaultPath")
        } catch (e: Throwable) {
            exceptionHandler(e)
        }
    }
    readFileAs(file, exceptionHandler)
}

suspend inline fun <reified T> Json.writeFile(
    file: File,
    content: T,
    crossinline exceptionHandler: (Throwable) -> Unit
): Unit = withContext(Dispatchers.IO) {
    try {
        file.parentFile?.mkdirs()
        file.writeText(encodeToString(content))
    } catch (e: Throwable) {
        exceptionHandler(e)
    }
}

fun String.template(vararg args: Pair<String, Any?>): String {
    val reader = reader()
    val writer = StringWriter()
    val template = Template("Botkit", reader, Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS))
    template.process(args.toMap(), writer)
    return writer.toString()
}
