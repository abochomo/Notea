package com.notea.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

val ES: Locale = Locale("es", "ES")

enum class Frequency(val label: String) {
    DAILY("Diaria"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual"),
    CUSTOM("Personalizada"),
}

val Routine.freq: Frequency
    get() = try {
        Frequency.valueOf(frequency)
    } catch (e: Exception) {
        Frequency.DAILY
    }

fun Long.millisToDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.millisToDate(): LocalDate = millisToDateTime().toLocalDate()

fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

/** ¿Toca la rutina en esta fecha según su frecuencia y su periodo de vigencia? */
fun Routine.occursOn(date: LocalDate): Boolean {
    val day = date.toEpochDay()
    if (day < startDay) return false
    val end = endDay
    if (end != null && day > end) return false
    return when (freq) {
        Frequency.DAILY -> true
        Frequency.WEEKLY, Frequency.CUSTOM -> (daysMask and dayBit(date.dayOfWeek)) != 0
        Frequency.MONTHLY -> date.dayOfMonth == minOf(dayOfMonth, date.lengthOfMonth())
    }
}

/** Próxima fecha y hora programada estrictamente posterior a [after]. */
fun Routine.nextOccurrence(after: LocalDateTime): LocalDateTime? {
    var d = after.toLocalDate()
    val startDate = LocalDate.ofEpochDay(startDay)
    if (d.isBefore(startDate)) d = startDate
    for (i in 0 until 800) {
        if (occursOn(d)) {
            val dt = d.atTime(timeMinutes / 60, timeMinutes % 60)
            if (dt.isAfter(after)) return dt
        }
        d = d.plusDays(1)
        val end = endDay
        if (end != null && d.toEpochDay() > end) return null
    }
    return null
}

fun dayShortName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT, ES).trimEnd('.').replaceFirstChar { it.uppercase(ES) }

fun dayFullName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, ES)

fun formatMinutes(minutes: Int): String =
    String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

fun Routine.frequencyText(): String {
    val days = DayOfWeek.values().filter { (daysMask and dayBit(it)) != 0 }
    val base = when (freq) {
        Frequency.DAILY -> "Cada día"
        Frequency.WEEKLY -> if (days.isEmpty()) "Cada semana" else "Cada " + dayFullName(days.first())
        Frequency.CUSTOM -> when {
            days.isEmpty() -> "Sin días"
            days.size == 7 -> "Cada día"
            else -> days.joinToString(", ") { dayShortName(it) }
        }
        Frequency.MONTHLY -> if (dayOfMonth >= 31) "El último día de cada mes" else "El día $dayOfMonth de cada mes"
    }
    return base + " · " + formatMinutes(timeMinutes)
}

data class RoutineStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val scheduled: Int,
    val answeredOnSchedule: Int,
    val totalAnswers: Int,
) {
    val completion: Float get() = if (scheduled == 0) 0f else answeredOnSchedule.toFloat() / scheduled
}

/**
 * Rachas y cumplimiento. El día de hoy, si aún está pendiente, no rompe la racha
 * ni cuenta como fallo.
 */
fun Routine.stats(answeredDays: Set<Long>, today: LocalDate): RoutineStats {
    var best = 0
    var run = 0
    var scheduled = 0
    var answered = 0
    var d = LocalDate.ofEpochDay(startDay)
    var guard = 0
    while (!d.isAfter(today) && guard < 3660) {
        if (occursOn(d)) {
            val done = answeredDays.contains(d.toEpochDay())
            if (d == today && !done) {
                // pendiente de hoy: se ignora
            } else {
                scheduled++
                if (done) {
                    run++
                    answered++
                    if (run > best) best = run
                } else {
                    run = 0
                }
            }
        }
        d = d.plusDays(1)
        guard++
    }
    return RoutineStats(run, best, scheduled, answered, answeredDays.size)
}

/** Fechas programadas sin respuesta entre [from] y [to] (ambas incluidas). */
fun Routine.pendingBetween(answeredDays: Set<Long>, from: LocalDate, to: LocalDate): List<LocalDate> {
    val out = ArrayList<LocalDate>()
    var d = from
    while (!d.isAfter(to)) {
        if (occursOn(d) && !answeredDays.contains(d.toEpochDay())) out.add(d)
        d = d.plusDays(1)
    }
    return out
}
