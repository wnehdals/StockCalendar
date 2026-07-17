package com.jdm.stockcalendar.data

import com.jdm.stockcalendar.data.local.EarningsDao
import com.jdm.stockcalendar.data.local.SyncStatusEntity
import com.jdm.stockcalendar.data.remote.NasdaqApiService
import com.jdm.stockcalendar.data.remote.dto.NasdaqEarningsRowDto
import com.jdm.stockcalendar.domain.EarningsEvent
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private const val MAX_CONCURRENT_SYNCS = 4
private const val DEFAULT_WINDOW_DAYS = 14L

/** Nasdaq's earnings calendar and trading-day boundary are defined in the exchange's local time, not the device's. */
val NASDAQ_ZONE: ZoneId = ZoneId.of("America/New_York")

interface EarningsRepository {
    fun observeEarningsForDate(date: LocalDate): Flow<List<EarningsEvent>>
    fun observeMarkedDates(startDate: LocalDate, endDate: LocalDate): Flow<Set<LocalDate>>
    suspend fun syncDate(date: LocalDate): Result<Unit>
    suspend fun syncUpcomingWindow(
        today: LocalDate = LocalDate.now(NASDAQ_ZONE),
        days: Long = DEFAULT_WINDOW_DAYS,
        forceRefresh: Boolean = false,
    ): Result<Unit>
    suspend fun upsertUserEntry(event: EarningsEvent): Result<Unit>
    suspend fun deleteEntry(symbol: String, date: LocalDate): Result<Unit>
}

class DefaultEarningsRepository(
    private val dao: EarningsDao,
    private val apiService: NasdaqApiService,
    private val clock: Clock = Clock.system(NASDAQ_ZONE),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EarningsRepository {

    override fun observeEarningsForDate(date: LocalDate): Flow<List<EarningsEvent>> =
        dao.observeByDate(date.toString()).map { entities -> entities.map { it.toDomain() } }

    override fun observeMarkedDates(startDate: LocalDate, endDate: LocalDate): Flow<Set<LocalDate>> =
        dao.observeMarkedDates(startDate.toString(), endDate.toString())
            .map { dates -> dates.map(LocalDate::parse).toSet() }

    override suspend fun syncDate(date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        when (val response = apiService.getEarningsCalendar(date.toString())) {
            is ApiResponse.Success -> persistRows(date, response.data.data?.rows.orEmpty())
            is ApiResponse.Failure.Error -> Result.failure(IOException("Nasdaq API error: ${response.payload}"))
            is ApiResponse.Failure.Exception -> Result.failure(response.throwable)
        }
    }

    override suspend fun upsertUserEntry(event: EarningsEvent): Result<Unit> = withContext(ioDispatcher) {
        dbCatching { dao.upsertAll(listOf(event.toEntity())) }
    }

    override suspend fun deleteEntry(symbol: String, date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        dbCatching { dao.deleteEntry(symbol, date.toString()) }
    }

    private suspend fun persistRows(date: LocalDate, rows: List<NasdaqEarningsRowDto>): Result<Unit> = dbCatching {
        dao.replaceDate(
            date = date.toString(),
            entities = rows.filter { it.symbol.isNotBlank() }.map { it.toEntity(date) },
            status = SyncStatusEntity(date.toString(), LocalDate.now(clock).toEpochDay()),
        )
    }

    private suspend fun dbCatching(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncUpcomingWindow(today: LocalDate, days: Long, forceRefresh: Boolean): Result<Unit> {
        val windowDates = (0..days).map { today.plusDays(it) }

        val datesToSync = if (forceRefresh) {
            windowDates
        } else {
            val freshDates = dao.getFreshDates(
                today.toEpochDay(),
                windowDates.map { it.toString() },
            ).toSet()
            windowDates.filterNot { it.toString() in freshDates }
        }

        if (datesToSync.isEmpty()) return Result.success(Unit)

        val semaphore = Semaphore(MAX_CONCURRENT_SYNCS)
        val results = coroutineScope {
            datesToSync.map { date ->
                async { semaphore.withPermit { syncDate(date) } }
            }.map { it.await() }
        }

        return if (results.all { it.isFailure }) {
            results.first()
        } else {
            Result.success(Unit)
        }
    }
}
