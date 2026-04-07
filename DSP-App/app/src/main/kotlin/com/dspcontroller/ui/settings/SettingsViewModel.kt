package com.dspcontroller.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dspcontroller.data.datastore.AppSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen.
 *
 * Reads from and writes to [AppSettingsDataStore] for all app preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: AppSettingsDataStore
) : ViewModel() {

    /** Current theme mode ("system", "light", "dark"). */
    val themeMode: StateFlow<String> = settingsDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "system"
    )

    /** WebSocket reconnect interval in milliseconds. */
    val wsReconnectIntervalMs: StateFlow<Long> = settingsDataStore.wsReconnectIntervalMs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 3_000L
    )

    /** Telemetry data retention period in days. */
    val telemetryRetentionDays: StateFlow<Int> = settingsDataStore.telemetryRetentionDays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 7
    )

    /** Chart time window in seconds. */
    val chartWindowSeconds: StateFlow<Int> = settingsDataStore.chartWindowSeconds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 30
    )

    /** Update the theme mode. */
    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    /** Update the reconnect interval. */
    fun setReconnectInterval(intervalMs: Long) {
        viewModelScope.launch { settingsDataStore.setWsReconnectIntervalMs(intervalMs) }
    }

    /** Update telemetry retention days. */
    fun setTelemetryRetentionDays(days: Int) {
        viewModelScope.launch { settingsDataStore.setTelemetryRetentionDays(days) }
    }

    /** Clear all local data (preferences). */
    fun clearLocalData() {
        viewModelScope.launch { settingsDataStore.clearAll() }
    }
}
