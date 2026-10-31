package com.hubidubicash.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.hubidubicash.core.Kind
import com.hubidubicash.core.Money
import com.hubidubicash.core.i18n.I18n
import com.hubidubicash.core.i18n.t
import java.time.LocalDate
import java.util.Currency

/** Each kind's glyph: Material's outlined set, as HubidubiTask's tabs and rows use. */
val Kind.icon: ImageVector
    get() = when (this) {
        Kind.FOOD -> Icons.Outlined.Restaurant
        Kind.HOME -> Icons.Outlined.Home
        Kind.TRANSPORT -> Icons.Outlined.DirectionsBus
        Kind.UTILITIES -> Icons.Outlined.Lightbulb
        Kind.HEALTH -> Icons.Outlined.HealthAndSafety
        Kind.FUN -> Icons.Outlined.Celebration
        Kind.SHOPPING -> Icons.Outlined.ShoppingBag
        Kind.OTHER -> Icons.Outlined.Category
    }

val IncomeIcon: ImageVector get() = Icons.Outlined.Payments

/** The kind's name in the app's language. */
val Kind.title: String get() = t(label)

/** "1 234,56 zł". */
fun money(amount: Long, currency: Currency): String = Money.format(amount, currency, I18n.language.locale)

/** "+10 000,00 zł" for an income, "−3 000,00 zł" for a spending. */
fun signedMoney(amount: Long, income: Boolean, currency: Currency): String =
    (if (income) "+" else "−") + money(amount, currency)

/** "Today, Sat 31 Oct", or "Fri 30 Oct". */
fun dayLabel(day: LocalDate, today: LocalDate): String =
    if (day == today) t("Today") + ", " + I18n.dayShort(day) else I18n.dayShort(day)
