package com.jdm.stockcalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jdm.stockcalendar.data.EarningsRepository
import com.jdm.stockcalendar.data.NASDAQ_ZONE
import com.jdm.stockcalendar.domain.EarningsEvent
import com.jdm.stockcalendar.domain.EarningsTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private const val QUERY_WINDOW_DAYS = 14L

class CalendarViewModel(
    private val repository: EarningsRepository,
    today: LocalDate = LocalDate.now(NASDAQ_ZONE),
) : ViewModel() {

    private val queryWindowEnd = today.plusDays(QUERY_WINDOW_DAYS)

    private val _state = MutableStateFlow(
        CalendarUiState(
            today = today,
            queryWindowEnd = queryWindowEnd,
            visibleMonth = YearMonth.from(today),
            selectedDate = today,
        ),
    )
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        observeMarkedDates()
        observeSelectedDayEvents()
        syncUpcomingWindow(forceRefresh = false)
    }

    fun onMonthChanged(month: YearMonth) {
        _state.update { it.copy(visibleMonth = month) }
    }

    fun onDateSelected(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
    }

    fun onRefresh() {
        syncUpcomingWindow(forceRefresh = true)
    }

    fun onAddEventClicked() {
        _state.update {
            it.copy(
                editDialogState = EditDialogState.Editing(
                    original = null,
                    date = it.selectedDate,
                    symbol = "",
                    name = "",
                    marketCap = "",
                    epsForecast = "",
                    time = EarningsTime.UNKNOWN,
                ),
            )
        }
    }

    fun onEditEventClicked(event: EarningsEvent) {
        _state.update {
            it.copy(
                editDialogState = EditDialogState.Editing(
                    original = event,
                    date = event.date,
                    symbol = event.symbol,
                    name = event.name,
                    marketCap = event.marketCap.orEmpty(),
                    epsForecast = event.epsForecast.orEmpty(),
                    time = event.time,
                ),
            )
        }
    }

    fun onEditFormChanged(updated: EditDialogState.Editing) {
        _state.update { it.copy(editDialogState = updated) }
    }

    fun onDismissEditDialog() {
        _state.update { it.copy(editDialogState = EditDialogState.Hidden) }
    }

    fun onSaveEvent() {
        val form = _state.value.editDialogState as? EditDialogState.Editing ?: return
        val symbol = form.symbol.trim().uppercase()
        if (symbol.isBlank()) {
            _state.update { it.copy(editDialogState = form.copy(error = "종목 코드를 입력하세요.")) }
            return
        }
        val event = EarningsEvent(
            symbol = symbol,
            name = form.name.trim().ifBlank { symbol },
            date = form.date,
            marketCap = form.marketCap.trim().ifBlank { null },
            fiscalQuarterEnding = form.original?.fiscalQuarterEnding,
            epsForecast = form.epsForecast.trim().ifBlank { null },
            numberOfEstimates = form.original?.numberOfEstimates,
            lastYearReportDate = form.original?.lastYearReportDate,
            lastYearEps = form.original?.lastYearEps,
            time = form.time,
            isUserAdded = true,
        )
        viewModelScope.launch {
            val result = repository.upsertUserEntry(event)
            _state.update {
                if (result.isSuccess) {
                    it.copy(editDialogState = EditDialogState.Hidden)
                } else {
                    it.copy(editDialogState = form.copy(error = result.exceptionOrNull()?.message ?: "저장 실패"))
                }
            }
        }
    }

    fun onDeleteEventClicked(event: EarningsEvent) {
        _state.update { it.copy(pendingDelete = event) }
    }

    fun onDismissDeleteConfirm() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun onConfirmDelete() {
        val target = _state.value.pendingDelete ?: return
        viewModelScope.launch {
            repository.deleteEntry(target.symbol, target.date)
            _state.update { it.copy(pendingDelete = null) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMarkedDates() {
        _state
            .map { it.visibleMonth }
            .distinctUntilChanged()
            .flatMapLatest { month ->
                repository.observeMarkedDates(month.minusMonths(1).atDay(1), month.plusMonths(1).atEndOfMonth())
            }
            .onEach { dates -> _state.update { it.copy(markedDates = dates) } }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedDayEvents() {
        _state
            .map { it.selectedDate }
            .distinctUntilChanged()
            .flatMapLatest { date -> repository.observeEarningsForDate(date) }
            .onEach { events -> _state.update { it.copy(selectedDayEvents = events) } }
            .launchIn(viewModelScope)
    }

    private fun syncUpcomingWindow(forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = repository.syncUpcomingWindow(
                today = _state.value.today,
                days = QUERY_WINDOW_DAYS,
                forceRefresh = forceRefresh,
            )
            _state.update {
                it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    companion object {
        fun factory(repository: EarningsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CalendarViewModel(repository) as T
            }
    }
}
