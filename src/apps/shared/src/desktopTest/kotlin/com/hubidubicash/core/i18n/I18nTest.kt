package com.hubidubicash.core.i18n

import com.hubidubicash.core.Kind
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nTest {
    @AfterTest fun english() { I18n.language = Language.EN }

    /** Every t("…") in the apps and the shared code, read from the sources themselves. */
    private fun texts(): Set<String> {
        val roots = listOf("src/commonMain/kotlin", "src/androidMain/kotlin", "src/desktopMain/kotlin",
            "../android/src/main/kotlin", "../desktop/src/main/kotlin").map(::File)
        val call = Regex("""(?:(?<![\w.])|(?<=i18n\.))t\("((?:[^"\\]|\\.)*)"""")
        val found = roots.filter { it.isDirectory }
            .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" && "/core/i18n/" !in it.path }.toList() }
            .flatMap { f -> call.findAll(f.readText()).map { it.groupValues[1] }.toList() }.toSet()
        assertTrue(found.size > 60, "the scan found only ${found.size} texts; are the source folders where the test expects?")
        // Labels from lists, translated where they are shown: the kinds, tabs, periods, charts and themes.
        return found + Kind.entries.map { it.label } + LISTED
    }

    @Test fun `every text the apps show has every translation`() {
        val all = texts()
        for (l in Language.entries - Language.EN) {
            val table = I18n.tableFor(l)
            val missing = all.filter { it !in table }
            assertTrue(missing.isEmpty(), "${l.label} misses ${missing.size}: ${missing.take(10)}")
        }
    }

    @Test fun `translations keep the English placeholders, in order`() {
        val spec = Regex("%[sd]")
        for (l in Language.entries - Language.EN) for ((en, tr) in I18n.tableFor(l)) {
            assertEquals(spec.findAll(en).map { it.value }.toList(), spec.findAll(tr).map { it.value }.toList(),
                "${l.label}: \"$tr\" for \"$en\"")
        }
    }

    @Test fun `dates follow the chosen language`() {
        val d = LocalDate.of(2026, 10, 31)
        val expected = mapOf(
            Language.EN to listOf("Saturday, 31 October", "October 2026", "25–31 Oct"),
            Language.PL to listOf("Sobota, 31 października", "Październik 2026", "25–31 paź"),
            Language.DE to listOf("Samstag, 31. Oktober", "Oktober 2026", "25.–31. Okt."),
        )
        for ((l, want) in expected) {
            I18n.language = l
            assertEquals(want[0], I18n.dayLong(d), l.label)
            assertEquals(want[1], I18n.monthYear(YearMonth.of(2026, 10)), l.label)
            assertEquals(want[2], I18n.span(LocalDate.of(2026, 10, 25), d), l.label)
        }
    }

    @Test fun `counts use each language's plural forms`() {
        I18n.language = Language.PL
        assertEquals(listOf("1 wydatek", "2 wydatki", "5 wydatków", "12 wydatków", "22 wydatki"),
            listOf(1, 2, 5, 12, 22).map { I18n.count(it, "spending") })
        I18n.language = Language.EN
        assertEquals(listOf("1 spending", "7 spendings"), listOf(1, 7).map { I18n.count(it, "spending") })
    }

    companion object {
        val LISTED = setOf(
            "Statistics", "Spendings", "Regular", "Bills", "Settings",
            "Today", "The last 7 days", "The last month", "The last year", "Bars", "Circle",
            "Ivory", "Sand", "Paper", "Night", "Forest", "Ocean", "Follows the system", "Light", "Dark",
        )
    }
}
