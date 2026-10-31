package com.hubidubicash.core

import java.util.Currency
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    private val pln = Currency.getInstance("PLN")
    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")

    @Test
    fun `amounts parse with a comma or a point and at most two decimals`() {
        assertEquals(4250, Money.parse("42,50"))
        assertEquals(4250, Money.parse("42.5"))
        assertEquals(4200, Money.parse("42"))
        assertEquals(4200, Money.parse("42,"))
        assertEquals(50, Money.parse(",5"))
        assertEquals(123456, Money.parse("1 234,56"))
        assertEquals(123456, Money.parse("1 234,56"))
        assertEquals(123456, Money.parse("1.234,56"))
        assertEquals(123456, Money.parse("1,234.56"))
        assertEquals(100000, Money.parse("1.000"))
        assertEquals(100000000, Money.parse("1,000,000"))
        assertEquals(4250, Money.parse(" +42,50 "))
        for (bad in listOf("", "0", "0,00", "-5", "abc", "4,2,5", "12,3456", "1234,567,89", ".", "1e5"))
            assertNull(Money.parse(bad), bad)
    }

    @Test
    fun `amounts are written the way the currency's country writes them`() {
        assertEquals("1 234,56 zł", Money.format(123456, pln, Locale.ENGLISH))
        assertEquals("$1,234.56", Money.format(123456, usd, Locale.forLanguageTag("pl")))
        assertEquals("1.234,56 €", Money.format(123456, eur, Locale.ENGLISH))
        assertEquals("1 234,56 €", Money.format(123456, eur, Locale.FRENCH))
        assertEquals("+10 000,00", Money.signed(1_000_000, pln, Locale.ENGLISH))
        assertEquals("−3 000,00", Money.signed(-300_000, pln, Locale.ENGLISH))
        assertEquals("zł", Money.symbol(pln, Locale.ENGLISH))
        assertEquals("42,50", Money.editable(4250, pln, Locale.ENGLISH))
        assertEquals("42.50", Money.editable(4250, usd, Locale.ENGLISH))
        assertEquals("42", Money.editable(4200, pln, Locale.ENGLISH))
    }

    @Test
    fun `the default currency is the region's, the euro without one`() {
        assertEquals(pln, Money.defaultCurrency(Locale.forLanguageTag("pl-PL")))
        assertEquals(usd, Money.defaultCurrency(Locale.US))
        assertEquals(eur, Money.defaultCurrency(Locale.ENGLISH))
    }
}
