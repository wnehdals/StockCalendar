package com.jdm.stockcalendar.data.fakes

import com.jdm.stockcalendar.data.remote.TossInvestApiService
import com.jdm.stockcalendar.data.remote.dto.TossAutoCompleteRequestDto
import com.jdm.stockcalendar.data.remote.dto.TossAutoCompleteResponseDto
import com.jdm.stockcalendar.data.remote.dto.TossProductItemDto
import com.jdm.stockcalendar.data.remote.dto.TossResultSectionDto
import com.jdm.stockcalendar.data.remote.dto.TossSectionDataDto
import com.skydoves.sandwich.ApiResponse
import java.io.IOException

class FakeTossInvestApiService : TossInvestApiService {
    private var response: ApiResponse<TossAutoCompleteResponseDto> = emptyProductResponse()
    val requestedQueries = mutableListOf<String>()

    fun setItems(items: List<TossProductItemDto>) {
        response = ApiResponse.Success(
            TossAutoCompleteResponseDto(
                result = listOf(TossResultSectionDto(type = "PRODUCT", data = TossSectionDataDto(items = items))),
            ),
        )
    }

    fun setFailure(message: String = "boom") {
        response = ApiResponse.Failure.Exception(IOException(message))
    }

    override suspend fun searchProducts(request: TossAutoCompleteRequestDto): ApiResponse<TossAutoCompleteResponseDto> {
        requestedQueries += request.query
        return response
    }

    private companion object {
        fun emptyProductResponse() = ApiResponse.Success(
            TossAutoCompleteResponseDto(
                result = listOf(TossResultSectionDto(type = "PRODUCT", data = TossSectionDataDto(items = emptyList()))),
            ),
        )
    }
}
