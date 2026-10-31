package com.hubidubicash.data

import com.hubidubicash.core.Kind
import com.hubidubicash.core.Period
import com.hubidubicash.core.RegularSchedule
import com.hubidubicash.core.Window
import com.hubidubicash.core.Windows
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CashRepositoryTest {
    private val db = inMemoryDatabase()
    private var clock = 1_000L
    private val repo = CashRepository(db) { clock++ }
    private val today = LocalDate.of(2026, 10, 31)

    @AfterTest fun close() = db.close()

    @Test
    fun `a page holds 50, newest first, and says when there is more`() = runBlocking {
        repeat(120) { i -> repo.addSpending(Kind.FOOD, 100L + i, today.minusDays(i / 4L), "n$i") }
        val window = Window(today.minusDays(40), today)
        val first = repo.page(window, 0)
        assertEquals(50, first.rows.size)
        assertEquals(120, first.count)
        assertEquals(today, LocalDate.ofEpochDay(first.rows.first().day))
        assertTrue(first.rows.zipWithNext().all { (a, b) -> a.day >= b.day })
        val last = repo.page(window, 100)
        assertEquals(20, last.rows.size)
        // Past the end: the last page.
        assertEquals(100, repo.page(window, 500).offset)
    }

    @Test
    fun `a day's total counts the whole day even when the page ends inside it`() = runBlocking {
        repeat(52) { repo.addSpending(Kind.FOOD, 100, today, "") }
        val page = repo.page(Window(today, today), 0)
        assertEquals(50, page.rows.size)
        assertEquals(mapOf(today.toEpochDay() to 5200L), page.dayTotals)
    }

    @Test
    fun `kind totals and the total cover exactly the window`() = runBlocking {
        repo.addSpending(Kind.FOOD, 1000, today, "")
        repo.addSpending(Kind.FOOD, 500, today.minusDays(6), "")
        repo.addSpending(Kind.FUN, 700, today.minusDays(7), "")
        val week = Windows.current(today, Period.WEEK)
        assertEquals(mapOf(Kind.FOOD to 1500L), repo.kindTotals(week))
        assertEquals(1500, repo.total(week))
        assertEquals(700, repo.total(Windows.previous(today, Period.WEEK)))
    }

    @Test
    fun `deleted spendings are in no list or sum`() = runBlocking {
        val id = repo.addSpending(Kind.FOOD, 1000, today, "")
        repo.deleteSpending(id)
        val week = Windows.current(today, Period.WEEK)
        assertEquals(0, repo.total(week))
        assertEquals(0, repo.page(week, 0).count)
        assertNull(repo.spending(id))
    }

    @Test
    fun `a regular spending is added on its day, every month since, and never twice`() = runBlocking {
        val id = repo.addRegular(false, Kind.HOME, 3000_00, "Rent", 1, LocalDate.of(2026, 7, 20))
        assertEquals(0, repo.catchUp(LocalDate.of(2026, 7, 31)))
        assertEquals(3, repo.catchUp(today))
        assertEquals(0, repo.catchUp(today))
        val year = Window(LocalDate.of(2026, 1, 1), today)
        val days = repo.page(year, 0).rows.map { LocalDate.ofEpochDay(it.day) }
        assertEquals(listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)), days)
        assertTrue(repo.page(year, 0).rows.all { it.regularId == id && it.note == "Rent" })
    }

    @Test
    fun `two catch-ups at once add each month once`() = runBlocking {
        repo.addRegular(false, Kind.HOME, 100, "", 1, LocalDate.of(2026, 1, 1))
        val added = (1..4).map { async { repo.catchUp(today) } }.awaitAll()
        // The first catch-up ran when it was made, on 1 January.
        assertEquals(9, added.sum())
        assertEquals(10, repo.page(Window(LocalDate.of(2026, 1, 1), today), 0).count)
    }

    @Test
    fun `a deleted generated spending does not come back`() = runBlocking {
        val id = repo.addRegular(false, Kind.HOME, 100, "", 1, LocalDate.of(2026, 10, 1))
        repo.deleteSpending(RegularSchedule.spendingId(id, YearMonth.of(2026, 10)))
        db.dao().upsertRegular(db.dao().regular(id)!!.copy(lastAdded = null))
        assertEquals(0, repo.catchUp(today))
        assertEquals(0, repo.total(Window(LocalDate.of(2026, 10, 1), today)))
    }

    @Test
    fun `deleting a regular item keeps what it added, and an income is never a spending`() = runBlocking {
        val rent = repo.addRegular(false, Kind.HOME, 100, "", 1, LocalDate.of(2026, 9, 1))
        repo.addRegular(true, Kind.OTHER, 900, "Salary", 10, LocalDate.of(2026, 9, 1))
        repo.catchUp(today)
        repo.deleteRegular(rent)
        assertEquals(0, repo.catchUp(LocalDate.of(2026, 12, 31)))
        assertEquals(200, repo.total(Window(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))))
        assertEquals(listOf("income"), repo.regulars().map { it.kind })
    }

    @Test
    fun `an edit changes the months not yet added`() = runBlocking {
        val id = repo.addRegular(false, Kind.HOME, 100, "", 1, LocalDate.of(2026, 10, 1))
        repo.updateRegular(id, Kind.HOME, 250, "", 1, today)
        repo.catchUp(LocalDate.of(2026, 11, 1))
        assertEquals(100, repo.total(Window(LocalDate.of(2026, 10, 1), today)))
        assertEquals(250, repo.total(Window(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1))))
    }

    @Test
    fun `bill ticks are kept per item and month`() = runBlocking {
        val id = repo.addRegular(false, Kind.HOME, 100, "", 1, LocalDate.of(2026, 10, 1))
        repo.setPaid(id, YearMonth.of(2026, 10), true)
        repo.setPaid(id, YearMonth.of(2026, 11), true)
        repo.setPaid(id, YearMonth.of(2026, 11), false)
        assertEquals(setOf("2026-10" to true, "2026-11" to false), repo.marks().map { it.month to it.paid }.toSet())
    }
}
