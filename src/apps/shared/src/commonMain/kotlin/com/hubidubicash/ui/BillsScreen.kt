package com.hubidubicash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubidubicash.core.BillRow
import com.hubidubicash.core.Kind
import com.hubidubicash.core.Money
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t

private val DAY = 56.dp
private val AMOUNT = 112.dp
private val PAID = 52.dp

/**
 * Bills: one month as a table, a line per regular income and spending, each
 * ticked when it is paid or has come in, then the life costs still to come,
 * then the total of every line. Once every line is ticked, the next month.
 */
@Composable
fun BillsScreen(state: UiState, onPlus: () -> Unit, onTick: (BillRow) -> Unit, onOpen: (BillRow) -> Unit) {
    val c = HubidubiCashTheme.colors
    val bills = state.bills
    val currency = state.settings.currency
    val locale = I18n.language.locale
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        val left = when {
            bills.rows.isEmpty() -> null
            bills.open == 0 -> t("all ticked")
            else -> t("%d still to tick", bills.open)
        }
        ScreenHeader(t("Bills"), listOfNotNull(I18n.monthYear(bills.month), left).joinToString(" · ")) {
            PlusButton(t("New regular income or bill"), onPlus)
        }
        Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().background(c.raised).border(1.dp, c.line)) {
            GridRow(Modifier.background(c.nav), height = 34.dp) {
                Cell(DAY, Alignment.CenterEnd) { Header(t("Day")) }
                Cell(null, Alignment.CenterStart) { Header(t("Name")) }
                Cell(AMOUNT, Alignment.CenterEnd) { Header(Money.symbol(currency, locale)) }
                // A tick for "paid", the same in every language and as narrow as the boxes under it.
                Cell(PAID, Alignment.Center, last = true) {
                    Icon(Icons.Filled.Check, contentDescription = t("Paid"), tint = c.muted, modifier = Modifier.size(18.dp))
                }
            }
            bills.rows.forEach { row ->
                val item = row.item
                val name = item.note.ifBlank { if (item.income) t("Income") else Kind.of(item.kind).title }
                GridRow(Modifier.clickable { onOpen(row) }) {
                    Cell(DAY, Alignment.CenterEnd) {
                        Text(row.date.dayOfMonth.toString(), style = cellText(),
                            color = if (row.late) c.danger else c.ink,
                            fontWeight = if (row.late) FontWeight.SemiBold else FontWeight.Normal)
                    }
                    Cell(null, Alignment.CenterStart) {
                        NameCell(if (item.income) IncomeIcon else Kind.of(item.kind).icon, name)
                    }
                    Cell(AMOUNT, Alignment.CenterEnd) {
                        Text(Money.signed(row.signed, currency, locale), style = cellText(), color = c.ink, maxLines = 1)
                    }
                    Cell(PAID, Alignment.Center, last = true) {
                        Tick(row.paid, if (item.income) t("Received: %s", name) else t("Paid: %s", name)) { onTick(row) }
                    }
                }
            }
            GridRow {
                Cell(DAY, Alignment.CenterEnd) {}
                Cell(null, Alignment.CenterStart) { NameCell(Icons.Outlined.ShoppingCart, t("Life costs")) }
                Cell(AMOUNT, Alignment.CenterEnd) {
                    Text(Money.signed(-bills.lifeCosts, currency, locale), style = cellText(), color = c.ink, maxLines = 1)
                }
                Cell(PAID, Alignment.Center, last = true) {}
            }
            Row(Modifier.fillMaxWidth().background(c.paper).defaultMinSize(minHeight = 48.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(t("Total"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = c.ink, modifier = Modifier.weight(1f))
                Text(money(bills.total, currency), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = c.ink)
            }
        }
        if (bills.rows.isEmpty()) EmptyLine(t("No regular income or bills yet. Tap + to add one."))
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun cellText() = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp)

@Composable
private fun Header(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = HubidubiCashTheme.colors.muted,
        maxLines = 1)
}

@Composable
private fun NameCell(icon: ImageVector, name: String) {
    val c = HubidubiCashTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = c.glyph, modifier = Modifier.size(18.dp))
        Text(name, style = cellText(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A line of the table; every cell draws the rule to its right, the line its rule below. */
@Composable
private fun GridRow(modifier: Modifier = Modifier, height: Dp = 44.dp, content: @Composable RowScope.() -> Unit) {
    val line = HubidubiCashTheme.colors.line
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).defaultMinSize(minHeight = height)
            .drawBehind {
                val w = 1.dp.toPx()
                drawLine(line, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
            },
        content = content,
    )
}

@Composable
private fun RowScope.Cell(width: Dp?, align: Alignment, last: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val line = HubidubiCashTheme.colors.line
    Box(
        (if (width == null) Modifier.weight(1f) else Modifier.width(width)).fillMaxHeight()
            .drawBehind {
                if (!last) {
                    val w = 1.dp.toPx()
                    drawLine(line, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
                }
            }
            .padding(horizontal = when (width) { PAID -> 0.dp; DAY -> 6.dp; else -> 8.dp }),
        contentAlignment = align,
        content = content,
    )
}

/** A square tick box, filled with the button colour and a check once paid. */
@Composable
private fun Tick(paid: Boolean, label: String, onToggle: () -> Unit) {
    val c = HubidubiCashTheme.colors
    Box(
        Modifier.size(44.dp)
            .toggleable(value = paid, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(20.dp).background(if (paid) c.button else Color.Transparent)
                .border(2.dp, if (paid) c.button else c.glyph),
            contentAlignment = Alignment.Center,
        ) {
            if (paid) Icon(Icons.Filled.Check, contentDescription = null, tint = c.onButton, modifier = Modifier.size(16.dp))
        }
    }
}
