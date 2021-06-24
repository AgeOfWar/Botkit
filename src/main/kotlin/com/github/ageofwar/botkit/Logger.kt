package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.PrintWriter
import java.lang.System.currentTimeMillis
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@Serializable
sealed class Logger {
    open suspend fun init() {}
    abstract suspend fun log(message: String)
    open suspend fun close() {}
}

@Serializable
@SerialName("console")
object ConsoleLogger : Logger() {
    override suspend fun log(message: String) {
        println(message)
    }
}

@Serializable
@SerialName("file")
class FileLogger(
    @Contextual private val directory: String,
    @SerialName("file_name") private val fileName: String,
    @SerialName("max_file_size") private val maxFileSize: Int
) : Logger() {
    @Transient
    private lateinit var writer: PrintWriter
    @Transient
    private var fileSize = 0
    
    override suspend fun init() = withContext(Dispatchers.IO) {
        Path(directory).createDirectories()
        writer = newWriter()
    }
    
    override suspend fun log(message: String) {
        withContext(Dispatchers.IO) {
            writer.println(message)
            writer.flush()
            fileSize++
            if (fileSize >= maxFileSize) {
                writer.println("...")
                writer.close()
                writer = newWriter()
                fileSize = 0
            }
        }
    }
    
    override suspend fun close() = withContext(Dispatchers.IO) {
        writer.close()
    }
    
    private fun newWriter() = PrintWriter(Path(directory, fileName()).bufferedWriter(), true)
    private fun fileName(): String {
        var i = 0
        var fileName: String
        do {
            fileName = this.fileName.template("date" to currentTimeMillis(), "id" to i)
            i++
        } while (Path(fileName).exists())
        return fileName
    }
}

class PluginsLogger(private val plugins: Plugins) : Logger() {
    override suspend fun log(message: String) {
        plugins.forEach { (_, plugin) ->
            plugin.loggers.forEach {
                @Suppress("UNCHECKED_CAST")
                with(it) { plugin.log(message) }
            }
        }
    }
}
