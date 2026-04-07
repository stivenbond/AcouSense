package com.dspcontroller.domain.model

/**
 * Represents a discovered or previously connected DSP device.
 *
 * @property id Local database primary key.
 * @property mac Unique MAC address of the ESP32 device.
 * @property label User-assigned friendly name for the device.
 * @property ipLast Last known IP address on the local network.
 * @property firmware Firmware version string reported by the device.
 * @property addedAt Epoch milliseconds when the device was first added.
 * @property lastSeen Epoch milliseconds of the most recent connection.
 */
data class Device(
    val id: Long = 0L,
    val mac: String,
    val label: String = "",
    val ipLast: String,
    val firmware: String,
    val addedAt: Long,
    val lastSeen: Long
)
