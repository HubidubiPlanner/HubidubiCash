package com.hubidubicash.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The few pieces every screen is built from, as HubidubiTask builds its own:
// the same title, the same grouped card, the same 48dp row, the same two
// buttons, the same "+". Keeping them here is what makes five screens look
// like one app, and the two apps like one family.

/** A screen's title, with an optional line under it and an action to its right. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = HubidubiCashTheme.colors
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp)) {
        // The trailing button sits on the title's line, so a subtitle never
        // moves it: the + is in the same place on every tab.
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = c.ink,
                modifier = Modifier.weight(1f))
            trailing?.invoke(this)
        }
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * The "+" at the top right of a tab, as in HubidubiTask: the accent "+" alone,
 * no circle, in a 48dp target that never makes the title row taller.
 */
@Composable
fun PlusButton(description: String, onClick: () -> Unit) {
    val c = HubidubiCashTheme.colors
    Box(
        modifier = Modifier
            .layout { m, cs ->
                val p = m.measure(cs.copy(minHeight = 0))
                val h = 34.sp.roundToPx()
                layout(p.width, h) { p.place(0, (h - p.height) / 2) }
            }
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                role = Role.Button, onClickLabel = description, onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = description, tint = c.accent, modifier = Modifier.size(30.dp))
    }
}

/** RENT, SAT 31 OCT: the small tracked label above a group, with a value on its right. */
@Composable
fun SectionLabel(
    text: String,
    color: Color = HubidubiCashTheme.colors.muted,
    value: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.weight(1f))
        value?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = HubidubiCashTheme.colors.muted)
        }
    }
}

/** A group of rows on a raised card. Borders, not shadows: flat paper stays flat. */
@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    border: Color = HubidubiCashTheme.colors.line,
    // From the screen's edge to the card's.
    inset: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = HubidubiCashTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = inset)
            .background(c.raised).border(1.dp, border),
        content = content,
    )
}

/**
 * One tappable row inside a [CardGroup]: at least 48dp tall, an optional glyph
 * on the left, a hairline under it unless it is the last.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRow(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    last: Boolean = false,
    startPadding: Int = 16,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val c = HubidubiCashTheme.colors
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = startPadding.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { it(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
            if (!last) HorizontalDivider(color = c.line, thickness = 1.dp)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = HubidubiCashTheme.colors
    Button(
        onClick = onClick, enabled = enabled, shape = RectangleShape,
        // At least the usual height; taller when the words take two lines.
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.button, contentColor = c.onButton,
            disabledContainerColor = c.line, disabledContentColor = c.muted,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) { ButtonContent(text, icon) }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = HubidubiCashTheme.colors
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RectangleShape,
        modifier = modifier.height(48.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (enabled) c.frame else c.line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = c.ink, disabledContentColor = c.muted,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) { ButtonContent(text, icon) }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    icon?.let {
        Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

/** Field colours shared by every text input: raised fill, accent edge when in use. */
@Composable
fun appFieldColors(): TextFieldColors {
    val c = HubidubiCashTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.raised, unfocusedContainerColor = c.raised,
        disabledContainerColor = c.paper,
        focusedBorderColor = c.accent, unfocusedBorderColor = c.glyph,
        disabledBorderColor = c.line,
        focusedLabelColor = c.accent, unfocusedLabelColor = c.muted,
        cursorColor = c.accent,
        focusedTextColor = c.ink, unfocusedTextColor = c.ink,
        focusedSuffixColor = c.muted, unfocusedSuffixColor = c.muted,
    )
}

/** The title a bottom sheet opens with, plus the muted line above it. */
@Composable
fun SheetTitle(title: String, overline: String? = null, icon: ImageVector? = null) {
    val c = HubidubiCashTheme.colors
    overline?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
    Row(
        Modifier.padding(top = if (overline != null) 4.dp else 0.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = c.accent, modifier = Modifier.size(26.dp)) }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.ink)
    }
}

/**
 * A sheet from the bottom edge: the one surface every choice, detail and
 * question opens in, so they all appear in the same place under the thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = HubidubiCashTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.paper,
        contentColor = c.ink,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 18.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(c.glyph, RoundedCornerShape(2.dp)))
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
                .navigationBarsPadding(),
            content = content,
        )
    }
}

/** One choice in a sheet: icon, label, optional value on the right. */
@Composable
fun ActionRow(
    icon: ImageVector,
    label: String,
    meta: String? = null,
    color: Color = HubidubiCashTheme.colors.ink,
    enabled: Boolean = true,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    val c = HubidubiCashTheme.colors
    val tint = if (enabled) color else c.muted
    Column(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = tint,
                modifier = Modifier.weight(1f))
            meta?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
        }
        if (!last) HorizontalDivider(color = c.line, thickness = 1.dp)
    }
}

/** Two to four mutually exclusive choices side by side: once or every month, a range of dates. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = HubidubiCashTheme.colors
    Row(modifier.fillMaxWidth().border(1.dp, c.glyph)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier.weight(1f).height(44.dp)
                    .background(if (on) c.button else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) c.onButton else c.ink, maxLines = 1)
            }
        }
    }
}

/** One entry of a setting's choices: what it says, and what choosing it does. */
data class DropdownItem(
    val label: String,
    val selected: Boolean = false,
    /** Shown on the right of the entry, quieter: what a theme does. */
    val detail: String? = null,
    val onClick: () -> Unit,
)

/**
 * A setting's choices, opened under it at its width. A quiet detail and the
 * current choice's tick sit on the right, in columns that line up.
 */
@Composable
private fun ChoicesMenu(open: Boolean, onDismiss: () -> Unit, items: List<DropdownItem>, width: Int) {
    val c = HubidubiCashTheme.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val detailStyle = MaterialTheme.typography.bodySmall
    val detailWidth = items.mapNotNull { it.detail }.maxOfOrNull { measurer.measure(it, detailStyle).size.width }
        ?.let { with(density) { it.toDp() } + 2.dp }
    DropdownMenu(
        expanded = open, onDismissRequest = onDismiss,
        modifier = Modifier.width(with(density) { width.toDp() }).heightIn(max = 420.dp),
        shape = RectangleShape, containerColor = c.raised,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.glyph),
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (detailWidth != null)
                            Text(item.detail.orEmpty(), style = detailStyle, color = c.muted, maxLines = 1,
                                textAlign = TextAlign.End, modifier = Modifier.width(detailWidth))
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            if (item.selected)
                                Icon(Icons.Filled.Check, contentDescription = null, tint = c.accent,
                                    modifier = Modifier.size(20.dp))
                        }
                    }
                },
                onClick = { onDismiss(); item.onClick() },
            )
        }
    }
}

/**
 * One row of Settings' table, no frames: what the setting is on the left, at
 * [labelWidth], what it is set to on the right with a chevron. The whole row
 * opens its choices, or does [onClick] when it has none; a thin line under
 * it, except under the last.
 */
@Composable
fun SettingRow(
    label: String,
    value: String,
    items: List<DropdownItem>,
    labelWidth: Dp,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = HubidubiCashTheme.colors
    var open by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        Column {
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp)
                    .clickable(role = Role.DropdownList) { if (onClick != null) onClick() else open = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = c.ink, modifier = Modifier.width(labelWidth))
                Text(value, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = c.accent,
                    modifier = Modifier.size(22.dp))
            }
            if (!last) HorizontalDivider(color = c.line, thickness = 1.dp)
        }
        ChoicesMenu(open, { open = false }, items, width)
    }
}

/**
 * The table's first column: as wide as the widest of [labels] in a row's
 * type, so every value starts at one line, and at most [max].
 */
@Composable
fun labelColumnWidth(labels: List<String>, max: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    val px = labels.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
    // A little air after the longest name, so it doesn't run into its value.
    return (with(LocalDensity.current) { px.toDp() } + 16.dp).coerceAtMost(max)
}

/** A quiet line of text where a list is empty: no picture, just what to do. */
@Composable
fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = HubidubiCashTheme.colors.muted,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
}
