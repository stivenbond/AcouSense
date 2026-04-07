package com.dspcontroller.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for a DSP preset as returned by the ESP32 REST API.
 *
 * @property id Remote preset ID on the device.
 * @property name Human-readable preset name.
 * @property description Optional description.
 * @property params List of parameter entries in this preset.
 */
@Serializable
data class PresetDto(
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val params: List<ParamEntryDto> = emptyList()
)
