package com.hubidubicash.core

import com.hubidubicash.core.i18n.Language
import java.io.File
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsTest {
    private fun file() = File.createTempFile("settings", ".properties").also { it.delete(); it.deleteOnExit() }

    @Test
    fun `a new install starts on the week, bars and 5000 of life costs`() {
        val s = Settings(file()).current
        assertEquals(Period.WEEK, s.period)
        assertEquals(Chart.BARS, s.chart)
        assertEquals(5000_00, s.lifeCosts)
        assertNull(s.theme)
    }

    @Test
    fun `what is set is kept for the next start`() {
        val f = file()
        Settings(f).apply {
            setPeriod(Period.MONTH); setChart(Chart.CIRCLE); setLifeCosts(4200_00)
            setCurrency(Currency.getInstance("PLN")); setLanguage(Language.PL); setTheme("night")
            setWidgetKind(7, Kind.FUN)
        }
        val again = Settings(f)
        assertEquals(SettingsState(Period.MONTH, Chart.CIRCLE, 4200_00, Currency.getInstance("PLN"), Language.PL, "night"),
            again.current)
        assertEquals(Kind.FUN, again.widgetKind(7))
        again.forgetWidget(7)
        assertNull(Settings(f).widgetKind(7))
    }
}
