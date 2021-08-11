package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.ktelegram.*
import kotlinx.coroutines.delay

suspend fun TelegramApi.broadcastMessage(
    users: Sequence<Long>,
    content: MessageContent<*>,
    replyMarkup: ReplyMarkup? = null,
    disableNotification: Boolean = false,
    chunkSize: Int = 20,
    delay: Long = 10000L) = users.chunked(chunkSize).forEach { chunk ->
    chunk.forEach {
        sendMessage(ChatId.fromId(it), content, null, replyMarkup, disableNotification)
    }
    delay(delay)
}

suspend fun TelegramApi.broadcastMessage(
    users: Iterable<Long>,
    content: MessageContent<*>,
    replyMarkup: ReplyMarkup? = null,
    disableNotification: Boolean = false,
    chunkSize: Int = 20,
    delay: Long = 10000L) = users.chunked(chunkSize).forEach { chunk ->
    chunk.forEach {
        sendMessage(ChatId.fromId(it), content, null, replyMarkup, disableNotification)
    }
    delay(delay)
}
