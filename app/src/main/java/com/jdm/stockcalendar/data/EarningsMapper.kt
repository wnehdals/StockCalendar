package com.jdm.stockcalendar.data

import com.jdm.stockcalendar.data.local.EarningsEntity
import com.jdm.stockcalendar.data.remote.dto.NasdaqEarningsRowDto
import com.jdm.stockcalendar.domain.EarningsEvent
import com.jdm.stockcalendar.domain.EarningsTime
import com.jdm.stockcalendar.domain.toRaw
import java.time.LocalDate

fun NasdaqEarningsRowDto.toEntity(date: LocalDate): EarningsEntity = EarningsEntity(
    symbol = symbol,
    date = date.toString(),
    name = name,
    marketCap = marketCap,
    fiscalQuarterEnding = fiscalQuarterEnding,
    epsForecast = epsForecast,
    numberOfEstimates = noOfEsts,
    lastYearReportDate = lastYearRptDt,
    lastYearEps = lastYearEPS,
    time = time.orEmpty(),
)

fun EarningsEntity.toDomain(): EarningsEvent = EarningsEvent(
    symbol = symbol,
    name = name,
    date = LocalDate.parse(date),
    marketCap = marketCap,
    fiscalQuarterEnding = fiscalQuarterEnding,
    epsForecast = epsForecast,
    numberOfEstimates = numberOfEstimates,
    lastYearReportDate = lastYearReportDate,
    lastYearEps = lastYearEps,
    time = EarningsTime.fromRaw(time),
    isUserAdded = isUserAdded,
)

/** Saving (add or edit) always claims the row as user-owned, so future API re-syncs won't overwrite it. */
fun EarningsEvent.toEntity(): EarningsEntity = EarningsEntity(
    symbol = symbol,
    date = date.toString(),
    name = name,
    marketCap = marketCap,
    fiscalQuarterEnding = fiscalQuarterEnding,
    epsForecast = epsForecast,
    numberOfEstimates = numberOfEstimates,
    lastYearReportDate = lastYearReportDate,
    lastYearEps = lastYearEps,
    time = time.toRaw(),
    isUserAdded = true,
)
