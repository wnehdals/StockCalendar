package com.jdm.stockcalendar.data.remote

import com.jdm.stockcalendar.data.remote.dto.TossAutoCompleteRequestDto
import com.jdm.stockcalendar.data.remote.dto.TossAutoCompleteResponseDto
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TossInvestApiService {
    @POST("api/v3/search-all/wts-auto-complete")
    suspend fun searchProducts(
        @Body request: TossAutoCompleteRequestDto,
    ): ApiResponse<TossAutoCompleteResponseDto>

    companion object {
        const val BASE_URL = "https://wts-info-api.tossinvest.com/"
    }
}
