package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import com.github.ageofwar.botkit.plugin.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.lang.System.currentTimeMillis

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
        File(directory).mkdirs()
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
    
    private fun newWriter() = PrintWriter(FileWriter(File(directory, fileName()), true))
    private fun fileName(): String {
        var i = 0
        var fileName: String
        do {
            fileName = this.fileName.template(mapOf("date" to currentTimeMillis(), "id" to i))
            i++
        } while (File(fileName).exists())
        return fileName
    }
}

@Serializable
@SerialName("plugins")
class PluginsLogger(@Contextual private val plugins: MutableMap<String, @Contextual Plugin>) : Logger() {
    override suspend fun log(message: String) {
        plugins.forEach { (_, plugin) ->
            plugin.loggers.forEach {
                it.log(message)
            }
        }
    }
}

internal class PluginsSerializer(private val plugins: Plugins) : KSerializer<Plugins> {
    override val descriptor = PrimitiveSerialDescriptor("plugins", PrimitiveKind.BOOLEAN)
    override fun deserialize(decoder: Decoder) = plugins
    override fun serialize(encoder: Encoder, value: Plugins) = Unit
}
