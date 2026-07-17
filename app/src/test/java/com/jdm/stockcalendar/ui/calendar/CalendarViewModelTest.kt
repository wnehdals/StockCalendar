package com.jdm.stockcalendar.ui.calendar

import app.cash.turbine.test
import com.jdm.stockcalendar.domain.EarningsEvent
import com.jdm.stockcalendar.domain.EarningsTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val today = LocalDate.of(2026, 7, 17)

    private fun sampleEvent(symbol: String, date: LocalDate) = EarningsEvent(
        symbol = symbol,
        name = "$symbol Inc.",
        date = date,
        marketCap = null,
        fiscalQuarterEnding = null,
        epsForecast = null,
        numberOfEstimates = null,
        lastYearReportDate = null,
        lastYearEps = null,
        time = EarningsTime.PRE_MARKET,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers exactly one sync of today through today plus 14`() = runTest {
        val repository = FakeEarningsRepository()

        CalendarViewModel(repository, today = today)

        assertEquals(1, repository.syncCalls.size)
        assertEquals(FakeEarningsRepository.SyncCall(today, 14L, false), repository.syncCalls.single())
    }

    @Test
    fun `settles to non-loading state with no error on success`() = runTest {
        val repository = FakeEarningsRepository()
        repository.setEventsForDate(today, listOf(sampleEvent("AAA", today)))
        val viewModel = CalendarViewModel(repository, today = today)

        viewModel.state.test {
            val settled = awaitItem()
            assertFalse(settled.isLoading)
            assertNull(settled.error)
            assertEquals(listOf("AAA"), settled.selectedDayEvents.map { it.symbol })
        }
    }

    @Test
    fun `sync failure surfaces an error message and stops loading`() = runTest {
        val repository = FakeEarningsRepository()
        repository.syncResult = Result.failure(RuntimeException("network down"))
        val viewModel = CalendarViewModel(repository, today = today)

        viewModel.state.test {
            val settled = awaitItem()
            assertFalse(settled.isLoading)
            assertNotNull(settled.error)
        }
    }

    @Test
    fun `selecting a date outside the query window still shows its cached events without an extra sync`() = runTest {
        val repository = FakeEarningsRepository()
        val outOfWindowDate = today.plusDays(30)
        repository.setEventsForDate(outOfWindowDate, listOf(sampleEvent("FAR", outOfWindowDate)))
        val viewModel = CalendarViewModel(repository, today = today)

        viewModel.onDateSelected(outOfWindowDate)

        assertEquals(1, repository.syncCalls.size)
        assertEquals(listOf("FAR"), viewModel.state.value.selectedDayEvents.map { it.symbol })
    }

    @Test
    fun `onRefresh triggers a second sync with forceRefresh true`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)

        viewModel.onRefresh()

        assertEquals(2, repository.syncCalls.size)
        assertEquals(FakeEarningsRepository.SyncCall(today, 14L, true), repository.syncCalls.last())
    }

    @Test
    fun `onAddEventClicked opens a blank Add form for the selected date`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)

        viewModel.onAddEventClicked()

        val dialog = viewModel.state.value.editDialogState as EditDialogState.Editing
        assertTrue(dialog.isNew)
        assertEquals(today, dialog.date)
        assertEquals("", dialog.symbol)
    }

    @Test
    fun `onEditEventClicked opens a pre-filled Edit form`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)
        val event = sampleEvent("AAA", today)

        viewModel.onEditEventClicked(event)

        val dialog = viewModel.state.value.editDialogState as EditDialogState.Editing
        assertFalse(dialog.isNew)
        assertEquals("AAA", dialog.symbol)
    }

    @Test
    fun `saving with a blank symbol shows a validation error and does not call the repository`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)
        viewModel.onAddEventClicked()

        viewModel.onSaveEvent()

        assertTrue(repository.upsertedEvents.isEmpty())
        val dialog = viewModel.state.value.editDialogState as EditDialogState.Editing
        assertNotNull(dialog.error)
    }

    @Test
    fun `saving a valid new entry closes the dialog and calls upsertUserEntry`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)
        viewModel.onAddEventClicked()
        val form = viewModel.state.value.editDialogState as EditDialogState.Editing
        viewModel.onEditFormChanged(form.copy(symbol = "aaa", name = "AAA Inc."))

        viewModel.onSaveEvent()

        assertEquals(EditDialogState.Hidden, viewModel.state.value.editDialogState)
        assertEquals("AAA", repository.upsertedEvents.single().symbol)
    }

    @Test
    fun `editing preserves fields not shown in the form`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)
        val original = sampleEvent("AAA", today).copy(numberOfEstimates = "5", lastYearEps = "${'$'}0.20")
        viewModel.onEditEventClicked(original)

        viewModel.onSaveEvent()

        val saved = repository.upsertedEvents.single()
        assertEquals("5", saved.numberOfEstimates)
        assertEquals("${'$'}0.20", saved.lastYearEps)
    }

    @Test
    fun `delete flow requires confirmation before calling the repository`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)
        val event = sampleEvent("AAA", today)

        viewModel.onDeleteEventClicked(event)
        assertEquals(event, viewModel.state.value.pendingDelete)
        assertTrue(repository.deletedEntries.isEmpty())

        viewModel.onConfirmDelete()

        assertNull(viewModel.state.value.pendingDelete)
        assertEquals("AAA" to today, repository.deletedEntries.single())
    }

    @Test
    fun `dismissing the delete confirmation does not call the repository`() = runTest {
        val repository = FakeEarningsRepository()
        val viewModel = CalendarViewModel(repository, today = today)
        viewModel.onDeleteEventClicked(sampleEvent("AAA", today))

        viewModel.onDismissDeleteConfirm()

        assertNull(viewModel.state.value.pendingDelete)
        assertTrue(repository.deletedEntries.isEmpty())
    }
}
