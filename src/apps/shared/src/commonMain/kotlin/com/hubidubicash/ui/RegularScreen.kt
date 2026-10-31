package com.hubidubicash.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hubidubicash.core.i18n.t
import com.hubidubicash.data.RegularRow

/** Every regular income and spending at once: what comes in and goes out each month. */
@Composable
fun RegularScreen(state: UiState, onPlus: () -> Unit, onOpen: (RegularRow) -> Unit, onHold: (RegularRow) -> Unit) {
    val currency = state.settings.currency
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        ScreenHeader(t("Regular"), t("Every month, added on its day")) {
            PlusButton(t("New regular income or spending"), onPlus)
        }
        if (state.incomes.isEmpty() && state.regularSpendings.isEmpty()) {
            EmptyLine(t("Nothing regular yet. Tap + to add rent, a salary or a subscription."))
        } else {
            Group(t("Income"), state.incomes, income = true, state, onOpen, onHold)
            Group(t("Spendings"), state.regularSpendings, income = false, state, onOpen, onHold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Group(
    label: String,
    rows: List<RegularRow>,
    income: Boolean,
    state: UiState,
    onOpen: (RegularRow) -> Unit,
    onHold: (RegularRow) -> Unit,
) {
    val c = HubidubiCashTheme.colors
    val currency = state.settings.currency
    if (rows.isEmpty()) return
    SectionLabel(label, value = signedMoney(rows.sumOf { it.amount }, income, currency))
    CardGroup {
        rows.forEachIndexed { i, r ->
            val icon = if (r.income) IncomeIcon else r.kindOf.icon
            val name = r.note.ifBlank { if (r.income) t("Income") else r.kindOf.title }
            ListRow(onClick = { onOpen(r) }, onLongClick = { onHold(r) }, last = i == rows.lastIndex,
                leading = { Icon(icon, contentDescription = null, tint = c.glyph, modifier = Modifier.size(20.dp)) }) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = c.ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(t("Day %d", r.dayOfMonth), style = MaterialTheme.typography.bodySmall, color = c.muted)
                Text(signedMoney(r.amount, r.income, currency), style = MaterialTheme.typography.bodyMedium, color = c.ink)
            }
        }
    }
}
