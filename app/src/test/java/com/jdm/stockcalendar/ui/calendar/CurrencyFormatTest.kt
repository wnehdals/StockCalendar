package com.jdm.stockcalendar.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatTest {
    @Test
    fun `converts a simple dollar amount to won at the fixed rate`() {
        assertEquals("₩570", "${'$'}0.38".toKrwPriceDisplay())
    }

    @Test
    fun `converts a large comma-separated amount and groups the won output`() {
        assertEquals("₩201,061,671,327,000", "${'$'}134,041,114,218".toKrwPriceDisplay())
    }

    @Test
    fun `converts a parenthesized negative amount to a negative won value`() {
        assertEquals("-₩240", "(${'$'}0.16)".toKrwPriceDisplay())
    }

    @Test
    fun `returns N-A for null, blank, or N-A input`() {
        assertEquals("N/A", null.toKrwPriceDisplay())
        assertEquals("N/A", "".toKrwPriceDisplay())
        assertEquals("N/A", "N/A".toKrwPriceDisplay())
    }
}
