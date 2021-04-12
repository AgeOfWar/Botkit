package com.github.ageofwar.botkit

import com.github.ageofwar.botkit.files.template
import com.github.ageofwar.ktelegram.*
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
@SerialName("telegram")
class TelegramLogger(
    private val token: String,
    @SerialName("api_url") private val apiUrl: String,
    private val chat: Long
) : Logger() {
    @Transient lateinit var api: TelegramApi
    
    override suspend fun init() {
        api = TelegramApi(token, apiUrl)
        api.getMe()
    }
    
    override suspend fun log(message: String) {
        api.sendMessage(ChatId.fromId(chat), TextContent(Text(message)), disableNotification = true)
    }
    
    override suspend fun close() = withContext(Dispatchers.IO) {
        api.close()
    }
}

internal class PrefixSerializer(private val prefix: String) : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("prefix", PrimitiveKind.STRING)
    
    override fun deserialize(decoder: Decoder) = "$prefix/${decoder.decodeString()}"
    
    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value.removePrefix(prefix))
    }
}
