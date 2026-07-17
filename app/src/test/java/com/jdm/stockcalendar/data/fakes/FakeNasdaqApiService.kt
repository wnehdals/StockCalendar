package com.jdm.stockcalendar.data.fakes

import com.jdm.stockcalendar.data.remote.NasdaqApiService
import com.jdm.stockcalendar.data.remote.dto.NasdaqEarningsDataDto
import com.jdm.stockcalendar.data.remote.dto.NasdaqEarningsResponseDto
import com.jdm.stockcalendar.data.remote.dto.NasdaqEarningsRowDto
import com.skydoves.sandwich.ApiResponse
import java.io.IOException

class FakeNasdaqApiService : NasdaqApiService {
    private val responses = mutableMapOf<String, ApiResponse<NasdaqEarningsResponseDto>>()
    val requestedDates = mutableListOf<String>()

    fun setRowsForDate(date: String, rows: List<NasdaqEarningsRowDto>) {
        responses[date] = ApiResponse.Success(NasdaqEarningsResponseDto(data = NasdaqEarningsDataDto(rows = rows)))
    }

    fun setFailureForDate(date: String, message: String = "boom") {
        responses[date] = ApiResponse.Failure.Exception(IOException(message))
    }

    override suspend fun getEarningsCalendar(date: String): ApiResponse<NasdaqEarningsResponseDto> {
        requestedDates += date
        return responses[date]
            ?: ApiResponse.Success(NasdaqEarningsResponseDto(data = NasdaqEarningsDataDto(rows = emptyList())))
    }
}

fun sampleRow(symbol: String) = NasdaqEarningsRowDto(
    symbol = symbol,
    name = "$symbol Inc.",
    marketCap = "${'$'}1,000",
    fiscalQuarterEnding = "Jun/2026",
    epsForecast = "${'$'}0.10",
    noOfEsts = "2",
    lastYearRptDt = "7/1/2025",
    lastYearEPS = "${'$'}0.09",
    time = "time-pre-market",
)
