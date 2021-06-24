package com.github.ageofwar.botkit.plugin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.random.Random

suspend inline fun delayUntil(temporal: Temporal) = delay(LocalDateTime.now().until(temporal, ChronoUnit.MILLIS))

suspend inline fun delayUntil(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(now)) candidateScheduledTime else {
        candidateScheduledTime.plusDays(1)
    }
    delayUntil(scheduledTime)
}

suspend inline fun delayUntil(dayOfWeek: DayOfWeek, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
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

suspend inline fun delayUntil(dayOfMonth: Int, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.withDayOfMonth(dayOfMonth).with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(now)) candidateScheduledTime else {
        candidateScheduledTime.plusMonths(1)
    }
    delayUntil(scheduledTime)
}

suspend inline fun delayUntil(dayOfYear: MonthDay, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault()) {
    val now = ZonedDateTime.now(zoneId)
    val candidateScheduledTime = now.with(dayOfYear).with(time)
    val scheduledTime = if (candidateScheduledTime.isAfter(now)) candidateScheduledTime else {
        candidateScheduledTime.plusYears(1)
    }
    delayUntil(scheduledTime)
}

inline fun CoroutineScope.scheduleAt(temporal: Temporal, crossinline block: suspend () -> Unit) = launch {
    delayUntil(temporal)
    block()
}

inline fun CoroutineScope.scheduleAt(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    delayUntil(time, zoneId)
    block()
}

inline fun CoroutineScope.scheduleEveryDayAt(time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    while (isActive) {
        delayUntil(time, zoneId)
        block()
    }
}

inline fun CoroutineScope.scheduleAt(dayOfWeek: DayOfWeek, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    delayUntil(dayOfWeek, time, zoneId)
    block()
}

inline fun CoroutineScope.scheduleEveryWeekAt(dayOfWeek: DayOfWeek, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    while (isActive) {
        delayUntil(dayOfWeek, time, zoneId)
        block()
    }
}

inline fun CoroutineScope.scheduleAt(dayOfMonth: Int, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    delayUntil(dayOfMonth, time, zoneId)
    block()
}

inline fun CoroutineScope.scheduleEveryMonthAt(dayOfMonth: Int, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    while (isActive) {
        delayUntil(dayOfMonth, time, zoneId)
        block()
    }
}

inline fun CoroutineScope.scheduleAt(dayOfYear: MonthDay, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    delayUntil(dayOfYear, time, zoneId)
    block()
}

inline fun CoroutineScope.scheduleEveryYearAt(dayOfYear: MonthDay, time: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), crossinline block: suspend () -> Unit) = launch {
    while (isActive) {
        delayUntil(dayOfYear, time, zoneId)
        block()
    }
}

inline fun CoroutineScope.scheduleEveryDayBetween(startTime: LocalTime, endTime: LocalTime, zoneId: ZoneId = ZoneId.systemDefault(), random: Random = Random, crossinline block: suspend () -> Unit) = launch {
    while (isActive) {
        delayUntil(random.localTimeBetween(startTime, endTime), zoneId)
        block()
    }
}

fun Random.localTimeBetween(start: LocalTime, end: LocalTime): LocalTime {
    val distance = when {
        end.isAfter(start) -> end.toNanoOfDay() - start.toNanoOfDay()
        start.isAfter(end) -> end.toNanoOfDay() - start.toNanoOfDay() + LocalTime.MAX.toNanoOfDay() + 1
        else -> 0
    }
    return start.plusNanos(nextLong(distance))
}
