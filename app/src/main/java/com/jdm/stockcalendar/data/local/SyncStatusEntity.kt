package com.jdm.stockcalendar.data.local

import androidx.room.Entity

@Entity(tableName = "sync_status", primaryKeys = ["date"])
data class SyncStatusEntity(
    val date: String,
    val syncedAtEpochDay: Long,
)
