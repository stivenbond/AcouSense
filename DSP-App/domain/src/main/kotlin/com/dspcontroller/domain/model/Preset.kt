package com.dspcontroller.domain.model

/**
 * Represents a saved DSP configuration preset containing a collection of parameters.
 *
 * @property id Local database primary key.
 * @property deviceMac MAC address of the device this preset belongs to.
 * @property name Human-readable preset name (e.g., "Studio Mix").
 * @property description Optional description of the preset's purpose.
 * @property params List of all DSP parameters in this preset.
 * @property checksum SHA-256 hash of the sorted parameter JSON for sync comparison.
 * @property synced Whether this preset has been synced to the ESP32 device.
 * @property createdAt Epoch milliseconds when the preset was created.
 * @property updatedAt Epoch milliseconds of the last modification.
 */
data class Preset(
    val id: Long = 0L,
    val deviceMac: String,
    val name: String,
    val description: String = "",
    val params: List<DspParam> = emptyList(),
    val checksum: String = "",
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
