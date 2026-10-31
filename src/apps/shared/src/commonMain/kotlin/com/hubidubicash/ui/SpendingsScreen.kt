package com.hubidubicash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hubidubicash.core.Window
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t
import com.hubidubicash.data.CashRepository
import com.hubidubicash.data.SpendingRow
import java.time.LocalDate

/**
 * Every spending in the dates chosen, newest first, under its day and the
 * day's total; at most 50 at a time, the rest a page older.
 */
@Composable
fun SpendingsScreen(
    state: UiState,
    onPlus: () -> Unit,
    onRange: (Range, Window?) -> Unit,
    onOpen: (SpendingRow) -> Unit,
    onHold: (SpendingRow) -> Unit,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
) {
    val c = HubidubiCashTheme.colors
    val list = state.list
    val page = list.page
    val currency = state.settings.currency
    var picking by remember { mutableStateOf(false) }
    val byDay = page.rows.groupBy { it.day }
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            ScreenHeader(t("Spendings"),
                I18n.span(list.window.first, list.window.last) + " · " + I18n.count(page.count, "spending")) {
                PlusButton(t("New spending"), onPlus)
            }
        }
        item {
            Segmented(
                listOf(Range.WEEK to t("Week"), Range.MONTH to t("Month"), Range.YEAR to t("Year"), Range.DATES to t("Dates")),
                list.range,
                { if (it == Range.DATES) picking = true else onRange(it, null) },
                Modifier.padding(horizontal = 20.dp),
            )
        }
        if (page.rows.isEmpty()) item { EmptyLine(t("No spendings in these days.")) }
        byDay.forEach { (day, rows) ->
            item(key = "h$day") {
                val d = LocalDate.ofEpochDay(day)
                SectionLabel(dayLabel(d, state.today), value = money(page.dayTotals[day] ?: 0, currency))
            }
            item(key = "c$day") {
                CardGroup {
                    rows.forEachIndexed { i, s ->
                        ListRow(onClick = { onOpen(s) }, onLongClick = { onHold(s) }, last = i == rows.lastIndex,
                            leading = { Icon(s.kindOf.icon, contentDescription = s.kindOf.title, tint = c.glyph,
                                modifier = Modifier.size(20.dp)) }) {
                            Text(s.note.ifBlank { s.kindOf.title }, style = MaterialTheme.typography.bodyMedium,
                                color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (s.regularId != null) Icon(Icons.Outlined.Repeat, contentDescription = t("Regular"),
                                tint = c.glyph, modifier = Modifier.size(18.dp))
                            Text(money(s.amount, currency), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        }
                    }
                }
            }
        }
        if (page.count > CashRepository.PAGE) item {
            val last = minOf(page.offset + CashRepository.PAGE, page.count)
            Text(t("%d–%d of %d", page.offset + 1, last, page.count), style = MaterialTheme.typography.bodySmall,
                color = c.muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp))
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(t("Newer"), onNewer, Modifier.weight(1f), enabled = page.offset > 0)
                SecondaryButton(t("Older"), onOlder, Modifier.weight(1f), enabled = last < page.count)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (picking) RangePicker(list.window.first, list.window.last, state.today,
        { picking = false; onRange(Range.DATES, it) }, { picking = false })
}
