package com.jdm.stockcalendar.ui.calendar

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private const val USD_TO_KRW_RATE = 1500.0
private val nonNumericChars = Regex("[^0-9.]")

/**
 * Nasdaq's API returns pre-formatted USD strings (e.g. "${'$'}0.38", "${'$'}134,041,114,218",
 * "(${'$'}0.16)" for negatives). Converts to a KRW display string at a fixed 1500 rate.
 */
fun String?.toKrwPriceDisplay(): String {
    val usd = this.parseUsdAmountOrNull() ?: return "N/A"
    val won = usd * USD_TO_KRW_RATE
    val formatted = NumberFormat.getIntegerInstance(Locale.KOREA).format(abs(won).toLong())
    return if (won < 0) "-₩$formatted" else "₩$formatted"
}

private fun String?.parseUsdAmountOrNull(): Double? {
    if (this.isNullOrBlank() || this == "N/A") return null
    val isNegative = startsWith("(") || startsWith("-")
    val digitsOnly = replace(nonNumericChars, "")
    return digitsOnly.toDoubleOrNull()?.let { if (isNegative) -it else it }
}
