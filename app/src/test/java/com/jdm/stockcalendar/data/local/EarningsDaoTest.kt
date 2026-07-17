package com.jdm.stockcalendar.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EarningsDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: EarningsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.earningsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(symbol: String, date: String, isUserAdded: Boolean = false) = EarningsEntity(
        symbol = symbol,
        date = date,
        name = "$symbol Inc.",
        marketCap = "${'$'}1,000",
        fiscalQuarterEnding = "Jun/2026",
        epsForecast = "${'$'}0.10",
        numberOfEstimates = "2",
        lastYearReportDate = "7/1/2025",
        lastYearEps = "${'$'}0.09",
        time = "time-pre-market",
        isUserAdded = isUserAdded,
    )

    @Test
    fun `upsertAll then observeByDate returns inserted rows`() = runTest {
        dao.upsertAll(listOf(entity("AAA", "2026-07-17"), entity("BBB", "2026-07-18")))

        val rows = dao.observeByDate("2026-07-17").first()

        assertEquals(1, rows.size)
        assertEquals("AAA", rows[0].symbol)
    }

    @Test
    fun `observeMarkedDates returns distinct dates within range`() = runTest {
        dao.upsertAll(
            listOf(
                entity("AAA", "2026-07-17"),
                entity("BBB", "2026-07-17"),
                entity("CCC", "2026-07-20"),
                entity("DDD", "2026-08-01"),
            ),
        )

        val marked = dao.observeMarkedDates("2026-07-17", "2026-07-31").first()

        assertEquals(setOf("2026-07-17", "2026-07-20"), marked.toSet())
    }

    @Test
    fun `getFreshDates returns only dates synced today`() = runTest {
        val today = 20000L
        dao.upsertSyncStatus(SyncStatusEntity(date = "2026-07-17", syncedAtEpochDay = today))
        dao.upsertSyncStatus(SyncStatusEntity(date = "2026-07-18", syncedAtEpochDay = today - 1))

        val fresh = dao.getFreshDates(today, listOf("2026-07-17", "2026-07-18", "2026-07-19"))

        assertEquals(listOf("2026-07-17"), fresh)
    }

    @Test
    fun `deleteApiEntriesForDate removes only that date's rows`() = runTest {
        dao.upsertAll(listOf(entity("AAA", "2026-07-17"), entity("BBB", "2026-07-18")))

        dao.deleteApiEntriesForDate("2026-07-17")

        assertTrue(dao.observeByDate("2026-07-17").first().isEmpty())
        assertEquals(1, dao.observeByDate("2026-07-18").first().size)
    }

    @Test
    fun `deleteApiEntriesForDate preserves user-added rows for the same date`() = runTest {
        dao.upsertAll(
            listOf(
                entity("AAA", "2026-07-17", isUserAdded = false),
                entity("BBB", "2026-07-17", isUserAdded = true),
            ),
        )

        dao.deleteApiEntriesForDate("2026-07-17")

        val remaining = dao.observeByDate("2026-07-17").first()
        assertEquals(listOf("BBB"), remaining.map { it.symbol })
    }

    @Test
    fun `deleteEntry removes exactly the given symbol and date`() = runTest {
        dao.upsertAll(listOf(entity("AAA", "2026-07-17"), entity("BBB", "2026-07-17")))

        dao.deleteEntry("AAA", "2026-07-17")

        val remaining = dao.observeByDate("2026-07-17").first()
        assertEquals(listOf("BBB"), remaining.map { it.symbol })
    }

    @Test
    fun `replaceDate keeps user-added rows while swapping in fresh API rows`() = runTest {
        dao.upsertAll(listOf(entity("USER", "2026-07-17", isUserAdded = true)))

        dao.replaceDate(
            date = "2026-07-17",
            entities = listOf(entity("API", "2026-07-17")),
            status = SyncStatusEntity(date = "2026-07-17", syncedAtEpochDay = 1L),
        )

        val remaining = dao.observeByDate("2026-07-17").first().map { it.symbol }.toSet()
        assertEquals(setOf("USER", "API"), remaining)
    }
}
