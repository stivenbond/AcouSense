package com.dspcontroller.domain.model

/**
 * Represents a single telemetry snapshot from the ESP32 DSP device.
 *
 * @property id Local database primary key.
 * @property deviceMac MAC address of the source device.
 * @property cpuPercent CPU usage percentage (0–100).
 * @property heapBytes Free heap memory in bytes.
 * @property signalRms Root Mean Square of the current audio signal (0.0–1.0).
 * @property timestamp Epoch milliseconds when this reading was taken.
 */
data class Telemetry(
    val id: Long = 0L,
    val deviceMac: String,
    val cpuPercent: Int,
    val heapBytes: Long,
    val signalRms: Float,
    val timestamp: Long
)
