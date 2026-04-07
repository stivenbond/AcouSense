package com.dspcontroller.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO for device information returned by the REST API `GET /api/info` endpoint.
 *
 * @property mac Device MAC address.
 * @property firmware Firmware version string.
 * @property uptime Device uptime in seconds.
 */
@Serializable
data class DeviceInfoDto(
    val mac: String,
    val firmware: String,
    val uptime: Long
)
