package com.hubidubicash.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** A regular income or spending, as Bills needs it. */
data class BillItem(
    val id: String,
    val income: Boolean,
    val kind: String,
    val note: String,
    val amount: Long,
    val dayOfMonth: Int,
    val since: LocalDate,
)

/** One line of the table: an item in one month, paid (received) or not. */
data class BillRow(val item: BillItem, val date: LocalDate, val paid: Boolean, val late: Boolean) {
    /** Income adds, a spending takes away. */
    val signed: Long get() = if (item.income) item.amount else -item.amount
}

/** One month's table, with what is left of the month's life costs and the total of every line. */
data class BillsMonth(val month: YearMonth, val rows: List<BillRow>, val lifeCosts: Long, val total: Long) {
    val open: Int get() = rows.count { !it.paid }
}

/**
 * Bills: a table of one month's regular incomes and spendings, each ticked
 * when paid or received, plus the month's life costs as they run down.
 *
 * It shows the earliest month with something not yet ticked; once every line
 * of a month is ticked, the next month shows, even before this one is over.
 * An item is in the table from the month it was made, whatever its day, so a
 * bill already paid this month can be ticked off too.
 */
object Bills {
    fun rows(items: List<BillItem>, month: YearMonth, paid: (String, YearMonth) -> Boolean, today: LocalDate): List<BillRow> =
        items.filter { YearMonth.from(it.since) <= month }
            .map { item ->
                val date = RegularSchedule.dateIn(month, item.dayOfMonth)
                val ticked = paid(item.id, month)
                BillRow(item, date, ticked, late = !ticked && date.isBefore(today))
            }
            .sortedWith(compareBy<BillRow> { it.date }.thenBy { !it.item.income }.thenBy { it.item.note.lowercase() })

    /** The month Bills shows; with no items, this month. */
    fun shownMonth(items: List<BillItem>, paid: (String, YearMonth) -> Boolean, today: LocalDate): YearMonth {
        val start = items.minOfOrNull { YearMonth.from(it.since) } ?: return YearMonth.from(today)
        var m = start
        repeat(1200) {
            if (rows(items, m, paid, today).any { !it.paid }) return m
            m = m.plusMonths(1)
        }
        return m
    }

    /**
     * What is left of [perMonth] life costs in [month], seen on [today]: all of
     * it before the month begins, none once it is over, and in it the share of
     * the days left, today included, in whole units of the currency.
     */
    fun lifePart(perMonth: Long, month: YearMonth, today: LocalDate): Long {
        val now = YearMonth.from(today)
        return when {
            month.isBefore(now) -> 0
            month.isAfter(now) -> perMonth
            else -> {
                val days = month.lengthOfMonth()
                val left = days - today.dayOfMonth + 1
                BigDecimal.valueOf(perMonth * left)
                    .divide(BigDecimal.valueOf(days.toLong() * Money.SCALE), 0, RoundingMode.HALF_UP)
                    .toLong() * Money.SCALE
            }
        }
    }

    fun month(items: List<BillItem>, paid: (String, YearMonth) -> Boolean, lifeCosts: Long, today: LocalDate): BillsMonth {
        val m = shownMonth(items, paid, today)
        val rows = rows(items, m, paid, today)
        val life = lifePart(lifeCosts, m, today)
        return BillsMonth(m, rows, life, rows.sumOf { it.signed } - life)
    }
}
