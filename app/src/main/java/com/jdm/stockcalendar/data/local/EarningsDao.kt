package com.jdm.stockcalendar.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EarningsDao {
    @Query("SELECT * FROM earnings WHERE date = :date ORDER BY symbol")
    fun observeByDate(date: String): Flow<List<EarningsEntity>>

    @Query("SELECT DISTINCT date FROM earnings WHERE date BETWEEN :startDate AND :endDate")
    fun observeMarkedDates(startDate: String, endDate: String): Flow<List<String>>

    @Upsert
    suspend fun upsertAll(entities: List<EarningsEntity>)

    @Query("DELETE FROM earnings WHERE date = :date AND isUserAdded = 0")
    suspend fun deleteApiEntriesForDate(date: String)

    @Query("DELETE FROM earnings WHERE symbol = :symbol AND date = :date")
    suspend fun deleteEntry(symbol: String, date: String)

    @Upsert
    suspend fun upsertSyncStatus(status: SyncStatusEntity)

    @Query("SELECT date FROM sync_status WHERE syncedAtEpochDay = :todayEpochDay AND date IN (:dates)")
    suspend fun getFreshDates(todayEpochDay: Long, dates: List<String>): List<String>

    /** Only replaces API-sourced rows for [date] — user-added/edited rows (isUserAdded = 1) survive re-syncs. */
    @Transaction
    suspend fun replaceDate(date: String, entities: List<EarningsEntity>, status: SyncStatusEntity) {
        deleteApiEntriesForDate(date)
        if (entities.isNotEmpty()) {
            upsertAll(entities)
        }
        upsertSyncStatus(status)
    }
}
