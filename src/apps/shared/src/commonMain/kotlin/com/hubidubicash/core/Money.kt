package com.hubidubicash.core

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Amounts are whole numbers of hundredths ([SCALE]) whatever the currency, so
 * changing the currency in Settings changes only how they are shown. They are
 * shown the way the currency's own country writes them, "1 234,56 zł",
 * "$1,234.56", so a sum looks as it does on the bank's page.
 */
object Money {
    const val SCALE = 100
    /** 999 999 999,99: far beyond any spending, far below overflow. */
    const val MAX = 99_999_999_999L

    /** The currencies offered in Settings; the device's own is added when it is not one of them. */
    val COMMON = listOf("PLN", "EUR", "USD", "GBP", "CHF", "CZK", "SEK", "NOK", "DKK", "HUF", "RON", "UAH",
        "CAD", "AUD", "JPY")

    /** Where each currency is at home; a currency of several countries is written as the app's language writes it. */
    private val HOME = mapOf(
        "PLN" to "pl-PL", "USD" to "en-US", "GBP" to "en-GB", "CHF" to "de-CH", "CZK" to "cs-CZ", "SEK" to "sv-SE",
        "NOK" to "nb-NO", "DKK" to "da-DK", "HUF" to "hu-HU", "RON" to "ro-RO", "UAH" to "uk-UA", "CAD" to "en-CA",
        "AUD" to "en-AU", "JPY" to "ja-JP",
    )

    /** The locale [currency] is written in, given the app's [language]. */
    fun locale(currency: Currency, language: Locale): Locale {
        HOME[currency.currencyCode]?.let { return Locale.forLanguageTag(it) }
        if (currency.currencyCode == "EUR") return when (language.language) {
            "de" -> Locale.GERMANY
            "fr" -> Locale.FRANCE
            "es" -> Locale.forLanguageTag("es-ES")
            else -> Locale.forLanguageTag("de-DE")
        }
        return language
    }

    /** The device region's currency; the euro when the region has none or is unknown. */
    fun defaultCurrency(region: Locale = Locale.getDefault()): Currency =
        runCatching { Currency.getInstance(region) }.getOrNull() ?: Currency.getInstance("EUR")

    /** "1 234,56 zł". */
    fun format(amount: Long, currency: Currency, language: Locale): String {
        val f = NumberFormat.getCurrencyInstance(locale(currency, language))
        f.currency = currency
        return f.format(value(amount))
    }

    /** "1 234,56": a number in the currency's own way, for a column headed by the currency. */
    fun number(amount: Long, currency: Currency, language: Locale): String {
        val f = NumberFormat.getNumberInstance(locale(currency, language))
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)
        f.minimumFractionDigits = digits
        f.maximumFractionDigits = digits
        return f.format(value(amount))
    }

    /** "+10 000,00" or "−3 000,00", with a true minus sign. */
    fun signed(amount: Long, currency: Currency, language: Locale): String =
        (if (amount < 0) "−" else "+") + number(kotlin.math.abs(amount), currency, language)

    /** The sign written alone, "zł", "€", "$". */
    fun symbol(currency: Currency, language: Locale): String = currency.getSymbol(locale(currency, language))

    /** The amount as a person types it back in to change it: "42,50" or "42.50", no grouping. */
    fun editable(amount: Long, currency: Currency, language: Locale): String {
        val sep = (NumberFormat.getNumberInstance(locale(currency, language)) as? DecimalFormat)
            ?.decimalFormatSymbols?.decimalSeparator ?: '.'
        val whole = amount / SCALE
        val cents = amount % SCALE
        return if (cents == 0L) whole.toString() else "$whole$sep" + "%02d".format(cents)
    }

    /**
     * What a person typed, as hundredths: "42", "42,5", "42.50", "1 234,56",
     * "1.234,56", "1,234.56". A separator followed by one or two digits at the
     * end is the decimal one; any other separates thousands. Null for anything
     * else, for zero and below, and for more than [MAX].
     */
    fun parse(text: String): Long? {
        val s = text.filterNot { it.isWhitespace() || it in "\u00a0\u202f'\u2019" }.removePrefix("+")
            .let { if (it.endsWith(',') || it.endsWith('.')) it.dropLast(1) else it }
        if (s.isEmpty() || !s.all { it.isDigit() || it == ',' || it == '.' }) return null
        val lastSep = s.indexOfLast { it == ',' || it == '.' }
        val decimals = if (lastSep >= 0) s.length - lastSep - 1 else 0
        val (whole, fraction) =
            if (lastSep >= 0 && decimals in 1..2) s.substring(0, lastSep) to s.substring(lastSep + 1) else s to ""
        // What is left of the whole part: digits, with any grouping in threes.
        val groups = whole.split(',', '.')
        if (groups.first().isEmpty() && groups.size > 1) return null
        if (groups.size > 1 && (groups.first().length > 3 || groups.drop(1).any { it.length != 3 })) return null
        val digits = groups.joinToString("")
        if (digits.isEmpty() && fraction.isEmpty()) return null
        val value = BigDecimal((digits.ifEmpty { "0" }) + "." + fraction.padEnd(2, '0'))
            .movePointRight(2).longValueExact()
        return value.takeIf { it in 1..MAX }
    }

    private fun value(amount: Long): BigDecimal = BigDecimal.valueOf(amount, 2)
}
