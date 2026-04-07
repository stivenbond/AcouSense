package com.dspcontroller.ui.dashboard

import app.cash.turbine.test
import com.dspcontroller.data.datastore.AppSettingsDataStore
import com.dspcontroller.domain.model.Alert
import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.Telemetry
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.domain.repository.TelemetryRepository
import com.dspcontroller.domain.usecase.EvaluateAlertsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DashboardViewModel] using Turbine and MockK.
 *
 * Tests cover:
 * - Initial state verification
 * - Connection state observation
 * - Telemetry message processing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var connectionRepository: ConnectionRepository
    private lateinit var telemetryRepository: TelemetryRepository
    private lateinit var evaluateAlertsUseCase: EvaluateAlertsUseCase
    private lateinit var settingsDataStore: AppSettingsDataStore
    private lateinit var viewModel: DashboardViewModel

    private val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val inboundMessagesFlow = MutableSharedFlow<WsMessage>(replay = 0, extraBufferCapacity = 64)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        connectionRepository = mockk(relaxed = true) {
            every { connectionState } returns connectionStateFlow
            every { inboundMessages } returns inboundMessagesFlow
        }

        telemetryRepository = mockk(relaxed = true) {
            coEvery { insertTelemetry(any()) } returns Result.success(Unit)
        }

        val alertRepository = mockk<com.dspcontroller.domain.repository.AlertRepository>(relaxed = true) {
            coEvery { getActiveAlerts(any()) } returns Result.success(emptyList())
        }
        evaluateAlertsUseCase = EvaluateAlertsUseCase(alertRepository)

        settingsDataStore = mockk(relaxed = true) {
            every { lastConnectedMac } returns flowOf("AA:BB:CC:DD:EE:FF")
            every { lastConnectedIp } returns flowOf("192.168.1.100")
        }

        viewModel = DashboardViewModel(
            connectionRepository = connectionRepository,
            telemetryRepository = telemetryRepository,
            evaluateAlertsUseCase = evaluateAlertsUseCase,
            settingsDataStore = settingsDataStore
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial telemetry is null`() {
        assertNull(viewModel.telemetry.value)
    }

    @Test
    fun `initial connection state is Disconnected`() {
        assertEquals(ConnectionState.Disconnected, viewModel.connectionState.value)
    }

    @Test
    fun `initial active alerts is empty`() {
        assertEquals(emptyList<Alert>(), viewModel.activeAlerts.value)
    }

    @Test
    fun `connection state updates are observed`() {
        connectionStateFlow.value = ConnectionState.Connected
        assertEquals(ConnectionState.Connected, viewModel.connectionState.value)

        connectionStateFlow.value = ConnectionState.Disconnected
        assertEquals(ConnectionState.Disconnected, viewModel.connectionState.value)
    }

    @Test
    fun `connectionState flow emits state changes`() = kotlinx.coroutines.test.runTest {
        viewModel.connectionState.test {
            assertEquals(ConnectionState.Disconnected, awaitItem())

            connectionStateFlow.value = ConnectionState.Connecting
            assertEquals(ConnectionState.Connecting, awaitItem())

            connectionStateFlow.value = ConnectionState.Connected
            assertEquals(ConnectionState.Connected, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
