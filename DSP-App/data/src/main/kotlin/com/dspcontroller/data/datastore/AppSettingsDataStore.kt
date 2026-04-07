package com.dspcontroller.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Extension for creating the DataStore instance on the Application context. */
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dsp_settings"
)

/**
 * Typed wrapper around Preferences DataStore for application settings.
 *
 * Provides strongly-typed access to each setting as a [Flow] and
 * suspend setter functions.
 */
@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val LAST_CONNECTED_IP = stringPreferencesKey("last_connected_ip")
        val LAST_CONNECTED_MAC = stringPreferencesKey("last_connected_mac")
        val WS_RECONNECT_INTERVAL_MS = longPreferencesKey("ws_reconnect_interval_ms")
        val TELEMETRY_RETENTION_DAYS = intPreferencesKey("telemetry_retention_days")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CHART_WINDOW_SECONDS = intPreferencesKey("chart_window_seconds")
    }

    // ── Read Flows ───────────────────────────────────────────────────────────

    /** Last connected device IP address. */
    val lastConnectedIp: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[Keys.LAST_CONNECTED_IP] ?: ""
    }

    /** Last connected device MAC address. */
    val lastConnectedMac: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[Keys.LAST_CONNECTED_MAC] ?: ""
    }

    /** WebSocket reconnect interval in milliseconds. */
    val wsReconnectIntervalMs: Flow<Long> = context.appDataStore.data.map { prefs ->
        prefs[Keys.WS_RECONNECT_INTERVAL_MS] ?: 3_000L
    }

    /** Telemetry data retention period in days. */
    val telemetryRetentionDays: Flow<Int> = context.appDataStore.data.map { prefs ->
        prefs[Keys.TELEMETRY_RETENTION_DAYS] ?: 7
    }

    /** UI theme mode: "system", "light", or "dark". */
    val themeMode: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    /** Chart time window in seconds. */
    val chartWindowSeconds: Flow<Int> = context.appDataStore.data.map { prefs ->
        prefs[Keys.CHART_WINDOW_SECONDS] ?: 30
    }

    // ── Write Functions ──────────────────────────────────────────────────────

    /** Save the last connected device IP and MAC. */
    suspend fun setLastConnected(ip: String, mac: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.LAST_CONNECTED_IP] = ip
            prefs[Keys.LAST_CONNECTED_MAC] = mac
        }
    }

    /** Update the WebSocket reconnect interval. */
    suspend fun setWsReconnectIntervalMs(intervalMs: Long) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.WS_RECONNECT_INTERVAL_MS] = intervalMs
        }
    }

    /** Update the telemetry retention period. */
    suspend fun setTelemetryRetentionDays(days: Int) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.TELEMETRY_RETENTION_DAYS] = days
        }
    }

    /** Update the theme mode. */
    suspend fun setThemeMode(mode: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    /** Update the chart time window. */
    suspend fun setChartWindowSeconds(seconds: Int) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.CHART_WINDOW_SECONDS] = seconds
        }
    }

    /** Clear all stored preferences. */
    suspend fun clearAll() {
        context.appDataStore.edit { it.clear() }
    }
}
