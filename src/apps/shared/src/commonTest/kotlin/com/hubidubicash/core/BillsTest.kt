package com.hubidubicash.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BillsTest {
    private val since = LocalDate.of(2026, 9, 1)
    private fun item(id: String, day: Int, amount: Long, income: Boolean = false, since: LocalDate = this.since) =
        BillItem(id, income, if (income) INCOME else Kind.HOME.id, id, amount * 100, day, since)

    private val october = YearMonth.of(2026, 10)
    private val items = listOf(
        item("rent", 1, 3000), item("salary", 10, 10000, income = true), item("internet", 15, 100), item("phone", 20, 60),
    )

    /** Exactly these lines ticked. */
    private fun ticked(vararg paid: Pair<String, YearMonth>): (String, YearMonth) -> Boolean = { id, m -> (id to m) in paid }

    @Test
    fun `the October example totals 4259`() {
        val today = LocalDate.of(2026, 10, 16)
        val madeInOctober = items.map { it.copy(since = LocalDate.of(2026, 10, 1)) }
        val bills = Bills.month(madeInOctober, ticked("rent" to october, "salary" to october), 5000_00, today)
        assertEquals(october, bills.month)
        assertEquals(2581_00, bills.lifeCosts)
        assertEquals(4259_00, bills.total)
        assertEquals(listOf("rent", "salary", "internet", "phone"), bills.rows.map { it.item.id })
        assertEquals(2, bills.open)
    }

    @Test
    fun `life costs are what is left of this month, all of a later one and none of an earlier one`() {
        val today = LocalDate.of(2026, 10, 31)
        assertEquals(161_00, Bills.lifePart(5000_00, october, today))
        assertEquals(5000_00, Bills.lifePart(5000_00, october, LocalDate.of(2026, 10, 1)))
        assertEquals(5000_00, Bills.lifePart(5000_00, YearMonth.of(2026, 11), today))
        assertEquals(0, Bills.lifePart(5000_00, YearMonth.of(2026, 9), today))
        assertEquals(0, Bills.lifePart(0, october, today))
    }

    @Test
    fun `the shown month is the earliest with something unticked`() {
        val today = LocalDate.of(2026, 10, 16)
        val september = YearMonth.of(2026, 9)
        // September not all ticked: it stays, even in October, and its unticked lines are late.
        val bills = Bills.month(items, ticked("rent" to september, "salary" to september), 5000_00, today)
        assertEquals(september, bills.month)
        assertEquals(0, bills.lifeCosts)
        assertTrue(bills.rows.first { it.item.id == "phone" }.late)
        assertFalse(bills.rows.first { it.item.id == "rent" }.late)
    }

    @Test
    fun `a month all ticked gives way to the next, even mid-month`() {
        val today = LocalDate.of(2026, 10, 16)
        val all = items.flatMap { listOf(it.id to YearMonth.of(2026, 9), it.id to october) }.toTypedArray()
        val bills = Bills.month(items, ticked(*all), 5000_00, today)
        assertEquals(YearMonth.of(2026, 11), bills.month)
        assertEquals(5000_00, bills.lifeCosts)
        assertEquals((10000 - 3000 - 100 - 60 - 5000) * 100L, bills.total)
        assertTrue(bills.rows.none { it.late })
    }

    @Test
    fun `an unticked line whose day has passed is late, this month too`() {
        val today = LocalDate.of(2026, 10, 16)
        val sept = items.map { it.id to YearMonth.of(2026, 9) }.toTypedArray()
        val rows = Bills.month(items, ticked(*sept), 5000_00, today).rows.associateBy { it.item.id }
        assertTrue(rows.getValue("rent").late)
        assertTrue(rows.getValue("internet").late)
        assertFalse(rows.getValue("phone").late)
    }

    @Test
    fun `an item is in the table from the month it was made, whatever its day`() {
        val made = item("gym", 5, 129, since = LocalDate.of(2026, 10, 20))
        val rows = Bills.rows(listOf(made), october, ticked(), LocalDate.of(2026, 10, 20))
        assertEquals(listOf(LocalDate.of(2026, 10, 5)), rows.map { it.date })
        assertEquals(emptyList(), Bills.rows(listOf(made), YearMonth.of(2026, 9), ticked(), LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun `with nothing regular, Bills is this month with its life costs`() {
        val bills = Bills.month(emptyList(), ticked(), 5000_00, LocalDate.of(2026, 10, 16))
        assertEquals(october, bills.month)
        assertEquals(-2581_00, bills.total)
    }
}
