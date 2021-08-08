package com.github.ageofwar.botkit.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.random.Random

suspend fun delayUntil(temporal: Temporal) = delay(Instant.now().until(temporal, ChronoUnit.MILLIS))

suspend fun delayUntil(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(now)) candidateScheduledTime else {
        candidateScheduledTime.plusDays(1)
    }
    delayUntil(scheduledTime)
}

suspend fun delayUntil(dayOfWeek: DayOfWeek, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.with(time)
    val days = when {
        dayOfWeek.ordinal > now.dayOfWeek.ordinal -> dayOfWeek.ordinal - now.dayOfWeek.ordinal
        dayOfWeek.ordinal < now.dayOfWeek.ordinal -> dayOfWeek.ordinal - now.dayOfWeek.ordinal + 7
        dayOfWeek.ordinal == now.dayOfWeek.ordinal -> if (candidateScheduledTime.isAfter(now)) 0 else 7
        else -> throw AssertionError()
    }
    val scheduledTime = candidateScheduledTime.plusDays(days.toLong())
    delayUntil(scheduledTime)
}

suspend fun delayUntil(dayOfMonth: Int, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.withDayOfMonth(dayOfMonth).with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(now)) candidateScheduledTime else {
        candidateScheduledTime.plusMonths(1)
    }
    delayUntil(scheduledTime)
}

suspend fun delayUntil(dayOfYear: MonthDay, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.with(dayOfYear).with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(now)) candidateScheduledTime else {
        candidateScheduledTime.plusYears(1)
    }
    delayUntil(scheduledTime)
}

fun CoroutineScope.scheduleAt(temporal: Temporal, block: suspend CoroutineScope.() -> Unit) = launch {
    delayUntil(temporal)
    block()
}

fun CoroutineScope.scheduleAt(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    delayUntil(time, zoneId)
    block()
}

fun CoroutineScope.scheduleEveryDayAt(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    while (isActive) {
        delayUntil(time, zoneId)
        block()
    }
}

fun CoroutineScope.scheduleAt(dayOfWeek: DayOfWeek, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    delayUntil(dayOfWeek, time, zoneId)
    block()
}

fun CoroutineScope.scheduleEveryWeekAt(dayOfWeek: DayOfWeek, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    while (isActive) {
        delayUntil(dayOfWeek, time, zoneId)
        block()
    }
}

fun CoroutineScope.scheduleAt(dayOfMonth: Int, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    delayUntil(dayOfMonth, time, zoneId)
    block()
}

fun CoroutineScope.scheduleEveryMonthAt(dayOfMonth: Int, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    while (isActive) {
        delayUntil(dayOfMonth, time, zoneId)
        block()
    }
}

fun CoroutineScope.scheduleAt(dayOfYear: MonthDay, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    delayUntil(dayOfYear, time, zoneId)
    block()
}

fun CoroutineScope.scheduleEveryYearAt(dayOfYear: MonthDay, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), block: suspend CoroutineScope.() -> Unit) = launch {
    while (isActive) {
        delayUntil(dayOfYear, time, zoneId)
        block()
    }
}

fun CoroutineScope.scheduleEveryDayBetween(startTime: LocalTime, endTime: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), random: Random = Random, block: suspend CoroutineScope.() -> Unit) = launch {
    delayUntil(endTime)
    while (isActive) {
        delayUntil(random.localTimeBetween(startTime, endTime), zoneId)
        block()
        delayUntil(endTime, zoneId)
    }
}

fun Random.localTimeBetween(start: LocalTime, end: LocalTime): LocalTime {
    val distance = when {
        end.isAfter(start) -> end.toNanoOfDay() - start.toNanoOfDay()
        start.isAfter(end) -> end.toNanoOfDay() - start.toNanoOfDay() + LocalTime.MAX.toNanoOfDay() + 1
        else -> return start
    }
    return start.plusNanos(nextLong(distance))
}
