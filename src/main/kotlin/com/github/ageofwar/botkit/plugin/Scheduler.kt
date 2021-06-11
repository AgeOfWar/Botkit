package com.github.ageofwar.botkit.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

suspend inline fun delayUntil(temporal: Temporal) = delay(LocalDateTime.now().until(temporal, ChronoUnit.MILLIS))

suspend inline fun delayUntil(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val nowZoned = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = nowZoned.with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(nowZoned)) candidateScheduledTime else {
        candidateScheduledTime.plusDays(1)
    }
    delayUntil(scheduledTime)
}

inline fun CoroutineScope.scheduleAt(temporal: Temporal, crossinline block: () -> Unit) = launch {
    delayUntil(temporal)
    block()
}

inline fun CoroutineScope.scheduleAt(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: () -> Unit) = launch {
    delayUntil(time, zoneId)
    block()
}