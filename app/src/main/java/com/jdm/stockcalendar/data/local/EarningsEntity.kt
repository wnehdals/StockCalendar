package com.jdm.stockcalendar.data.local

import androidx.room.Entity

@Entity(tableName = "earnings", primaryKeys = ["symbol", "date"])
data class EarningsEntity(
    val symbol: String,
    val date: String,
    val name: String,
    val marketCap: String?,
    val fiscalQuarterEnding: String?,
    val epsForecast: String?,
    val numberOfEstimates: String?,
    val lastYearReportDate: String?,
    val lastYearEps: String?,
    val time: String,
    val isUserAdded: Boolean = false,
)
