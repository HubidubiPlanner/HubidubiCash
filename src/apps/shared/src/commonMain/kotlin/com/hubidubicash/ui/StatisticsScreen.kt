package com.hubidubicash.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hubidubicash.core.Chart
import com.hubidubicash.core.Comparison
import com.hubidubicash.core.Period
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t
import java.text.NumberFormat
import java.util.Currency

/** "The last 7 days": how Settings and the subtitle name a period. */
fun periodLabel(p: Period): String = when (p) {
    Period.DAY -> t("Today")
    Period.WEEK -> t("The last 7 days")
    Period.MONTH -> t("The last month")
    Period.YEAR -> t("The last year")
}

/**
 * The first thing the app shows: what went out in the period chosen in
 * Settings, how that compares with the period before, and where it went.
 */
@Composable
fun StatisticsScreen(state: UiState, onPlus: () -> Unit) {
    val c = HubidubiCashTheme.colors
    val stats = state.stats
    val currency = state.settings.currency
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        ScreenHeader(t("Statistics"), periodLabel(stats.period) + " · " + I18n.span(stats.window.first, stats.window.last)) {
            PlusButton(t("New spending"), onPlus)
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp)) {
            Text(t("Spent").uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
            Text(money(stats.total, currency), color = c.ink, fontSize = 40.sp, lineHeight = 46.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em, fontFamily = PlexSans,
                modifier = Modifier.padding(top = 4.dp))
            ComparisonLine(stats.comparison, stats.period)
        }
        SectionLabel(t("By kind"))
        val spent = stats.kinds.filter { it.amount > 0 }
        if (spent.isEmpty()) EmptyLine(t("Nothing spent in these days. Tap + to note a spending."))
        else when (state.settings.chart) {
            Chart.BARS -> KindBars(spent, currency)
            Chart.CIRCLE -> KindCircle(stats.kinds, currency)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Less in the ok colour, more in the danger colour, each with its arrow: never colour alone. */
@Composable
private fun ComparisonLine(comparison: Comparison, period: Period) {
    val c = HubidubiCashTheme.colors
    val pct = { p: Long -> NumberFormat.getPercentInstance(I18n.language.locale).format(p / 100.0) }
    val (text, color, icon) = when (comparison) {
        is Comparison.Less -> Triple(when (period) {
            Period.DAY -> t("%s less than yesterday", pct(comparison.percent))
            Period.WEEK -> t("%s less than the week before", pct(comparison.percent))
            Period.MONTH -> t("%s less than the month before", pct(comparison.percent))
            Period.YEAR -> t("%s less than the year before", pct(comparison.percent))
        }, c.ok, Icons.AutoMirrored.Outlined.TrendingDown)
        is Comparison.More -> Triple(when (period) {
            Period.DAY -> t("%s more than yesterday", pct(comparison.percent))
            Period.WEEK -> t("%s more than the week before", pct(comparison.percent))
            Period.MONTH -> t("%s more than the month before", pct(comparison.percent))
            Period.YEAR -> t("%s more than the year before", pct(comparison.percent))
        }, c.danger, Icons.AutoMirrored.Outlined.TrendingUp)
        Comparison.Same -> Triple(t("About the same as before"), c.muted, Icons.AutoMirrored.Outlined.TrendingFlat)
        Comparison.NothingBefore -> Triple(t("Nothing was spent the time before"), c.muted, null)
        Comparison.Nothing -> return
    }
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        icon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
        Text(text, color = color, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.Medium)
    }
}

/** One bar per kind spent on, largest first, all in the accent: the length says how much. */
@Composable
private fun KindBars(spent: List<KindShare>, currency: Currency) {
    val c = HubidubiCashTheme.colors
    val sorted = spent.sortedByDescending { it.amount }
    val max = sorted.first().amount.toFloat()
    CardGroup {
        sorted.forEachIndexed { i, k ->
            Column(Modifier.padding(start = 16.dp)) {
                Column(Modifier.padding(end = 16.dp, top = 11.dp, bottom = 11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(k.kind.icon, contentDescription = null, tint = c.glyph, modifier = Modifier.size(20.dp))
                        Text(k.kind.title, style = MaterialTheme.typography.bodyMedium, color = c.ink,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(money(k.amount, currency), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        Text("${k.percent}%", style = MaterialTheme.typography.bodySmall, color = c.muted,
                            textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                    }
                    Box(Modifier.padding(start = 32.dp, top = 7.dp).fillMaxWidth().height(6.dp).background(c.well)) {
                        Box(Modifier.fillMaxWidth(k.amount / max).height(6.dp).background(c.accent))
                    }
                }
                if (i < sorted.lastIndex) HorizontalDivider(color = c.line, thickness = 1.dp)
            }
        }
    }
}

/**
 * A ring of the kinds in their fixed order, each in its own colour, a thin gap
 * between slices; the biggest kind's share in the middle; the legend under it
 * gives every amount in words, so nothing is told by colour alone.
 */
@Composable
private fun KindCircle(kinds: List<KindShare>, currency: Currency) {
    val c = HubidubiCashTheme.colors
    val spent = kinds.filter { it.amount > 0 }
    val total = spent.sumOf { it.amount }.toFloat()
    val biggest = spent.maxBy { it.amount }
    val colors = kinds.associate { it.kind to HubidubiCashTheme.kindColor(it.kind) }
    val description = t("Spending by kind") + ": " +
        spent.sortedByDescending { it.amount }.joinToString(", ") { "${it.kind.title} ${it.percent}%" }
    val ring: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(184.dp).semantics { contentDescription = description }) {
                val stroke = 28.dp.toPx()
                val r = (size.minDimension - stroke) / 2
                val gap = if (spent.size > 1) Math.toDegrees((2.dp.toPx() / r).toDouble()).toFloat() else 0f
                var at = -90f
                spent.forEach { k ->
                    val sweep = k.amount / total * 360f
                    drawArc(colors.getValue(k.kind), at + gap / 2, (sweep - gap).coerceAtLeast(0.5f), useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2), size = Size(r * 2, r * 2), style = Stroke(stroke))
                    at += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(biggest.kind.title, style = MaterialTheme.typography.bodySmall, color = c.muted)
                Text("${biggest.percent}%", style = MaterialTheme.typography.headlineMedium, color = c.ink)
            }
        }
    }
    val legend: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            spent.forEachIndexed { i, k ->
                Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(12.dp).background(colors.getValue(k.kind)))
                    Text(k.kind.title, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = c.ink,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(k.amount, currency), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = c.ink)
                    Text("${k.percent}%", style = MaterialTheme.typography.bodySmall, color = c.muted,
                        textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                }
                if (i < spent.lastIndex) HorizontalDivider(color = c.line, thickness = 1.dp)
            }
        }
    }
    CardGroup {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Side by side where there is room for both, the legend under the ring on a phone.
            if (maxWidth >= 600.dp) {
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    ring()
                    legend(Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 16.dp)) { ring() }
                    legend(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp))
                }
            }
        }
    }
}
