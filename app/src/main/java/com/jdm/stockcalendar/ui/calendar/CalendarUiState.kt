package com.jdm.stockcalendar.ui.calendar

import com.jdm.stockcalendar.domain.EarningsEvent
import com.jdm.stockcalendar.domain.EarningsTime
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val today: LocalDate,
    val queryWindowEnd: LocalDate,
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val markedDates: Set<LocalDate> = emptySet(),
    val selectedDayEvents: List<EarningsEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val editDialogState: EditDialogState = EditDialogState.Hidden,
    val pendingDelete: EarningsEvent? = null,
)

sealed interface EditDialogState {
    data object Hidden : EditDialogState

    /**
     * Backs both the "add" and "edit" forms. [original] is null for a brand-new entry; when
     * editing an existing entry, fields not exposed in the form (fiscalQuarterEnding,
     * numberOfEstimates, lastYearReportDate, lastYearEps) are carried over from [original]
     * on save so they aren't silently wiped.
     */
    data class Editing(
        val original: EarningsEvent?,
        val date: LocalDate,
        val symbol: String,
        val name: String,
        val marketCap: String,
        val epsForecast: String,
        val time: EarningsTime,
        val error: String? = null,
    ) : EditDialogState {
        val isNew: Boolean get() = original == null
    }
}
