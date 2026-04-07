package com.dspcontroller.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dspcontroller.data.datastore.AppSettingsDataStore
import com.dspcontroller.domain.model.Alert
import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.Telemetry
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.domain.repository.TelemetryRepository
import com.dspcontroller.domain.usecase.EvaluateAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main dashboard screen.
 *
 * Collects telemetry from the WebSocket, stores it in the database,
 * and evaluates alert thresholds. UI updates are debounced to 4 FPS.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val telemetryRepository: TelemetryRepository,
    private val evaluateAlertsUseCase: EvaluateAlertsUseCase,
    private val settingsDataStore: AppSettingsDataStore
) : ViewModel() {

    /** Current WebSocket connection state. */
    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    private val _telemetry = MutableStateFlow<Telemetry?>(null)

    /** Latest telemetry reading for display. */
    val telemetry: StateFlow<Telemetry?> = _telemetry.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<Alert>>(emptyList())

    /** Currently triggered alerts. */
    val activeAlerts: StateFlow<List<Alert>> = _activeAlerts.asStateFlow()

    /** Last connected device MAC. */
    val lastConnectedMac: StateFlow<String> = settingsDataStore.lastConnectedMac.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    init {
        collectTelemetry()
    }

    private fun collectTelemetry() {
        viewModelScope.launch {
            // WHY: conflate() ensures we only process the latest emission,
            // effectively debouncing telemetry updates to ~4 FPS max.
            connectionRepository.inboundMessages
                .conflate()
                .collect { message ->
                    if (message is WsMessage.TelemetryMessage) {
                        val mac = lastConnectedMac.value
                        if (mac.isEmpty()) return@collect

                        val telemetryData = Telemetry(
                            deviceMac = mac,
                            cpuPercent = message.cpu,
                            heapBytes = message.heap,
                            signalRms = message.signalRms,
                            timestamp = message.timestamp * 1000 // Convert seconds to millis
                        )

                        _telemetry.value = telemetryData

                        // Persist to database
                        telemetryRepository.insertTelemetry(telemetryData)
                            .onFailure { Timber.e(it, "Failed to save telemetry") }

                        // Evaluate alerts
                        evaluateAlertsUseCase(mac, telemetryData)
                            .onSuccess { triggered -> _activeAlerts.value = triggered }
                            .onFailure { Timber.e(it, "Failed to evaluate alerts") }
                    }
                }
        }
    }

    /** Disconnect from the current device. */
    fun disconnect() {
        viewModelScope.launch {
            connectionRepository.disconnect()
        }
    }
}
