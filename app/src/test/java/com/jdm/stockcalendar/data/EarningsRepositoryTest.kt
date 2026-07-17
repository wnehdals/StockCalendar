package com.jdm.stockcalendar.data

import com.jdm.stockcalendar.data.fakes.FakeEarningsDao
import com.jdm.stockcalendar.data.fakes.FakeNasdaqApiService
import com.jdm.stockcalendar.data.fakes.sampleRow
import com.jdm.stockcalendar.data.local.SyncStatusEntity
import com.jdm.stockcalendar.domain.EarningsEvent
import com.jdm.stockcalendar.domain.EarningsTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class EarningsRepositoryTest {
    private val today = LocalDate.of(2026, 7, 17)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private lateinit var dao: FakeEarningsDao
    private lateinit var api: FakeNasdaqApiService
    private lateinit var repository: DefaultEarningsRepository

    @Before
    fun setUp() {
        dao = FakeEarningsDao()
        api = FakeNasdaqApiService()
        repository = DefaultEarningsRepository(
            dao = dao,
            apiService = api,
            clock = clock,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `syncDate success upserts rows and stamps sync status`() = runTest {
        api.setRowsForDate("2026-07-17", listOf(sampleRow("AAA")))

        val result = repository.syncDate(LocalDate.parse("2026-07-17"))

        assertTrue(result.isSuccess)
        val events = repository.observeEarningsForDate(LocalDate.parse("2026-07-17")).first()
        assertEquals(listOf("AAA"), events.map { it.symbol })
    }

    @Test
    fun `syncUpcomingWindow requests all 15 dates when nothing cached`() = runTest {
        repository.syncUpcomingWindow(today = today, days = 14)

        assertEquals(15, api.requestedDates.size)
        assertEquals(today.toString(), api.requestedDates.min())
        assertEquals(today.plusDays(14).toString(), api.requestedDates.max())
    }

    @Test
    fun `syncUpcomingWindow skips dates already synced today`() = runTest {
        dao.upsertSyncStatus(SyncStatusEntity(today.toString(), today.toEpochDay()))
        dao.upsertSyncStatus(SyncStatusEntity(today.plusDays(1).toString(), today.toEpochDay()))

        repository.syncUpcomingWindow(today = today, days = 14)

        assertTrue(today.toString() !in api.requestedDates)
        assertTrue(today.plusDays(1).toString() !in api.requestedDates)
        assertEquals(13, api.requestedDates.size)
    }

    @Test
    fun `forceRefresh bypasses the freshness skip`() = runTest {
        dao.upsertSyncStatus(SyncStatusEntity(today.toString(), today.toEpochDay()))

        repository.syncUpcomingWindow(today = today, days = 14, forceRefresh = true)

        assertEquals(15, api.requestedDates.size)
        assertTrue(today.toString() in api.requestedDates)
    }

    @Test
    fun `partial failure still succeeds overall and keeps the successful dates' data`() = runTest {
        api.setFailureForDate(today.toString())
        api.setRowsForDate(today.plusDays(1).toString(), listOf(sampleRow("BBB")))

        val result = repository.syncUpcomingWindow(today = today, days = 1)

        assertTrue(result.isSuccess)
        val events = repository.observeEarningsForDate(today.plusDays(1)).first()
        assertEquals(listOf("BBB"), events.map { it.symbol })
    }

    @Test
    fun `all dates failing returns a failure result`() = runTest {
        api.setFailureForDate(today.toString())
        api.setFailureForDate(today.plusDays(1).toString())

        val result = repository.syncUpcomingWindow(today = today, days = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `upsertUserEntry persists a user-added event and survives a re-sync of the same date`() = runTest {
        val userEvent = EarningsEvent(
            symbol = "USER",
            name = "User Added Co.",
            date = today,
            marketCap = null,
            fiscalQuarterEnding = null,
            epsForecast = "${'$'}1.00",
            numberOfEstimates = null,
            lastYearReportDate = null,
            lastYearEps = null,
            time = EarningsTime.PRE_MARKET,
            isUserAdded = true,
        )

        val result = repository.upsertUserEntry(userEvent)
        assertTrue(result.isSuccess)

        api.setRowsForDate(today.toString(), listOf(sampleRow("API")))
        repository.syncDate(today)

        val symbols = repository.observeEarningsForDate(today).first().map { it.symbol }.toSet()
        assertEquals(setOf("USER", "API"), symbols)
    }

    @Test
    fun `deleteEntry removes the given symbol and date`() = runTest {
        api.setRowsForDate(today.toString(), listOf(sampleRow("AAA"), sampleRow("BBB")))
        repository.syncDate(today)

        val result = repository.deleteEntry("AAA", today)

        assertTrue(result.isSuccess)
        val symbols = repository.observeEarningsForDate(today).first().map { it.symbol }
        assertEquals(listOf("BBB"), symbols)
    }
}
