package com.jdm.stockcalendar.data.remote

import com.jdm.stockcalendar.data.remote.dto.NasdaqEarningsResponseDto
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NasdaqApiService {
    @GET("api/calendar/earnings")
    suspend fun getEarningsCalendar(
        @Query("date") date: String,
    ): ApiResponse<NasdaqEarningsResponseDto>

    companion object {
        const val BASE_URL = "https://api.nasdaq.com/"
    }
}
