package com.dspcontroller.domain.model

/**
 * Represents a user-defined alert threshold for a device metric.
 *
 * @property id Local database primary key.
 * @property deviceMac MAC address of the monitored device.
 * @property metric The metric to monitor ("cpu", "heap", "signal_rms").
 * @property condition Comparison operator ("gt", "lt", "eq").
 * @property threshold The threshold value that triggers the alert.
 * @property notify Whether to show a notification when triggered.
 * @property triggered Whether this alert is currently in a triggered state.
 */
data class Alert(
    val id: Long = 0L,
    val deviceMac: String,
    val metric: String,
    val condition: String,
    val threshold: Float,
    val notify: Boolean = true,
    val triggered: Boolean = false
)
