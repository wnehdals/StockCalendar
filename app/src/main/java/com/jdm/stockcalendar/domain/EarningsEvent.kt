package com.jdm.stockcalendar.domain

import java.time.LocalDate

data class EarningsEvent(
    val symbol: String,
    val name: String,
    val date: LocalDate,
    val marketCap: String?,
    val fiscalQuarterEnding: String?,
    val epsForecast: String?,
    val numberOfEstimates: String?,
    val lastYearReportDate: String?,
    val lastYearEps: String?,
    val time: EarningsTime,
    val isUserAdded: Boolean = false,
)

enum class EarningsTime {
    PRE_MARKET,
    POST_MARKET,
    NOT_SUPPLIED,
    UNKNOWN,
    ;

    companion object {
        fun fromRaw(raw: String?): EarningsTime = when (raw) {
            "time-pre-market" -> PRE_MARKET
            "time-post-market" -> POST_MARKET
            "time-not-supplied" -> NOT_SUPPLIED
            null -> UNKNOWN
            else -> UNKNOWN
        }
    }
}

fun EarningsTime.toRaw(): String = when (this) {
    EarningsTime.PRE_MARKET -> "time-pre-market"
    EarningsTime.POST_MARKET -> "time-post-market"
    EarningsTime.NOT_SUPPLIED -> "time-not-supplied"
    EarningsTime.UNKNOWN -> ""
}
