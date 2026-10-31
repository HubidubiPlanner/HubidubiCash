package com.hubidubicash.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RegularScheduleTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private fun ym(y: Int, m: Int) = YearMonth.of(y, m)

    @Test
    fun `a day past the month's end falls on its last day`() {
        assertEquals(d(2026, 2, 28), RegularSchedule.dateIn(ym(2026, 2), 31))
        assertEquals(d(2028, 2, 29), RegularSchedule.dateIn(ym(2028, 2), 30))
        assertEquals(d(2026, 4, 30), RegularSchedule.dateIn(ym(2026, 4), 31))
        assertEquals(d(2026, 10, 1), RegularSchedule.dateIn(ym(2026, 10), 1))
    }

    @Test
    fun `the first month is the one made in when its day is still to come, else the next`() {
        assertEquals(ym(2026, 10), RegularSchedule.firstMonth(d(2026, 10, 16), 20))
        assertEquals(ym(2026, 11), RegularSchedule.firstMonth(d(2026, 10, 16), 1))
        // Made on the day itself: due that day.
        assertEquals(ym(2026, 10), RegularSchedule.firstMonth(d(2026, 10, 16), 16))
        assertEquals(listOf(ym(2026, 10)), RegularSchedule.dueMonths(16, d(2026, 10, 16), null, d(2026, 10, 16)))
    }

    @Test
    fun `every missed month is caught up, each on its own day`() {
        val due = RegularSchedule.dueMonths(31, d(2026, 1, 5), null, d(2026, 4, 30))
        assertEquals(listOf(ym(2026, 1), ym(2026, 2), ym(2026, 3), ym(2026, 4)), due)
        assertEquals(listOf(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30)),
            due.map { RegularSchedule.dateIn(it, 31) })
    }

    @Test
    fun `nothing is due again once its month is added, nor before its day`() {
        assertEquals(emptyList(), RegularSchedule.dueMonths(1, d(2026, 9, 1), ym(2026, 10), d(2026, 10, 31)))
        assertEquals(emptyList(), RegularSchedule.dueMonths(15, d(2026, 10, 1), null, d(2026, 10, 14)))
        assertEquals(listOf(ym(2026, 11)), RegularSchedule.dueMonths(1, d(2026, 9, 1), ym(2026, 10), d(2026, 11, 1)))
    }

    @Test
    fun `a new day of month applies from the first month not yet added`() {
        // Added for October on the 1st; moved to the 15th: November's comes on the 15th.
        assertEquals(emptyList(), RegularSchedule.dueMonths(15, d(2026, 9, 1), ym(2026, 10), d(2026, 11, 14)))
        assertEquals(listOf(ym(2026, 11)), RegularSchedule.dueMonths(15, d(2026, 9, 1), ym(2026, 10), d(2026, 11, 15)))
    }

    @Test
    fun `the spending id is the same for one item and month, and differs otherwise`() {
        val a = RegularSchedule.spendingId("r_abc", ym(2026, 10))
        assertEquals(a, RegularSchedule.spendingId("r_abc", ym(2026, 10)))
        assertNotEquals(a, RegularSchedule.spendingId("r_abc", ym(2026, 11)))
        assertNotEquals(a, RegularSchedule.spendingId("r_abd", ym(2026, 10)))
        assertTrue(a.matches(Regex("g_[a-z2-7]{16}")), a)
        assertTrue(Ids.new("s").matches(Regex("s_[a-z2-7]{16}")))
    }
}
