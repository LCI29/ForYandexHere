package ru.clementl.metrotimex.model.data

import ru.clementl.metrotimex.*
import ru.clementl.metrotimex.converters.toDate
import ru.clementl.metrotimex.converters.toDateTime
import ru.clementl.metrotimex.converters.toLong
import ru.clementl.metrotimex.model.states.TimePoint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import kotlin.math.max
import kotlin.math.min

/**
 * Basic representation of any time interval
 */
open class TimeSpan(open val startMilli: Long?, open val endMilli: Long?) {

    open val duration: Long?
        get() {
            val a = startMilli ?: return null
            val b = endMilli ?: return null
            return b - a
        }
    open val period: Period?
        get() {
            val a = startMilli ?: return null
            val b = endMilli ?: return null
            return Period.between(
                a.toDate(),
                b.toDate()
            )
        }
    open val nullable: Boolean
        get() = startMilli == null || endMilli == null

    operator fun contains(timePoint: TimePoint): Boolean {
        return contains(timePoint.milli)
    }

    operator fun contains(moment: Long): Boolean {
        val a = startMilli ?: return false
        val b = endMilli ?: return false
        return moment in a..b
    }

    open fun fromStartTill(millis: Long): Long? {
        val a = startMilli ?: return null
        return millis - a
    }

    open fun tillEndFrom(millis: Long): Long? {
        val b = endMilli ?: return null
        return b - millis
    }


    open fun intersect(other: TimeSpan): TimeSpan? {
        if (listOf(this, other).any { it.nullable }) return null
        val a = max(this.startMilli!!, other.startMilli!!)
        val b = min(this.endMilli!!, other.endMilli!!)
        return if (a <= b) TimeSpan(a, b) else null
    }

    fun intersectionDuration(other: TimeSpan): Long = this.intersect(other)?.duration ?: 0

    companion object {
        fun eveningOf(date: LocalDate): TimeSpan =
            TimeSpan(
                LocalDateTime.of(date, EVENING_FROM).toLong(),
                LocalDateTime.of(date, EVENING_TILL).toLong()
            )

        fun nightEarlyOf(date: LocalDate): TimeSpan =
            TimeSpan(
                LocalDateTime.of(date.plusDays(1), LocalTime.MIN).toLong(),
                LocalDateTime.of(date.plusDays(1), NIGHT_TILL).toLong()
            )

        fun nightLateOf(date: LocalDate): TimeSpan =
            TimeSpan(
                LocalDateTime.of(date, NIGHT_FROM).toLong(),
                LocalDateTime.of(date, LocalTime.MAX).toLong()
            )
    }

    override fun toString(): String {
        return "[$startMilli, $endMilli]"
    }
}

/**
 * Comparing time in millis with TimeSpan. If the time is before start of TimeSpan, then it less;
 * if the time is after end of TimeSpan, then it greater; and if time is between start and end of
 * TimeSpan, then it equals
 */
operator fun Long.compareTo(timeSpan: TimeSpan): Int {
    timeSpan.startMilli?.let { a ->
        timeSpan.endMilli?.let { b ->
            return when {
                this < a -> -1
                this > b -> 1
                else -> 0
            }
        }
        return if (this < a) -1 else 0
    }
    timeSpan.endMilli?.let { b ->
        return if (this > b) 1 else 0
    }
    return 1
}

fun TimeSpan.isFinished(): Boolean {
    val end = endMilli ?: return false
    return LocalDateTime.now().isAfter(end.toDateTime())
}


fun main() {
    val a = TimeSpan(3, 8)
    val b = TimeSpan(0, 6)
    println(a.intersect(b))
}