package com.hubidubicash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hubidubicash.core.BillRow
import com.hubidubicash.core.Kind
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t
import com.hubidubicash.data.RegularRow
import com.hubidubicash.data.SpendingRow
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

/** The five tabs, in their order: what was spent comes first, the settings last. */
enum class Tab(val label: String, val icon: ImageVector) {
    STATISTICS("Statistics", Icons.Outlined.PieChart),
    SPENDINGS("Spendings", Icons.AutoMirrored.Outlined.ReceiptLong),
    REGULAR("Regular", Icons.Outlined.EventRepeat),
    BILLS("Bills", Icons.Outlined.TableView),
    SETTINGS("Settings", Icons.Outlined.Tune),
}

/** What is open over the tabs. */
sealed interface Open {
    data class Kinds(val showIncome: Boolean, val monthly: Boolean) : Open
    data class Amount(val target: AmountFor) : Open
    data class SpendingActions(val row: SpendingRow) : Open
    data class RegularActions(val row: RegularRow) : Open
    data class ConfirmSpending(val row: SpendingRow) : Open
    data class ConfirmRegular(val row: RegularRow) : Open
}

/** A month just finished in Bills, undoable for a few seconds: the last tick that finished it. */
private data class Finished(val regularId: String, val month: YearMonth)

/** The whole app, on the phone and the computer alike; [systemDark] stands in for the system's mode in screenshots. */
@Composable
fun HubidubiCashApp(model: CashModel, systemDark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()) {
    val state by model.state.collectAsState()
    HubidubiCashTheme(ColorTheme.of(state?.settings?.theme), systemDark) {
        val c = HubidubiCashTheme.colors
        Surface(color = c.paper, modifier = Modifier.fillMaxSize()) {
            val s = state ?: return@Surface
            // A new language redraws everything and keeps the place.
            key(s.settings.language) { Tabs(s, model) }
        }
    }
}

@Composable
private fun Tabs(s: UiState, model: CashModel) {
    val c = HubidubiCashTheme.colors
    var tab by rememberSaveable { mutableStateOf(Tab.STATISTICS) }
    var open by remember { mutableStateOf<Open?>(null) }
    var finished by remember { mutableStateOf<Finished?>(null) }
    BackHandlerCompat(tab != Tab.STATISTICS) { tab = Tab.STATISTICS }
    LaunchedEffect(finished) { if (finished != null) { delay(6_000); finished = null } }

    val page: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                Tab.STATISTICS -> StatisticsScreen(s) { open = Open.Kinds(showIncome = false, monthly = false) }
                Tab.SPENDINGS -> SpendingsScreen(s,
                    onPlus = { open = Open.Kinds(showIncome = false, monthly = false) },
                    onRange = { r, w -> model.showRange(r, w) },
                    onOpen = { open = Open.Amount(edit(it)) },
                    onHold = { open = Open.SpendingActions(it) },
                    onOlder = model::older, onNewer = model::newer)
                Tab.REGULAR -> RegularScreen(s,
                    onPlus = { open = Open.Kinds(showIncome = true, monthly = true) },
                    onOpen = { open = Open.Amount(edit(it)) },
                    onHold = { open = Open.RegularActions(it) })
                Tab.BILLS -> BillsScreen(s,
                    onPlus = { open = Open.Kinds(showIncome = true, monthly = true) },
                    onTick = { row -> tick(s, row, model) { finished = it } },
                    onOpen = { row -> s.regular(row.item.id)?.let { open = Open.Amount(edit(it)) } })
                Tab.SETTINGS -> SettingsScreen(s, model)
            }
            finished?.let { f ->
                UndoBar(t("%s is all done.", I18n.monthYear(f.month)), Modifier.align(Alignment.BottomCenter)) {
                    model.setPaid(f.regularId, f.month, false)
                    finished = null
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Scaffold(
                containerColor = c.paper,
                bottomBar = {
                    Column {
                        HorizontalDivider(color = c.line, thickness = 1.dp)
                        BottomTabs(tab) { tab = it }
                    }
                },
            ) { pad -> Box(Modifier.padding(pad).consumeWindowInsets(pad)) { page() } }
        } else {
            // On a computer the tabs sit on the left, the page gets the width.
            Row(Modifier.fillMaxSize()) {
                NavigationRail(containerColor = c.nav, modifier = Modifier.width(92.dp)) {
                    Tab.entries.forEach { d ->
                        val selected = tab == d
                        NavigationRailItem(
                            selected = selected, onClick = { tab = d },
                            icon = { Icon(d.icon, contentDescription = null) },
                            label = {
                                Text(t(d.label), style = MaterialTheme.typography.labelMedium, maxLines = 1,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = c.accent, selectedTextColor = c.accent,
                                unselectedIconColor = c.muted, unselectedTextColor = c.muted,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
                VerticalDivider(color = c.line, modifier = Modifier.fillMaxHeight())
                // The lists read better at a column's width than across a whole screen.
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.widthIn(max = 1100.dp).fillMaxHeight()) { page() }
                }
            }
        }
    }

    Overlays(s, model, open) { open = it }
}

/** Ticking a line; when it finishes the month, the month gives way to the next and Undo is offered. */
private fun tick(s: UiState, row: BillRow, model: CashModel, onFinished: (Finished) -> Unit) {
    val finishes = !row.paid && s.bills.open == 1
    model.setPaid(row.item.id, s.bills.month, !row.paid)
    if (finishes) onFinished(Finished(row.item.id, s.bills.month))
}

private fun UiState.regular(id: String): RegularRow? = (incomes + regularSpendings).firstOrNull { it.id == id }

private fun edit(row: SpendingRow) =
    AmountFor.EditSpending(row.id, row.kindOf, row.amount, LocalDate.ofEpochDay(row.day), row.note)

private fun edit(row: RegularRow) =
    AmountFor.EditRegular(row.id, row.income, row.kindOf, row.amount, row.dayOfMonth, row.note)

/** The "+" sheets, the edit sheets and the questions before a delete. */
@Composable
private fun Overlays(s: UiState, model: CashModel, open: Open?, set: (Open?) -> Unit) {
    when (open) {
        null -> Unit
        is Open.Kinds -> KindSheet(open.showIncome,
            onIncome = { set(Open.Amount(AmountFor.NewIncome)) },
            onKind = { set(Open.Amount(AmountFor.NewSpending(it, open.monthly))) },
            onDismiss = { set(null) })
        is Open.Amount -> AmountSheet(open.target, s.settings.currency, s.today,
            onSave = { a -> save(model, open.target, a); set(null) },
            onDelete = when (val t = open.target) {
                is AmountFor.EditSpending -> ({ model.deleteSpending(t.id); set(null) })
                is AmountFor.EditRegular -> ({ model.deleteRegular(t.id); set(null) })
                else -> null
            },
            onDismiss = { set(null) })
        is Open.SpendingActions -> ActionsSheet(open.row.note.ifBlank { open.row.kindOf.title },
            onEdit = { set(Open.Amount(edit(open.row))) },
            onDelete = { set(Open.ConfirmSpending(open.row)) },
            onDismiss = { set(null) })
        is Open.RegularActions -> ActionsSheet(
            open.row.note.ifBlank { if (open.row.income) t("Income") else open.row.kindOf.title },
            onEdit = { set(Open.Amount(edit(open.row))) },
            onDelete = { set(Open.ConfirmRegular(open.row)) },
            onDismiss = { set(null) })
        is Open.ConfirmSpending -> ConfirmDelete(t("Delete this spending?"), null,
            onConfirm = { model.deleteSpending(open.row.id); set(null) }, onDismiss = { set(null) })
        is Open.ConfirmRegular -> ConfirmDelete(t("Stop this regular item?"),
            t("What it has already added to Spendings stays."),
            onConfirm = { model.deleteRegular(open.row.id); set(null) }, onDismiss = { set(null) })
    }
}

/** Saves what the amount sheet hands back, as what it was opened for. */
fun save(model: CashModel, target: AmountFor, a: Amounted) {
    when (target) {
        is AmountFor.NewSpending ->
            if (a.monthly) model.addRegular(false, a.kind, a.amount, a.note, a.dayOfMonth)
            else model.addSpending(a.kind, a.amount, a.day, a.note)
        AmountFor.NewIncome -> model.addRegular(true, Kind.OTHER, a.amount, a.note, a.dayOfMonth)
        is AmountFor.EditSpending -> model.updateSpending(target.id, a.kind, a.amount, a.day, a.note)
        is AmountFor.EditRegular -> model.updateRegular(target.id, a.kind, a.amount, a.note, a.dayOfMonth)
    }
}

/** A line above the tabs for a few seconds, with one thing to do: undo. */
@Composable
private fun UndoBar(text: String, modifier: Modifier, onUndo: () -> Unit) {
    val c = HubidubiCashTheme.colors
    Row(
        modifier.padding(12.dp).fillMaxWidth().defaultMinSize(minHeight = 52.dp)
            .background(c.raised).border(1.dp, c.line).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
            color = c.ink, modifier = Modifier.weight(1f))
        TextButton(onClick = onUndo) {
            Text(t("Undo"), color = c.accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The phone's tab bar, drawn as HubidubiTask's: the nav colour, a 2dp accent
 * rule over the selected tab, its icon and label in the accent. Each of the
 * five tabs gets a full fifth of the width, and if a language's longest label
 * still does not fit, every label is set a little smaller, all alike.
 */
@Composable
private fun BottomTabs(selected: Tab, onSelect: (Tab) -> Unit) {
    val c = HubidubiCashTheme.colors
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    BoxWithConstraints(Modifier.fillMaxWidth().background(c.nav).navigationBarsPadding()) {
        val density = LocalDensity.current
        val room = with(density) { (maxWidth / Tab.entries.size - 6.dp).toPx() }
        val widest = Tab.entries.maxOf {
            measurer.measure(t(it.label), style.copy(fontWeight = FontWeight.SemiBold)).size.width
        }.toFloat()
        val labelSize = if (widest <= room) style.fontSize else style.fontSize * (room / widest)
        Row(Modifier.fillMaxWidth().height(80.dp).selectableGroup()) {
            Tab.entries.forEach { d ->
                val on = d == selected
                val tint = if (on) c.accent else c.muted
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(d) })
                        .drawBehind {
                            if (on) drawLine(c.accent, Offset(0f, 1f), Offset(size.width, 1f), strokeWidth = 2.dp.toPx())
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(d.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Text(t(d.label), style = style.copy(fontSize = labelSize), color = tint, maxLines = 1,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
