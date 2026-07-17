package com.jdm.stockcalendar.data.fakes

import com.jdm.stockcalendar.data.local.EarningsDao
import com.jdm.stockcalendar.data.local.EarningsEntity
import com.jdm.stockcalendar.data.local.SyncStatusEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeEarningsDao : EarningsDao {
    private val earnings = MutableStateFlow<List<EarningsEntity>>(emptyList())
    private val syncStatuses = mutableMapOf<String, Long>()

    override fun observeByDate(date: String): Flow<List<EarningsEntity>> =
        earnings.map { list -> list.filter { it.date == date } }

    override fun observeMarkedDates(startDate: String, endDate: String): Flow<List<String>> =
        earnings.map { list -> list.filter { it.date in startDate..endDate }.map { it.date }.distinct() }

    override suspend fun upsertAll(entities: List<EarningsEntity>) {
        earnings.update { current ->
            val keyed = current.associateBy { it.symbol to it.date }.toMutableMap()
            entities.forEach { keyed[it.symbol to it.date] = it }
            keyed.values.toList()
        }
    }

    override suspend fun deleteApiEntriesForDate(date: String) {
        earnings.update { list -> list.filterNot { it.date == date && !it.isUserAdded } }
    }

    override suspend fun deleteEntry(symbol: String, date: String) {
        earnings.update { list -> list.filterNot { it.symbol == symbol && it.date == date } }
    }

    override suspend fun upsertSyncStatus(status: SyncStatusEntity) {
        syncStatuses[status.date] = status.syncedAtEpochDay
    }

    override suspend fun getFreshDates(todayEpochDay: Long, dates: List<String>): List<String> =
        dates.filter { syncStatuses[it] == todayEpochDay }
}
