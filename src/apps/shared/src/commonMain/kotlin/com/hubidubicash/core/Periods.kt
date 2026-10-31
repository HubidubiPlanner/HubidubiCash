package com.hubidubicash.core

import java.time.LocalDate
import java.time.YearMonth

/** How far back Statistics looks: set in Settings. */
enum class Period(val id: String) {
    DAY("day"), WEEK("week"), MONTH("month"), YEAR("year");

    companion object {
        fun of(id: String?): Period = entries.firstOrNull { it.id == id } ?: WEEK
    }
}

/** Days from [first] to [last], both included. */
data class Window(val first: LocalDate, val last: LocalDate) {
    val firstDay: Long get() = first.toEpochDay()
    val lastDay: Long get() = last.toEpochDay()
}

/**
 * The window Statistics shows ends today and starts after the same day one
 * period back: 25–31 October for a week ending on the 31st. On a month's last
 * day a month is that whole calendar month, so 31 October looks at 1–31
 * October, not 2–31. The window it is compared with ends where it starts.
 */
object Windows {
    /** The day before [end]'s window starts. */
    fun startExclusive(end: LocalDate, period: Period): LocalDate {
        val lastOfMonth = end == YearMonth.from(end).atEndOfMonth()
        return when (period) {
            Period.DAY -> end.minusDays(1)
            Period.WEEK -> end.minusDays(7)
            Period.MONTH -> if (lastOfMonth) YearMonth.from(end).minusMonths(1).atEndOfMonth() else end.minusMonths(1)
            Period.YEAR -> if (lastOfMonth) YearMonth.from(end).minusYears(1).atEndOfMonth() else end.minusYears(1)
        }
    }

    fun current(today: LocalDate, period: Period): Window =
        Window(startExclusive(today, period).plusDays(1), today)

    fun previous(today: LocalDate, period: Period): Window {
        val end = startExclusive(today, period)
        return Window(startExclusive(end, period).plusDays(1), end)
    }
}

/** How this window's spending compares with the one before. */
sealed interface Comparison {
    /** Nothing spent in either. */
    data object Nothing : Comparison
    /** Something now, nothing before: no percentage to give. */
    data object NothingBefore : Comparison
    /** Within half a percent. */
    data object Same : Comparison
    data class Less(val percent: Long) : Comparison
    data class More(val percent: Long) : Comparison

    companion object {
        fun of(now: Long, before: Long): Comparison = when {
            before == 0L -> if (now == 0L) Nothing else NothingBefore
            else -> {
                val pct = (kotlin.math.abs(now - before) * 100 + before / 2) / before
                when {
                    pct == 0L -> Same
                    now < before -> Less(pct)
                    else -> More(pct)
                }
            }
        }
    }
}

/**
 * Each kind's share of a total in whole percent that add up to exactly 100:
 * every share rounded down, then the points left over to the largest remainders.
 */
fun shares(amounts: List<Long>): List<Int> {
    val total = amounts.sum()
    if (total <= 0) return amounts.map { 0 }
    val exact = amounts.map { it * 100.0 / total }
    val floors = exact.map { kotlin.math.floor(it).toInt() }.toMutableList()
    var left = 100 - floors.sum()
    exact.indices.sortedByDescending { exact[it] - floors[it] }.forEach { i ->
        if (left > 0 && amounts[i] > 0) { floors[i]++; left-- }
    }
    return floors
}
