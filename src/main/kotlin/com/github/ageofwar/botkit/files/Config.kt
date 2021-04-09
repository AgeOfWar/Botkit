package com.github.ageofwar.botkit.files

import io.github.ageofwar.javastringtemplate.StringTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.lang.Thread.currentThread
import java.nio.file.Files
import java.nio.file.Paths

suspend inline fun <reified T> Json.readFileAs(path: String, crossinline exceptionHandler: (Throwable) -> T): T {
    return withContext(Dispatchers.IO) {
        try {
            decodeFromString(File(path).readText())
        } catch (e: Throwable) {
            exceptionHandler(e)
        }
    }
}

suspend inline fun <reified T> Json.readFileAs(
    path: String,
    default: T,
    crossinline exceptionHandler: (Throwable) -> T
): T = withContext(Dispatchers.IO) {
    val config = File(path)
    if (!config.exists()) {
        config.parentFile?.mkdirs()
        config.writeText(encodeToString(default))
        default
    } else {
        readFileAs(path, exceptionHandler)
    }
}

suspend inline fun <reified T> Json.readFileOrCopy(
    path: String,
    defaultPath: String,
    crossinline exceptionHandler: (Throwable) -> T
): T = withContext(Dispatchers.IO) {
    val config = File(path)
    if (!config.exists()) {
        config.parentFile?.mkdirs()
        val default = currentThread().contextClassLoader.getResourceAsStream("config/$defaultPath")
        if (default == null) {
            throw FileNotFoundException("cannot find resource on config/$defaultPath")
        }
        Files.copy(default, Paths.get(path))
    }
    readFileAs(path, exceptionHandler)
}

fun String.template(args: Any?): String = StringTemplate.format(this, args)
