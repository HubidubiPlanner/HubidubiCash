package com.hubidubicash.ui

import com.hubidubicash.core.BillItem
import com.hubidubicash.core.Bills
import com.hubidubicash.core.BillsMonth
import com.hubidubicash.core.Chart
import com.hubidubicash.core.Comparison
import com.hubidubicash.core.Kind
import com.hubidubicash.core.Period
import com.hubidubicash.core.Settings
import com.hubidubicash.core.SettingsState
import com.hubidubicash.core.Window
import com.hubidubicash.core.Windows
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.Language
import com.hubidubicash.core.shares
import com.hubidubicash.data.Backup
import com.hubidubicash.data.CashDatabase
import com.hubidubicash.data.CashRepository
import com.hubidubicash.data.RegularRow
import com.hubidubicash.data.SpendingPage
import com.hubidubicash.data.SpendingRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency

/** What one process shares: the database's repository, the settings, and the app's version. */
class CashGraph(db: CashDatabase, val settings: Settings, val appVersion: String) {
    val repo = CashRepository(db)

    init { I18n.language = settings.current.language }
}

/** The dates Spendings lists. */
enum class Range { WEEK, MONTH, YEAR, DATES }

data class KindShare(val kind: Kind, val amount: Long, val percent: Int)

data class StatsState(
    val period: Period,
    val window: Window,
    val total: Long,
    val comparison: Comparison,
    /** Every kind in its fixed order, nothing spent included. */
    val kinds: List<KindShare>,
)

data class ListState(val range: Range, val window: Window, val page: SpendingPage)

data class UiState(
    val today: LocalDate,
    val settings: SettingsState,
    val stats: StatsState,
    val list: ListState,
    val incomes: List<RegularRow>,
    val regularSpendings: List<RegularRow>,
    val bills: BillsMonth,
)

/**
 * Everything the five tabs show, as one state, read again whenever anything
 * changes: a spending saved here or by the widget, a tick in Bills, a setting,
 * a new day. Every action writes through the repository and returns once it
 * is written.
 */
class CashModel(
    val graph: CashGraph,
    private val scope: CoroutineScope,
    private val clock: () -> LocalDate = LocalDate::now,
) {
    private val repo = graph.repo
    private val settings = graph.settings
    private val range = MutableStateFlow(Range.WEEK)
    private val dates = MutableStateFlow<Window?>(null)
    private val offset = MutableStateFlow(0)
    private val day = MutableStateFlow(clock())
    private val _state = MutableStateFlow<UiState?>(null)
    val state: StateFlow<UiState?> = _state.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { repo.catchUp(day.value) }
        val filter = combine(range, dates, offset) { r, d, o -> Triple(r, d, o) }
        scope.launch {
            combine(repo.version, settings.state, filter, day) { _, s, f, d -> Triple(s, f, d) }
                .collectLatest { (s, f, d) ->
                    I18n.language = s.language
                    _state.value = withContext(Dispatchers.IO) { load(s, f.first, f.second, f.third, d) }
                }
        }
        // A new day: Statistics' window moves, and regular spendings due today are added.
        scope.launch {
            while (true) {
                delay(60_000)
                checkDay()
            }
        }
    }

    /** When the app comes back to the front: a day may have passed. */
    fun checkDay() {
        val d = clock()
        scope.launch(Dispatchers.IO) { repo.catchUp(d) }
        if (d != day.value) day.value = d
    }

    private suspend fun load(s: SettingsState, r: Range, chosen: Window?, from: Int, today: LocalDate): UiState {
        val window = Windows.current(today, s.period)
        val byKind = repo.kindTotals(window)
        val total = byKind.values.sum()
        val amounts = Kind.entries.map { byKind[it] ?: 0L }
        val pct = shares(amounts)
        val stats = StatsState(s.period, window, total,
            Comparison.of(total, repo.total(Windows.previous(today, s.period))),
            Kind.entries.mapIndexed { i, k -> KindShare(k, amounts[i], pct[i]) })

        val listWindow = when (r) {
            Range.WEEK -> Windows.current(today, Period.WEEK)
            Range.MONTH -> Windows.current(today, Period.MONTH)
            Range.YEAR -> Windows.current(today, Period.YEAR)
            Range.DATES -> chosen ?: Windows.current(today, Period.WEEK)
        }
        val list = ListState(r, listWindow, repo.page(listWindow, from))

        val regulars = repo.regulars()
        val marks = repo.marks().associate { (it.regularId to it.month) to it.paid }
        val items = regulars.map {
            BillItem(it.id, it.income, it.kind, it.note, it.amount, it.dayOfMonth, LocalDate.ofEpochDay(it.since))
        }
        val bills = Bills.month(items, { id, m -> marks[id to m.toString()] == true }, s.lifeCosts, today)
        return UiState(today, s, stats, list, regulars.filter { it.income }, regulars.filter { !it.income }, bills)
    }

    private fun write(block: suspend CashRepository.() -> Unit) {
        scope.launch(Dispatchers.IO) { repo.block() }
    }

    // ---- spendings

    fun addSpending(kind: Kind, amount: Long, day: LocalDate, note: String) =
        write { addSpending(kind, amount, day, note) }

    fun updateSpending(id: String, kind: Kind, amount: Long, day: LocalDate, note: String) =
        write { updateSpending(id, kind, amount, day, note) }

    fun deleteSpending(id: String) = write { deleteSpending(id) }

    fun showRange(r: Range, chosen: Window? = null) {
        if (chosen != null) dates.value = chosen
        range.value = r
        offset.value = 0
    }

    fun older() { offset.value += CashRepository.PAGE }
    fun newer() { offset.value = (offset.value - CashRepository.PAGE).coerceAtLeast(0) }

    // ---- regular items

    fun addRegular(income: Boolean, kind: Kind, amount: Long, note: String, dayOfMonth: Int) =
        write { addRegular(income, kind, amount, note, dayOfMonth, clock()) }

    fun updateRegular(id: String, kind: Kind, amount: Long, note: String, dayOfMonth: Int) =
        write { updateRegular(id, kind, amount, note, dayOfMonth, clock()) }

    fun deleteRegular(id: String) = write { deleteRegular(id) }

    // ---- Bills

    fun setPaid(regularId: String, month: YearMonth, paid: Boolean) = write { setPaid(regularId, month, paid) }

    // ---- settings

    fun setPeriod(p: Period) = settings.setPeriod(p)
    fun setChart(c: Chart) = settings.setChart(c)
    fun setLifeCosts(amount: Long) = settings.setLifeCosts(amount)
    fun setCurrency(c: Currency) = settings.setCurrency(c)
    fun setLanguage(l: Language) { I18n.language = l; settings.setLanguage(l) }
    fun setTheme(t: ColorTheme) = settings.setTheme(t.id)

    // ---- backups

    suspend fun export(out: OutputStream, password: CharArray): Pair<Int, Int> = withContext(Dispatchers.IO) {
        out.use { repo.export(it, password, graph.appVersion) }
        val s = repo.page(Window(LocalDate.MIN, LocalDate.MAX), 0, 1).count
        s to repo.regulars().size
    }

    suspend fun readBackup(bytes: ByteArray, password: CharArray?): Backup.Contents =
        withContext(Dispatchers.IO) { repo.readBackup(bytes.inputStream(), password) }

    suspend fun plan(contents: Backup.Contents): Backup.Plan = withContext(Dispatchers.IO) { repo.plan(contents) }

    suspend fun restore(plan: Backup.Plan) = withContext(Dispatchers.IO) { repo.restore(plan, clock()) }

    companion object {
        /** All of a picked file, for a backup: small, and read once to see whether it is sealed. */
        suspend fun bytes(input: InputStream): ByteArray = withContext(Dispatchers.IO) { input.use { it.readBytes() } }

        fun isSealed(bytes: ByteArray): Boolean = Backup.isSealed(bytes.inputStream())
    }
}

/** The kind of a stored row. */
val SpendingRow.kindOf: Kind get() = Kind.of(kind)
val RegularRow.kindOf: Kind get() = Kind.of(kind)
