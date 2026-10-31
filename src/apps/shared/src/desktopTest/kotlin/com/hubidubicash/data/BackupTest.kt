package com.hubidubicash.data

import com.hubidubicash.core.Kind
import com.hubidubicash.core.Sealed
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupTest {
    private val password = "sunflower-cellar-42".toCharArray()
    private val today = LocalDate.of(2026, 10, 31)
    private var clock = 1_000L
    private val here = inMemoryDatabase()
    private val there = inMemoryDatabase()
    private val repoHere = CashRepository(here) { clock++ }
    private val repoThere = CashRepository(there) { clock++ }

    @AfterTest fun close() { here.close(); there.close() }

    /** A sealed backup of [repo], with few rounds so the test is quick. */
    private suspend fun backup(db: CashDatabase): ByteArray = ByteArrayOutputStream().also {
        val dao = db.dao()
        Backup.write(it, dao.allSpendings(), dao.allRegulars(), dao.marks(), "1.0.0", password, rounds = 100_000)
    }.toByteArray()

    @Test
    fun `everything live comes back, with what was ticked`() = runBlocking {
        repoHere.addSpending(Kind.FOOD, 4250, today, "Lunch")
        val gone = repoHere.addSpending(Kind.FUN, 100, today, "")
        repoHere.deleteSpending(gone)
        val rent = repoHere.addRegular(false, Kind.HOME, 3000_00, "Rent", 1, LocalDate.of(2026, 9, 1))
        repoHere.addRegular(true, Kind.OTHER, 10_000_00, "Salary", 10, LocalDate.of(2026, 9, 1))
        repoHere.catchUp(today)
        repoHere.setPaid(rent, YearMonth.of(2026, 10), true)

        val contents = repoThere.readBackup(ByteArrayInputStream(backup(here)), password)
        val plan = repoThere.plan(contents)
        assertEquals(5, plan.added)
        repoThere.restore(plan, today)
        val dao = there.dao()
        assertEquals(here.dao().allSpendings().filter { !it.deleted }.toSet(), dao.allSpendings().toSet())
        assertEquals(here.dao().regulars().toSet(), dao.regulars().toSet())
        assertEquals(here.dao().marks(), dao.marks())
        // Restoring again changes nothing.
        assertEquals(0, repoThere.plan(contents).added + repoThere.plan(contents).replaced)
    }

    @Test
    fun `a restore takes only what is newer than here, and a later delete here stays`() = runBlocking {
        val a = repoHere.addSpending(Kind.FOOD, 100, today, "old")
        val b = repoHere.addSpending(Kind.FOOD, 200, today, "keep")
        val file = backup(here)
        repoHere.updateSpending(a, Kind.FOOD, 150, today, "newer here")
        repoHere.deleteSpending(b)
        val plan = repoHere.plan(repoHere.readBackup(ByteArrayInputStream(file), password))
        assertEquals(0, plan.added)
        assertEquals(0, plan.replaced)
        assertEquals(2, plan.alreadyHere)
        repoHere.restore(plan, today)
        assertEquals("newer here", repoHere.spending(a)?.note)
        assertEquals(null, repoHere.spending(b))
    }

    @Test
    fun `a restored regular item never adds a month twice`() = runBlocking {
        repoThere.addRegular(false, Kind.HOME, 100, "", 1, LocalDate.of(2026, 8, 1))
        repoThere.catchUp(today)
        repoHere.restore(repoHere.plan(repoHere.readBackup(ByteArrayInputStream(backup(there)), password)), today)
        assertEquals(0, repoHere.catchUp(today))
        assertEquals(3, here.dao().allSpendings().size)
    }

    @Test
    fun `a sealed backup asks for its password, and anything else is not a backup`() {
        val file = runBlocking { backup(here) }
        assertTrue(Backup.isSealed(ByteArrayInputStream(file)))
        assertFailsWith<Backup.PasswordNeeded> { Backup.read(ByteArrayInputStream(file), null) }
        assertFailsWith<Sealed.WrongPassword> { Backup.read(ByteArrayInputStream(file), "not it at all".toCharArray()) }
        assertFalse(Backup.isSealed(ByteArrayInputStream("hello".toByteArray())))
        assertFailsWith<Backup.NotABackup> { Backup.read(ByteArrayInputStream("hello".toByteArray()), null) }
        assertEquals("HubidubiCash 2026-10-31.hubidubicash", Backup.fileName(today))
    }
}
