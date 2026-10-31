package com.hubidubicash.data

import com.hubidubicash.core.INCOME
import com.hubidubicash.core.Ids
import com.hubidubicash.core.Kind
import com.hubidubicash.core.RegularSchedule
import com.hubidubicash.core.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth

/** One page of spendings, newest first, with the whole of each shown day's total. */
data class SpendingPage(
    val rows: List<SpendingRow>,
    val offset: Int,
    val count: Int,
    val dayTotals: Map<Long, Long>,
)

/**
 * The only thing that writes the database. After every change [version]
 * moves on, and whatever shows the data reads it again: the screens, the
 * widget's sheet and an import all share one repository in a process.
 */
class CashRepository(private val db: CashDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.dao()
    private val catchUpLock = Mutex()
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    private fun changed() = _version.update { it + 1 }

    // ---- spendings

    suspend fun addSpending(kind: Kind, amount: Long, day: LocalDate, note: String): String {
        val t = now()
        val id = Ids.new("s")
        dao.upsertSpending(SpendingRow(id, kind.id, amount, day.toEpochDay(), note.trim(), null, t, t))
        changed()
        return id
    }

    suspend fun updateSpending(id: String, kind: Kind, amount: Long, day: LocalDate, note: String) {
        val old = dao.spending(id) ?: return
        dao.upsertSpending(old.copy(kind = kind.id, amount = amount, day = day.toEpochDay(), note = note.trim(),
            updated = now()))
        changed()
    }

    suspend fun deleteSpending(id: String) {
        val old = dao.spending(id) ?: return
        dao.upsertSpending(old.copy(deleted = true, updated = now()))
        changed()
    }

    suspend fun spending(id: String): SpendingRow? = dao.spending(id)?.takeIf { !it.deleted }

    suspend fun page(window: Window, offset: Int, limit: Int = PAGE): SpendingPage {
        val count = dao.count(window.firstDay, window.lastDay)
        val start = offset.coerceIn(0, maxOf(0, count - 1) / limit * limit)
        val rows = dao.page(window.firstDay, window.lastDay, limit, start)
        val totals = if (rows.isEmpty()) emptyMap()
            else dao.dayTotals(rows.minOf { it.day }, rows.maxOf { it.day }).associate { it.day to it.total }
        return SpendingPage(rows, start, count, totals)
    }

    suspend fun kindTotals(window: Window): Map<Kind, Long> =
        dao.kindTotals(window.firstDay, window.lastDay)
            .groupBy { Kind.of(it.kind) }.mapValues { (_, v) -> v.sumOf { it.total } }

    suspend fun total(window: Window): Long = dao.total(window.firstDay, window.lastDay)

    // ---- regular incomes and spendings

    suspend fun regulars(): List<RegularRow> = dao.regulars()

    suspend fun regular(id: String): RegularRow? = dao.regular(id)?.takeIf { !it.deleted }

    /** A new regular item; a spending whose day is today is added at once. */
    suspend fun addRegular(income: Boolean, kind: Kind, amount: Long, note: String, dayOfMonth: Int, today: LocalDate): String {
        val t = now()
        val id = Ids.new("r")
        dao.upsertRegular(RegularRow(id, income, if (income) INCOME else kind.id, amount, note.trim(),
            dayOfMonth.coerceIn(1, 31), today.toEpochDay(), null, t, t))
        changed()
        catchUp(today)
        return id
    }

    /** Changes what is added from the next month not yet added; what was added stays as it was. */
    suspend fun updateRegular(id: String, kind: Kind, amount: Long, note: String, dayOfMonth: Int, today: LocalDate) {
        val old = dao.regular(id) ?: return
        dao.upsertRegular(old.copy(kind = if (old.income) INCOME else kind.id, amount = amount, note = note.trim(),
            dayOfMonth = dayOfMonth.coerceIn(1, 31), updated = now()))
        changed()
        catchUp(today)
    }

    /** Stops it; the spendings it added stay. */
    suspend fun deleteRegular(id: String) {
        val old = dao.regular(id) ?: return
        dao.upsertRegular(old.copy(deleted = true, updated = now()))
        changed()
    }

    // ---- Bills

    suspend fun marks(): List<BillMarkRow> = dao.marks()

    suspend fun setPaid(regularId: String, month: YearMonth, paid: Boolean) {
        dao.upsertMark(BillMarkRow(regularId, month.toString(), paid, now()))
        changed()
    }

    // ---- regular spendings, added on their day

    /**
     * Adds every regular spending due by [today] that is not in yet, each on
     * its own day. Safe to run any number of times, and twice at once: each
     * month's spending has one id, and a second insert of it is ignored.
     * Returns how many were added.
     */
    suspend fun catchUp(today: LocalDate): Int = catchUpLock.withLock {
        var added = 0
        for (r in dao.regulars()) {
            if (r.income) continue
            val due = RegularSchedule.dueMonths(r.dayOfMonth, LocalDate.ofEpochDay(r.since),
                r.lastAdded?.let { runCatching { YearMonth.parse(it) }.getOrNull() }, today)
            for (m in due) {
                val t = now()
                val row = SpendingRow(RegularSchedule.spendingId(r.id, m), r.kind, r.amount,
                    RegularSchedule.dateIn(m, r.dayOfMonth).toEpochDay(), r.note, r.id, t, t)
                if (dao.insertSpendingIfAbsent(row) != -1L) added++
                dao.setLastAdded(r.id, m.toString())
            }
        }
        if (added > 0) changed()
        added
    }

    // ---- backups

    suspend fun export(out: OutputStream, password: CharArray, appVersion: String) {
        Backup.write(out, dao.allSpendings(), dao.allRegulars(), dao.marks(), appVersion, password)
    }

    fun readBackup(input: InputStream, password: CharArray?): Backup.Contents = Backup.read(input, password)

    suspend fun plan(contents: Backup.Contents): Backup.Plan {
        val spendings = dao.allSpendings().associateBy { it.id }
        val regulars = dao.allRegulars().associateBy { it.id }
        val marks = dao.marks().associateBy { it.regularId to it.month }
        return Backup.plan(contents, { spendings[it] }, { regulars[it] }, { r, m -> marks[r to m] })
    }

    /** Writes what [plan] brings in, then adds any regular spending now due. */
    suspend fun restore(plan: Backup.Plan, today: LocalDate) {
        plan.spendings.forEach { dao.upsertSpending(it) }
        plan.regulars.forEach { dao.upsertRegular(it) }
        plan.marks.forEach { dao.upsertMark(it) }
        changed()
        catchUp(today)
    }

    companion object {
        /** Spendings shows at most this many at once. */
        const val PAGE = 50
    }
}
