package com.hubidubicash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hubidubicash.core.Chart
import com.hubidubicash.core.Money
import com.hubidubicash.core.Period
import com.hubidubicash.core.Sealed
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.Language
import com.hubidubicash.core.i18n.t
import com.hubidubicash.data.Backup
import kotlinx.coroutines.launch
import java.util.Currency

/** Settings: a frameless table of choices, then the backup, as HubidubiTask's Settings. */
@Composable
fun SettingsScreen(state: UiState, model: CashModel) {
    val c = HubidubiCashTheme.colors
    val s = state.settings
    val locale = I18n.language.locale
    val scope = rememberCoroutineScope()
    val picker = rememberFilePicker()
    var lifeCosts by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    // Import: the file read, then its password if it is sealed, then what it would do.
    var file by remember { mutableStateOf<ByteArray?>(null) }
    var passwordProblem by remember { mutableStateOf<String?>(null) }
    var askingPassword by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<Backup.Plan?>(null) }

    fun problem(e: Throwable): String = when (e) {
        is Sealed.WrongPassword -> t("That password doesn't open this backup.")
        is Sealed.Damaged -> t("The backup is damaged or incomplete.")
        is Backup.NotABackup -> t("This file is not a HubidubiCash backup.")
        else -> t("The import failed: %s", e.message ?: e.toString())
    }

    fun open(bytes: ByteArray, password: CharArray?) = scope.launch {
        runCatching { model.plan(model.readBackup(bytes, password)) }.fold(
            { askingPassword = false; plan = it },
            { e -> if (askingPassword) passwordProblem = problem(e) else status = problem(e) },
        )
        password?.fill(' ')
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        ScreenHeader(t("Settings"))
        val labels = listOf(t("Statistics for"), t("Chart"), t("Life costs"), t("Currency"), t("Language"), t("Theme"))
        val labelWidth = labelColumnWidth(labels, max = 180.dp)
        Column(Modifier.padding(horizontal = 20.dp)) {
            SettingRow(t("Statistics for"), periodLabel(s.period),
                Period.entries.map { p -> DropdownItem(periodLabel(p), p == s.period) { model.setPeriod(p) } }, labelWidth)
            SettingRow(t("Chart"), chartLabel(s.chart),
                Chart.entries.map { ch -> DropdownItem(chartLabel(ch), ch == s.chart) { model.setChart(ch) } }, labelWidth)
            SettingRow(t("Life costs"), t("%s a month", money(s.lifeCosts, s.currency)), emptyList(), labelWidth,
                onClick = { lifeCosts = true })
            val currencies = (Money.COMMON.map(Currency::getInstance) + Money.defaultCurrency() + s.currency).distinct()
            SettingRow(t("Currency"), currencyLabel(s.currency),
                currencies.map { cur -> DropdownItem(currencyLabel(cur), cur == s.currency) { model.setCurrency(cur) } },
                labelWidth)
            SettingRow(t("Language"), s.language.label,
                Language.entries.map { l -> DropdownItem(l.label, l == s.language) { model.setLanguage(l) } }, labelWidth)
            val theme = ColorTheme.of(s.theme)
            SettingRow(t("Theme"), t(theme.label),
                ColorTheme.entries.map { th -> DropdownItem(t(th.label), th == theme, detail = t(th.mode)) { model.setTheme(th) } },
                labelWidth, last = true)

            Row(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(t("Export"), { status = null; exporting = true }, Modifier.weight(1f),
                    icon = Icons.Outlined.FileUpload)
                SecondaryButton(t("Import"), {
                    status = null
                    scope.launch {
                        val input = picker.open() ?: return@launch
                        val bytes = runCatching { CashModel.bytes(input) }.getOrElse { status = problem(it); return@launch }
                        file = bytes
                        if (CashModel.isSealed(bytes)) { passwordProblem = null; askingPassword = true } else open(bytes, null)
                    }
                }, Modifier.weight(1f), icon = Icons.Outlined.FileDownload)
            }
            Text(status ?: t("A copy of everything, sealed with a password, to keep outside the app or move to another device."),
                style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (lifeCosts) AmountDialog(t("Life costs a month"), s.lifeCosts, s.currency,
        { model.setLifeCosts(it); lifeCosts = false }, { lifeCosts = false })
    if (exporting) BackupPasswordSheet(
        onExport = { password ->
            exporting = false
            scope.launch {
                val out = picker.create(Backup.fileName(state.today)) ?: return@launch password.fill(' ')
                status = t("Exporting…")
                status = runCatching { model.export(out, password) }.fold(
                    { (spendings, regulars) -> t("Exported %d spendings and %d regular items.", spendings, regulars) },
                    { t("The export failed: %s", it.message ?: it.toString()) })
                password.fill(' ')
            }
        },
        onDismiss = { exporting = false },
    )
    if (askingPassword) file?.let { bytes ->
        OpenBackupSheet(passwordProblem, onOpen = { open(bytes, it) }, onDismiss = { askingPassword = false })
    }
    plan?.let { p ->
        AlertDialog(
            onDismissRequest = { plan = null },
            containerColor = c.paper,
            title = { Text(t("Import this backup?"), style = MaterialTheme.typography.headlineSmall, color = c.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t("New here: %d", p.added), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                    Text(t("Newer than here, replacing what is here: %d", p.replaced),
                        style = MaterialTheme.typography.bodyMedium, color = c.ink)
                    Text(t("Already here, left as they are: %d", p.alreadyHere),
                        style = MaterialTheme.typography.bodyMedium, color = c.muted)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    plan = null
                    scope.launch {
                        status = runCatching { model.restore(p) }.fold({ t("Imported.") }, { problem(it) })
                    }
                }) { Text(t("Import"), color = c.accent, style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { plan = null }) {
                    Text(t("Cancel"), color = c.ink, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

private fun chartLabel(chart: Chart): String = when (chart) {
    Chart.BARS -> t("Bars")
    Chart.CIRCLE -> t("Circle")
}

/** "Polish Zloty (zł)", in the app's language. */
private fun currencyLabel(c: Currency): String {
    val locale = I18n.language.locale
    val name = c.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    val symbol = Money.symbol(c, locale)
    return if (symbol == c.currencyCode) "$name (${c.currencyCode})" else "$name ($symbol)"
}
