package com.hubidubicash.core.i18n

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The languages the apps speak, each named in itself for the Settings list. */
enum class Language(val code: String, val label: String, val locale: Locale) {
    EN("en", "English", Locale.ENGLISH),
    PL("pl", "Polski", Locale.forLanguageTag("pl")),
    DE("de", "Deutsch", Locale.GERMAN),
    FR("fr", "Français", Locale.FRENCH),
    ES("es", "Español", Locale.forLanguageTag("es"));

    companion object {
        fun of(code: String?): Language? = entries.firstOrNull { it.code == code }
        /** The system's language when the apps speak it, else English. */
        fun system(): Language = of(Locale.getDefault().language) ?: EN
    }
}

/**
 * Everything the apps say to a person, in the language chosen in Settings.
 *
 * The English text is the key: `t("Bills")`, `t("Day %d", 1)`. Each other
 * language has a table from that English to its own words (Polish.kt and the
 * rest); a text missing from a table falls back to English, and a test fails
 * the build when any `t("…")` in the apps has no entry. Dates and counts go
 * through [dayLong], [dayShort], [count] and the like, which know each
 * language's word order and plural forms.
 */
object I18n {
    @Volatile var language: Language = Language.EN

    private fun table(l: Language): Map<String, String> = when (l) {
        Language.EN -> emptyMap()
        Language.PL -> POLISH
        Language.DE -> GERMAN
        Language.FR -> FRENCH
        Language.ES -> SPANISH
    }

    /** All the English texts the apps translate, by language: for the completeness test. */
    fun tableFor(l: Language): Map<String, String> = table(l)

    fun t(en: String, vararg args: Any?): String {
        val s = table(language)[en] ?: en
        return if (args.isEmpty()) s else String.format(language.locale, s, *args)
    }

    // ---- dates, each language's way

    private fun fmt(pattern: String) = DateTimeFormatter.ofPattern(pattern, language.locale)
    private fun cap(s: String) = s.replaceFirstChar { it.titlecase(language.locale) }

    /** "Saturday, 31 October", "Sobota, 31 października", "Samstag, 31. Oktober". */
    fun dayLong(d: LocalDate): String = cap(d.format(fmt(when (language) {
        Language.DE -> "EEEE, d. MMMM"
        Language.FR -> "EEEE d MMMM"
        Language.ES -> "EEEE, d 'de' MMMM"
        else -> "EEEE, d MMMM"
    })))

    /** "Sat 31 Oct": a day's heading in a list. */
    fun dayShort(d: LocalDate): String = cap(d.format(fmt(when (language) {
        Language.DE -> "EEE, d. MMM"
        else -> "EEE d MMM"
    })))

    /** "31 Oct". */
    fun dayMonth(d: LocalDate): String = d.format(fmt(if (language == Language.DE) "d. MMM" else "d MMM"))

    /** "31 October". */
    fun dayMonthLong(d: LocalDate): String = d.format(fmt(when (language) {
        Language.DE -> "d. MMMM"
        Language.ES -> "d 'de' MMMM"
        else -> "d MMMM"
    }))

    /** "October 2026", "Październik 2026": a month on its own. */
    fun monthYear(m: YearMonth): String = cap(m.atDay(1).format(fmt("LLLL yyyy")))

    /** "25–31 Oct", or "26 Sep – 2 Oct" across a month, "1 Nov 2025 – 31 Oct 2026" across a year. */
    fun span(start: LocalDate, end: LocalDate): String = when {
        start == end -> dayMonth(end)
        start.month == end.month && start.year == end.year ->
            (if (language == Language.DE) "${start.dayOfMonth}.–" else "${start.dayOfMonth}–") + dayMonth(end)
        start.year == end.year -> "${dayMonth(start)} – ${dayMonth(end)}"
        else -> "${dayMonth(start)} ${start.year} – ${dayMonth(end)} ${end.year}"
    }

    // ---- counts

    /** "1 spending", "3 spendings", "5 wydatków": [n] with the word in its plural form. */
    fun count(n: Int, word: String): String {
        val forms = PLURALS[language]?.get(word) ?: PLURALS.getValue(Language.EN).getValue(word)
        val i = when (language) {
            Language.PL -> when {
                n == 1 -> 0
                n % 10 in 2..4 && n % 100 !in 12..14 -> 1
                else -> 2
            }
            Language.FR -> if (n == 0 || n == 1) 0 else 1
            else -> if (n == 1) 0 else 1
        }
        return "$n ${forms[minOf(i, forms.lastIndex)]}"
    }

    /** One form for 1, then the rest; Polish has a third for 5 and up (and 12–14). */
    private val PLURALS: Map<Language, Map<String, List<String>>> = mapOf(
        Language.EN to mapOf("spending" to listOf("spending", "spendings")),
        Language.PL to mapOf("spending" to listOf("wydatek", "wydatki", "wydatków")),
        Language.DE to mapOf("spending" to listOf("Ausgabe", "Ausgaben")),
        Language.FR to mapOf("spending" to listOf("dépense", "dépenses")),
        Language.ES to mapOf("spending" to listOf("gasto", "gastos")),
    )
}

/** `t("…")` without the object's name, for the apps' screens. */
fun t(en: String, vararg args: Any?): String = I18n.t(en, *args)
