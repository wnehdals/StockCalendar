package com.jdm.stockcalendar.data

import com.jdm.stockcalendar.data.fakes.FakeTossInvestApiService
import com.jdm.stockcalendar.data.remote.dto.TossProductItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TossInvestRepositoryTest {

    private fun item(symbol: String, productCode: String) = TossProductItemDto(productCode = productCode, symbol = symbol)

    @Test
    fun `picks the exact symbol match among fuzzy results and builds the analytics url`() = runTest {
        val api = FakeTossInvestApiService()
        api.setItems(
            listOf(
                item("AAPL", "US19801212001"),
                item("APLY", "AMX0230418001"),
                item("AAPW", "AMX0250219001"),
            ),
        )
        val repository = DefaultTossInvestRepository(api, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.findAnalyticsUrl("aapl")

        assertEquals("https://www.tossinvest.com/stocks/US19801212001/analytics", result.getOrThrow())
        assertEquals(listOf("aapl"), api.requestedQueries)
    }

    @Test
    fun `falls back to the first result when no item matches the symbol exactly`() = runTest {
        val api = FakeTossInvestApiService()
        api.setItems(listOf(item("ALVO", "US20220616002")))
        val repository = DefaultTossInvestRepository(api, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.findAnalyticsUrl("alv")

        assertEquals("https://www.tossinvest.com/stocks/US20220616002/analytics", result.getOrThrow())
    }

    @Test
    fun `fails when no items are returned at all`() = runTest {
        val api = FakeTossInvestApiService()
        api.setItems(emptyList())
        val repository = DefaultTossInvestRepository(api, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.findAnalyticsUrl("zzzz")

        assertTrue(result.isFailure)
    }

    @Test
    fun `propagates API failures`() = runTest {
        val api = FakeTossInvestApiService()
        api.setFailure("network down")
        val repository = DefaultTossInvestRepository(api, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.findAnalyticsUrl("aapl")

        assertTrue(result.isFailure)
    }
}
