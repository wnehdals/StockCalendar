package com.jdm.stockcalendar.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NasdaqEarningsResponseDto(
    val data: NasdaqEarningsDataDto? = null,
    val message: String? = null,
)

@Serializable
data class NasdaqEarningsDataDto(
    val asOf: String? = null,
    val rows: List<NasdaqEarningsRowDto>? = null,
)

@Serializable
data class NasdaqEarningsRowDto(
    val symbol: String = "",
    val name: String = "",
    val marketCap: String? = null,
    val fiscalQuarterEnding: String? = null,
    val epsForecast: String? = null,
    val noOfEsts: String? = null,
    val lastYearRptDt: String? = null,
    val lastYearEPS: String? = null,
    val time: String? = null,
)
