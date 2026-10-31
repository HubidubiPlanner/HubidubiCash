package com.hubidubicash.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeriodsTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun `each period ends today and starts after the same day one period back`() {
        val today = d(2026, 10, 16)
        assertEquals(Window(today, today), Windows.current(today, Period.DAY))
        assertEquals(Window(d(2026, 10, 10), today), Windows.current(today, Period.WEEK))
        assertEquals(Window(d(2026, 9, 17), today), Windows.current(today, Period.MONTH))
        assertEquals(Window(d(2025, 10, 17), today), Windows.current(today, Period.YEAR))
    }

    @Test
    fun `the previous window ends where the current one starts`() {
        for (p in Period.entries) for (today in listOf(d(2026, 10, 16), d(2026, 10, 31), d(2028, 2, 29), d(2026, 3, 30))) {
            val now = Windows.current(today, p)
            val before = Windows.previous(today, p)
            assertEquals(now.first.minusDays(1), before.last, "$p $today")
            assertTrue(before.first <= before.last)
        }
    }

    @Test
    fun `a month window ending on a month's last day is the whole calendar month`() {
        assertEquals(Window(d(2026, 10, 1), d(2026, 10, 31)), Windows.current(d(2026, 10, 31), Period.MONTH))
        assertEquals(Window(d(2026, 9, 1), d(2026, 9, 30)), Windows.previous(d(2026, 10, 31), Period.MONTH))
        assertEquals(Window(d(2026, 2, 1), d(2026, 2, 28)), Windows.current(d(2026, 2, 28), Period.MONTH))
        assertEquals(Window(d(2025, 11, 1), d(2026, 10, 31)), Windows.current(d(2026, 10, 31), Period.YEAR))
    }

    @Test
    fun `a monthly cost falls at most once in any month window, and once in nearly all`() {
        var day = d(2026, 1, 1)
        var zero = 0
        while (day.year < 2030) {
            val w = Windows.current(day, Period.MONTH)
            for (dom in 1..31) {
                var n = 0
                var m = YearMonth.from(w.first).minusMonths(1)
                while (m <= YearMonth.from(w.last)) {
                    val due = RegularSchedule.dateIn(m, dom)
                    if (due >= w.first && due <= w.last) n++
                    m = m.plusMonths(1)
                }
                assertTrue(n <= 1, "day $dom falls $n times in the month ending $day")
                if (n == 0) zero++
            }
            day = day.plusDays(1)
        }
        // Where no run of days can hold it: 30 March can't both leave out 28 February for a cost on the
        // 28th and take it in for one on the 29th. A handful in four years.
        assertTrue(zero < 40, "$zero windows hold no month's cost")
    }

    @Test
    fun `spending less, more or about the same as before`() {
        assertEquals(Comparison.Less(12), Comparison.of(1846_20, 2098_00))
        assertEquals(Comparison.More(4), Comparison.of(6482_90, 6233_00))
        assertEquals(Comparison.Same, Comparison.of(10_000, 10_040))
        assertEquals(Comparison.Nothing, Comparison.of(0, 0))
        assertEquals(Comparison.NothingBefore, Comparison.of(500, 0))
        assertEquals(Comparison.Less(100), Comparison.of(0, 500))
    }

    @Test
    fun `shares are whole percent that add up to a hundred`() {
        val amounts = listOf(612_40L, 420_00, 286_50, 214_30, 183_00, 84_00, 46_00)
        assertEquals(listOf(33, 23, 15, 12, 10, 5, 2), shares(amounts))
        assertEquals(100, shares(listOf(1L, 1, 1)).sum())
        assertEquals(listOf(0, 0), shares(listOf(0L, 0)))
        assertEquals(listOf(100, 0), shares(listOf(5L, 0)))
    }
}
