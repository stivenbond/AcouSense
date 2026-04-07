package com.dspcontroller.ui.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dspcontroller.data.datastore.AppSettingsDataStore
import com.dspcontroller.domain.model.Telemetry
import com.dspcontroller.domain.usecase.ObserveTelemetryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the telemetry chart and statistics screen.
 *
 * Observes telemetry data within a configurable time window
 * and exposes it as a [StateFlow] for the chart Composable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TelemetryViewModel @Inject constructor(
    private val observeTelemetryUseCase: ObserveTelemetryUseCase,
    private val settingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _chartWindowSeconds = MutableStateFlow(30)

    /** Current chart time window in seconds. */
    val chartWindowSeconds: StateFlow<Int> = _chartWindowSeconds.asStateFlow()

    /** Telemetry data within the current time window. */
    val telemetryWindow: StateFlow<List<Telemetry>> = combine(
        settingsDataStore.lastConnectedMac,
        _chartWindowSeconds
    ) { mac, windowSec ->
        mac to windowSec
    }.flatMapLatest { (mac, windowSec) ->
        if (mac.isEmpty()) flowOf(emptyList())
        else observeTelemetryUseCase(mac, windowSec * 1000L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        // Load saved chart window preference
        viewModelScope.launch {
            settingsDataStore.chartWindowSeconds.collect { seconds ->
                _chartWindowSeconds.value = seconds
            }
        }
    }

    /**
     * Update the chart time window.
     *
     * @param seconds New window size (10, 30, 60, or 120 seconds).
     */
    fun setChartWindow(seconds: Int) {
        _chartWindowSeconds.value = seconds
        viewModelScope.launch {
            settingsDataStore.setChartWindowSeconds(seconds)
        }
    }
}
