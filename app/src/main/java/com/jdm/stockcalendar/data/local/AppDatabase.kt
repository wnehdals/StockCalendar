package com.jdm.stockcalendar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EarningsEntity::class, SyncStatusEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun earningsDao(): EarningsDao
}
