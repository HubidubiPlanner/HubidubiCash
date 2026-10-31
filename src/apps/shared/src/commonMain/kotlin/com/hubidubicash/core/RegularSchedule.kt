package com.hubidubicash.core

import java.time.LocalDate
import java.time.YearMonth

/**
 * When a regular cost is added to the spendings: once a month on its day,
 * from the first time that day comes on or after the day it was made, every
 * month after, missed months included. Each month's spending has an id made
 * from the item and the month, so adding it twice is impossible.
 */
object RegularSchedule {
    /** Its day in [month]; a day past the month's end falls on the month's last day. */
    fun dateIn(month: YearMonth, dayOfMonth: Int): LocalDate =
        month.atDay(dayOfMonth.coerceIn(1, 31).coerceAtMost(month.lengthOfMonth()))

    /** The first month whose day is on or after [since]. */
    fun firstMonth(since: LocalDate, dayOfMonth: Int): YearMonth {
        val m = YearMonth.from(since)
        return if (!dateIn(m, dayOfMonth).isBefore(since)) m else m.plusMonths(1)
    }

    /** The months due by [today] and not yet added, oldest first; at most 20 years of them. */
    fun dueMonths(dayOfMonth: Int, since: LocalDate, lastAdded: YearMonth?, today: LocalDate): List<YearMonth> {
        var m = firstMonth(since, dayOfMonth)
        val next = lastAdded?.plusMonths(1)
        if (next != null && next.isAfter(m)) m = next
        val out = mutableListOf<YearMonth>()
        while (!dateIn(m, dayOfMonth).isAfter(today) && out.size < 240) {
            out += m
            m = m.plusMonths(1)
        }
        return out
    }

    /** The id of [regularId]'s spending in [month]. */
    fun spendingId(regularId: String, month: YearMonth): String = Ids.derived("g", "$regularId|$month")
}
