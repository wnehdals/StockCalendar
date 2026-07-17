package com.jdm.stockcalendar.ui.calendar

import com.jdm.stockcalendar.data.EarningsRepository
import com.jdm.stockcalendar.domain.EarningsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeEarningsRepository : EarningsRepository {
    data class SyncCall(val today: LocalDate, val days: Long, val forceRefresh: Boolean)

    private val eventsByDate = MutableStateFlow<Map<LocalDate, List<EarningsEvent>>>(emptyMap())
    val syncCalls = mutableListOf<SyncCall>()
    val upsertedEvents = mutableListOf<EarningsEvent>()
    val deletedEntries = mutableListOf<Pair<String, LocalDate>>()
    var syncResult: Result<Unit> = Result.success(Unit)
    var upsertResult: Result<Unit> = Result.success(Unit)

    fun setEventsForDate(date: LocalDate, events: List<EarningsEvent>) {
        eventsByDate.update { it + (date to events) }
    }

    override fun observeEarningsForDate(date: LocalDate): Flow<List<EarningsEvent>> =
        eventsByDate.map { it[date].orEmpty() }

    override fun observeMarkedDates(startDate: LocalDate, endDate: LocalDate): Flow<Set<LocalDate>> =
        eventsByDate.map { map ->
            map.filterKeys { !it.isBefore(startDate) && !it.isAfter(endDate) }.keys
        }

    override suspend fun syncDate(date: LocalDate): Result<Unit> = syncResult

    override suspend fun syncUpcomingWindow(today: LocalDate, days: Long, forceRefresh: Boolean): Result<Unit> {
        syncCalls += SyncCall(today, days, forceRefresh)
        return syncResult
    }

    override suspend fun upsertUserEntry(event: EarningsEvent): Result<Unit> {
        upsertedEvents += event
        if (upsertResult.isSuccess) {
            eventsByDate.update { current ->
                val existing = current[event.date].orEmpty().filterNot { it.symbol == event.symbol }
                current + (event.date to (existing + event))
            }
        }
        return upsertResult
    }

    override suspend fun deleteEntry(symbol: String, date: LocalDate): Result<Unit> {
        deletedEntries += symbol to date
        eventsByDate.update { current ->
            current + (date to current[date].orEmpty().filterNot { it.symbol == symbol })
        }
        return Result.success(Unit)
    }
}
