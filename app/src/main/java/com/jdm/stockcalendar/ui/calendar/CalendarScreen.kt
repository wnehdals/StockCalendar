package com.jdm.stockcalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdm.stockcalendar.data.EarningsRepository
import com.jdm.stockcalendar.domain.EarningsEvent
import com.jdm.stockcalendar.domain.EarningsTime
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun CalendarScreen(repository: EarningsRepository, modifier: Modifier = Modifier) {
    val factory = remember(repository) { CalendarViewModel.factory(repository) }
    val viewModel: CalendarViewModel = viewModel(factory = factory)
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    CalendarContent(
        uiState = uiState,
        onMonthChanged = viewModel::onMonthChanged,
        onDateSelected = viewModel::onDateSelected,
        onRefresh = viewModel::onRefresh,
        onAddEventClicked = viewModel::onAddEventClicked,
        onEditEventClicked = viewModel::onEditEventClicked,
        onDeleteEventClicked = viewModel::onDeleteEventClicked,
        onEditFormChanged = viewModel::onEditFormChanged,
        onDismissEditDialog = viewModel::onDismissEditDialog,
        onSaveEvent = viewModel::onSaveEvent,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDelete = viewModel::onConfirmDelete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onAddEventClicked: () -> Unit,
    onEditEventClicked: (EarningsEvent) -> Unit,
    onDeleteEventClicked: (EarningsEvent) -> Unit,
    onEditFormChanged: (EditDialogState.Editing) -> Unit,
    onDismissEditDialog: () -> Unit,
    onSaveEvent: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startMonth = remember(uiState.today) { YearMonth.from(uiState.today).minusMonths(12) }
    val endMonth = remember(uiState.today) { YearMonth.from(uiState.today).plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = uiState.visibleMonth,
        firstDayOfWeek = daysOfWeek.first(),
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(calendarState.firstVisibleMonth) {
        onMonthChanged(calendarState.firstVisibleMonth.yearMonth)
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MonthTitleRow(
                visibleMonth = calendarState.firstVisibleMonth.yearMonth,
                onPrevious = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(calendarState.firstVisibleMonth.yearMonth.previousMonth)
                    }
                },
                onNext = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(calendarState.firstVisibleMonth.yearMonth.nextMonth)
                    }
                },
            )
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.error?.let { message ->
                Text(
                    text = "동기화 실패: $message",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    CalendarDayCell(
                        day = day,
                        isMarked = day.date in uiState.markedDates,
                        isSelected = day.date == uiState.selectedDate,
                        isInQueryWindow = !day.date.isBefore(uiState.today) && !day.date.isAfter(uiState.queryWindowEnd),
                        onClick = onDateSelected,
                    )
                },
                monthHeader = { WeekHeader(daysOfWeek) },
            )
            DayDetailSection(
                uiState = uiState,
                onAddEventClicked = onAddEventClicked,
                onEditEventClicked = onEditEventClicked,
                onDeleteEventClicked = onDeleteEventClicked,
            )
        }
    }

    val editState = uiState.editDialogState
    if (editState is EditDialogState.Editing) {
        EarningsEventEditDialog(
            state = editState,
            onFieldChange = onEditFormChanged,
            onDismiss = onDismissEditDialog,
            onSave = onSaveEvent,
        )
    }

    uiState.pendingDelete?.let { pending ->
        DeleteConfirmDialog(event = pending, onConfirm = onConfirmDelete, onDismiss = onDismissDeleteConfirm)
    }
}

@Composable
private fun MonthTitleRow(visibleMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) { Text("‹ 이전") }
        Text(
            text = "${visibleMonth.year}년 ${visibleMonth.monthValue}월",
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onNext) { Text("다음 ›") }
    }
}

@Composable
private fun WeekHeader(daysOfWeek: List<DayOfWeek>) {
    val locale = LocalConfiguration.current.locales[0]
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isMarked: Boolean,
    isSelected: Boolean,
    isInQueryWindow: Boolean,
    onClick: (LocalDate) -> Unit,
) {
    val isCurrentMonthDate = day.position == DayPosition.MonthDate
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = when {
        !isCurrentMonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !isInQueryWindow -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = isCurrentMonthDate) { onClick(day.date) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = day.date.dayOfMonth.toString(), color = textColor)
            if (isMarked && isCurrentMonthDate) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }
    }
}

@Composable
private fun DayDetailSection(
    uiState: CalendarUiState,
    onAddEventClicked: () -> Unit,
    onEditEventClicked: (EarningsEvent) -> Unit,
    onDeleteEventClicked: (EarningsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = uiState.selectedDate.toString(), style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onAddEventClicked) { Text("+ 일정 추가") }
        }
        if (uiState.selectedDayEvents.isEmpty()) {
            Text(
                text = "이 날짜에 저장된 실적 발표가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(uiState.selectedDayEvents, key = { it.symbol }) { event ->
                    EarningsEventRow(
                        event = event,
                        onEditClicked = { onEditEventClicked(event) },
                        onDeleteClicked = { onDeleteEventClicked(event) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningsEventRow(event: EarningsEvent, onEditClicked: () -> Unit, onDeleteClicked: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "${event.symbol} · ${event.name}", style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onClick = onEditClicked) { Text("수정") }
                    TextButton(onClick = onDeleteClicked) { Text("삭제") }
                }
            }
            Text(
                text = "EPS 예상: ${event.epsForecast.toKrwPriceDisplay()}  ·  시가총액: ${event.marketCap.toKrwPriceDisplay()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val timeOptions = listOf(
    EarningsTime.PRE_MARKET to "장 시작 전",
    EarningsTime.POST_MARKET to "장 마감 후",
    EarningsTime.NOT_SUPPLIED to "미정",
    EarningsTime.UNKNOWN to "알 수 없음",
)

@Composable
private fun EarningsEventEditDialog(
    state: EditDialogState.Editing,
    onFieldChange: (EditDialogState.Editing) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isNew) "실적 발표 일정 추가" else "실적 발표 일정 수정") },
        text = {
            Column {
                Text(text = state.date.toString(), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.symbol,
                    onValueChange = { onFieldChange(state.copy(symbol = it, error = null)) },
                    label = { Text("종목 코드") },
                    enabled = state.isNew,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onFieldChange(state.copy(name = it)) },
                    label = { Text("회사명") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = state.epsForecast,
                    onValueChange = { onFieldChange(state.copy(epsForecast = it)) },
                    label = { Text("EPS 예상 (USD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = state.marketCap,
                    onValueChange = { onFieldChange(state.copy(marketCap = it)) },
                    label = { Text("시가총액 (USD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    for ((time, label) in timeOptions) {
                        FilterChip(
                            selected = state.time == time,
                            onClick = { onFieldChange(state.copy(time = time)) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun DeleteConfirmDialog(event: EarningsEvent, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 삭제") },
        text = { Text("${event.symbol} · ${event.name} 일정을 삭제할까요?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("삭제") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
