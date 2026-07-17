package com.jdm.stockcalendar.data.remote

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val SAMPLE_RESPONSE_BODY = """
{
  "data": {
    "asOf": "Fri, Jul 17, 2026",
    "rows": [
      {
        "lastYearRptDt": "7/19/2025",
        "lastYearEPS": "${'$'}0.37",
        "time": "time-not-supplied",
        "symbol": "HDB",
        "name": "HDFC Bank Limited",
        "marketCap": "${'$'}134,041,114,218",
        "fiscalQuarterEnding": "Jun/2026",
        "epsForecast": "${'$'}0.38",
        "noOfEsts": "2"
      },
      {
        "lastYearRptDt": "N/A",
        "lastYearEPS": "${'$'}0.65",
        "time": "time-not-supplied",
        "symbol": "MCBS",
        "name": "MetroCity Bankshares, Inc.",
        "marketCap": "${'$'}1,026,889,305",
        "fiscalQuarterEnding": "Jun/2026",
        "epsForecast": "",
        "noOfEsts": "2"
      }
    ]
  },
  "message": null,
  "status": { "rCode": 200, "bCodeMessage": null, "developerMessage": null }
}
"""

class NasdaqApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: NasdaqApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true }
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .build()
        service = retrofit.create(NasdaqApiService::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `getEarningsCalendar parses rows including empty and N-A fields`() = runTest {
        server.enqueue(MockResponse(body = SAMPLE_RESPONSE_BODY))

        val result = service.getEarningsCalendar("2026-07-17")

        check(result is ApiResponse.Success<*>) { "expected Success but was $result" }
        val body = (result as ApiResponse.Success).data
        val rows = requireNotNull(body.data?.rows)
        assertEquals(2, rows.size)
        assertEquals("HDB", rows[0].symbol)
        assertEquals("${'$'}0.38", rows[0].epsForecast)
        assertEquals("", rows[1].epsForecast)
        assertEquals("N/A", rows[1].lastYearRptDt)
    }

    @Test
    fun `request includes a browser User-Agent and the date query param`() = runTest {
        server.enqueue(MockResponse(body = SAMPLE_RESPONSE_BODY))

        service.getEarningsCalendar("2026-07-17")

        val recorded = server.takeRequest()
        assertTrue(recorded.headers["User-Agent"]?.contains("Mozilla") == true)
        assertEquals("/api/calendar/earnings?date=2026-07-17", recorded.target)
    }
}
