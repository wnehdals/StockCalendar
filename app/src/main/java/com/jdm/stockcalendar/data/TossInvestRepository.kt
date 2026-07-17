package com.jdm.stockcalendar.data

import com.jdm.stockcalendar.data.remote.TossInvestApiService
import com.jdm.stockcalendar.data.remote.dto.TossAutoCompleteRequestDto
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val PRODUCT_SECTION_TYPE = "PRODUCT"

interface TossInvestRepository {
    suspend fun findAnalyticsUrl(symbol: String): Result<String>
}

class DefaultTossInvestRepository(
    private val apiService: TossInvestApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TossInvestRepository {

    override suspend fun findAnalyticsUrl(symbol: String): Result<String> = withContext(ioDispatcher) {
        when (val response = apiService.searchProducts(TossAutoCompleteRequestDto(query = symbol))) {
            is ApiResponse.Success -> {
                val items = response.data.result
                    ?.firstOrNull { it.type == PRODUCT_SECTION_TYPE }
                    ?.data?.items
                    .orEmpty()
                val match = items.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: items.firstOrNull()
                match?.let { Result.success("https://www.tossinvest.com/stocks/${it.productCode}/analytics") }
                    ?: Result.failure(NoSuchElementException("No Toss Invest product found for symbol $symbol"))
            }
            is ApiResponse.Failure.Error -> Result.failure(IOException("Toss Invest API error: ${response.payload}"))
            is ApiResponse.Failure.Exception -> Result.failure(response.throwable)
        }
    }
}
