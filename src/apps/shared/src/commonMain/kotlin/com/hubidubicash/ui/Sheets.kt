package com.hubidubicash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hubidubicash.core.Kind
import com.hubidubicash.core.Money
import com.hubidubicash.core.Window
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency

/**
 * "+": what kind of spending? The eight kinds, and on Regular and Bills an
 * income first. As HubidubiTask's "New" sheet asks what kind of item.
 */
@Composable
fun KindSheet(showIncome: Boolean, onIncome: () -> Unit, onKind: (Kind) -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    AppSheet(onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetTitle(if (showIncome) t("New") else t("New spending"))
            if (showIncome) ActionRow(IncomeIcon, t("Income"), color = c.accent, onClick = onIncome)
            Kind.entries.forEach { k ->
                ActionRow(k.icon, k.title, color = c.accent, last = k == Kind.entries.last()) { onKind(k) }
            }
            SecondaryButton(t("Cancel"), onDismiss, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    }
}

/** What the amount sheet is for. */
sealed interface AmountFor {
    /** A new spending of [kind]; [monthly] is where the "Once | Every month" choice starts. */
    data class NewSpending(val kind: Kind, val monthly: Boolean) : AmountFor
    data object NewIncome : AmountFor
    data class EditSpending(val id: String, val kind: Kind, val amount: Long, val day: LocalDate, val note: String) : AmountFor
    data class EditRegular(val id: String, val income: Boolean, val kind: Kind, val amount: Long, val dayOfMonth: Int,
                           val note: String) : AmountFor
}

/** What the amount sheet hands back on Save. */
data class Amounted(val kind: Kind, val amount: Long, val note: String, val monthly: Boolean, val day: LocalDate,
                    val dayOfMonth: Int)

/**
 * The amount: typed first, with the keyboard already up; then an optional
 * note and when. Done on the keyboard saves, as Save does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountSheet(
    target: AmountFor,
    currency: Currency,
    today: LocalDate,
    onSave: (Amounted) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val locale = I18n.language.locale
    val income = target is AmountFor.NewIncome || (target is AmountFor.EditRegular && target.income)
    val startKind = when (target) {
        is AmountFor.NewSpending -> target.kind
        is AmountFor.EditSpending -> target.kind
        is AmountFor.EditRegular -> target.kind
        AmountFor.NewIncome -> Kind.OTHER
    }
    val startAmount = when (target) {
        is AmountFor.EditSpending -> Money.editable(target.amount, currency, locale)
        is AmountFor.EditRegular -> Money.editable(target.amount, currency, locale)
        else -> ""
    }
    var amount by remember { mutableStateOf(startAmount) }
    var note by remember { mutableStateOf(when (target) {
        is AmountFor.EditSpending -> target.note
        is AmountFor.EditRegular -> target.note
        else -> ""
    }) }
    var kind by remember { mutableStateOf(startKind) }
    var monthly by remember { mutableStateOf(when (target) {
        is AmountFor.NewSpending -> target.monthly
        is AmountFor.EditSpending -> false
        else -> true
    }) }
    var day by remember { mutableStateOf((target as? AmountFor.EditSpending)?.day ?: today) }
    var dayOfMonth by remember { mutableStateOf((target as? AmountFor.EditRegular)?.dayOfMonth ?: 1) }
    var picking by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val parsed = Money.parse(amount)
    val focus = remember { FocusRequester() }
    val editing = target is AmountFor.EditSpending || target is AmountFor.EditRegular
    fun save() { parsed?.let { onSave(Amounted(kind, it, note.trim(), monthly, day, dayOfMonth)) } }

    AppSheet(onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            val overline = when (target) {
                is AmountFor.NewSpending -> t("New spending")
                AmountFor.NewIncome -> t("New regular income")
                is AmountFor.EditSpending -> t("Spending")
                is AmountFor.EditRegular -> t("Every month")
            }
            SheetTitle(if (income) t("Income") else kind.title, overline, if (income) IncomeIcon else kind.icon)
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.take(16) },
                label = { Text(t("Amount")) },
                suffix = { Text(Money.symbol(currency, locale), fontSize = 20.sp) },
                singleLine = true, colors = appFieldColors(),
                shape = androidx.compose.ui.graphics.RectangleShape,
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).focusRequester(focus),
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it.take(80) },
                label = { Text(t("Note (optional)")) },
                singleLine = true, colors = appFieldColors(),
                shape = androidx.compose.ui.graphics.RectangleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            if (target is AmountFor.NewSpending) {
                Segmented(listOf(false to t("Once"), true to t("Every month")), monthly, { monthly = it },
                    Modifier.padding(top = 16.dp))
            }
            val labels = listOf(t("Kind"), t("Date"), t("Day of month"))
            val labelWidth = labelColumnWidth(labels, max = 150.dp)
            Spacer(Modifier.height(8.dp))
            if (editing && !income) {
                SettingRow(t("Kind"), kind.title,
                    Kind.entries.map { k -> DropdownItem(k.title, k == kind) { kind = k } }, labelWidth)
            }
            if (monthly) {
                SettingRow(t("Day of month"), dayOfMonth.toString(),
                    (1..31).map { d -> DropdownItem(d.toString(), d == dayOfMonth) { dayOfMonth = d } }, labelWidth,
                    last = true)
            } else {
                SettingRow(t("Date"), dayLabel(day, today), emptyList(), labelWidth, last = true,
                    onClick = { picking = true })
            }
            PrimaryButton(t("Save"), ::save, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = parsed != null)
            if (onDelete != null) {
                SecondaryButton(t("Delete"), { deleting = true }, Modifier.fillMaxWidth().padding(top = 10.dp))
            }
        }
    }
    LaunchedEffect(Unit) { if (!editing) runCatching { focus.requestFocus() } }
    if (picking) DayPicker(day, today, { day = it; picking = false }, { picking = false })
    if (deleting && onDelete != null) {
        ConfirmDelete(
            if (target is AmountFor.EditRegular) t("Stop this regular item?") else t("Delete this spending?"),
            if (target is AmountFor.EditRegular) t("What it has already added to Spendings stays.") else null,
            onConfirm = { deleting = false; onDelete() },
            onDismiss = { deleting = false },
        )
    }
}

/** Asked before anything is deleted: what goes, and whether anything stays. */
@Composable
fun ConfirmDelete(title: String, detail: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.paper,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = c.ink) },
        text = detail?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.ink) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(t("Delete"), color = c.danger, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("Cancel"), color = c.ink, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/**
 * What can be done to one row, from a long press, as in HubidubiTask: actions
 * live here rather than as icons in the row.
 */
@Composable
fun ActionsSheet(title: String, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    AppSheet(onDismiss) {
        SheetTitle(title)
        ActionRow(Icons.Outlined.Edit, t("Edit"), color = c.accent, onClick = onEdit)
        ActionRow(Icons.Outlined.Delete, t("Delete"), color = c.danger, last = true, onClick = onDelete)
        SecondaryButton(t("Cancel"), onDismiss, Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcDay(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Days up to today: a spending is something already spent. */
@OptIn(ExperimentalMaterial3Api::class)
private class UpTo(private val today: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) = !utcTimeMillis.utcDay().isAfter(today)
    override fun isSelectableYear(year: Int) = year <= today.year
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPicker(day: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    val state = rememberDatePickerState(initialSelectedDateMillis = day.utcMillis(), selectableDates = UpTo(today))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it.utcDay()) } ?: onDismiss() }) {
                Text(t("OK"), color = c.accent, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("Cancel"), color = c.ink, style = MaterialTheme.typography.labelLarge) }
        },
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = DatePickerDefaults.colors(containerColor = c.paper),
    ) { DatePicker(state, colors = DatePickerDefaults.colors(containerColor = c.paper)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangePicker(start: LocalDate, end: LocalDate, today: LocalDate, onPick: (Window) -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start.utcMillis(), initialSelectedEndDateMillis = end.utcMillis(),
        selectableDates = UpTo(today),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val s = state.selectedStartDateMillis
                if (s == null) onDismiss()
                else onPick(Window(s.utcDay(), (state.selectedEndDateMillis ?: s).utcDay()))
            }) { Text(t("OK"), color = c.accent, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("Cancel"), color = c.ink, style = MaterialTheme.typography.labelLarge) }
        },
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = DatePickerDefaults.colors(containerColor = c.paper),
    ) {
        DateRangePicker(state, Modifier.height(480.dp), colors = DatePickerDefaults.colors(containerColor = c.paper))
    }
}

/** The password a backup is sealed with, asked twice before Export saves it. */
@Composable
fun BackupPasswordSheet(onExport: (CharArray) -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val problem = when {
        password.isNotEmpty() && password.length < MIN_BACKUP_PASSWORD -> t("At least 12 characters")
        again.isNotEmpty() && again != password -> t("The passwords differ.")
        else -> null
    }
    val ok = password.length >= MIN_BACKUP_PASSWORD && again == password
    AppSheet(onDismiss) {
        SheetTitle(t("Export"))
        PasswordField(password, { password = it }, t("Password"), ImeAction.Next) {}
        PasswordField(again, { again = it }, t("Same password again"), ImeAction.Done) {
            if (ok) onExport(password.toCharArray())
        }
        Text(problem ?: t("Without it, the backup can't be opened."),
            style = MaterialTheme.typography.bodySmall, color = if (problem != null) c.danger else c.muted,
            modifier = Modifier.padding(top = 10.dp))
        PrimaryButton(t("Export"), { onExport(password.toCharArray()) },
            Modifier.fillMaxWidth().padding(top = 18.dp), enabled = ok)
    }
}

/** The password of a sealed backup, to open it for Import. */
@Composable
fun OpenBackupSheet(problem: String?, onOpen: (CharArray) -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    var password by remember { mutableStateOf("") }
    AppSheet(onDismiss) {
        SheetTitle(t("Import"))
        PasswordField(password, { password = it }, t("The backup's password"), ImeAction.Done) {
            if (password.isNotEmpty()) onOpen(password.toCharArray())
        }
        problem?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.danger, modifier = Modifier.padding(top = 10.dp))
        }
        PrimaryButton(t("Open"), { onOpen(password.toCharArray()) },
            Modifier.fillMaxWidth().padding(top = 18.dp), enabled = password.isNotEmpty())
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, action: ImeAction, onDone: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        colors = appFieldColors(), shape = androidx.compose.ui.graphics.RectangleShape,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = action),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** A short field-and-button dialog: the life costs a month. */
@Composable
fun AmountDialog(title: String, start: Long, currency: Currency, onSave: (Long) -> Unit, onDismiss: () -> Unit) {
    val c = HubidubiCashTheme.colors
    val locale = I18n.language.locale
    var text by remember { mutableStateOf(Money.editable(start, currency, locale)) }
    // Zero is allowed here: no life costs at all.
    val value = if (text.isNotBlank() && text.all { it == '0' || it == ',' || it == '.' || it.isWhitespace() }) 0L
        else Money.parse(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.paper,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = c.ink) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(16) }, singleLine = true,
                suffix = { Text(Money.symbol(currency, locale)) },
                colors = appFieldColors(), shape = androidx.compose.ui.graphics.RectangleShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { value?.let(onSave) }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSave) }, enabled = value != null) {
                Text(t("Save"), color = if (value != null) c.accent else c.muted, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("Cancel"), color = c.ink, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

private const val MIN_BACKUP_PASSWORD = 12
