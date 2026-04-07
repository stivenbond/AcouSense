package com.dspcontroller.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class hierarchy representing all inbound WebSocket JSON messages
 * from the ESP32 device. Uses `type` as the discriminator field.
 *
 * Each subclass maps directly to a JSON shape defined in the protocol spec.
 */
@Serializable
sealed class WsMessageDto {

    /**
     * Real-time telemetry sent every 500ms.
     */
    @Serializable
    @SerialName("telemetry")
    data class TelemetryDto(
        val cpu: Int,
        val heap: Long,
        @SerialName("signal_rms") val signalRms: Float,
        val ts: Long
    ) : WsMessageDto()

    /**
     * Parameter change acknowledgement.
     */
    @Serializable
    @SerialName("param_ack")
    data class ParamAckDto(
        val key: String,
        val value: Float,
        val success: Boolean
    ) : WsMessageDto()

    /**
     * Full parameter dump response.
     */
    @Serializable
    @SerialName("params_dump")
    data class ParamsDumpDto(
        val params: List<ParamEntryDto>
    ) : WsMessageDto()

    /**
     * Preset loaded confirmation from device.
     */
    @Serializable
    @SerialName("preset_loaded")
    data class PresetLoadedDto(
        val id: Int,
        val name: String
    ) : WsMessageDto()

    /**
     * Device info sent once upon WebSocket connection.
     */
    @Serializable
    @SerialName("device_info")
    data class DeviceInfoMessageDto(
        val mac: String,
        val firmware: String,
        @SerialName("uptime_s") val uptimeSeconds: Long
    ) : WsMessageDto()

    /**
     * Error response from the device.
     */
    @Serializable
    @SerialName("error")
    data class ErrorDto(
        val code: Int,
        val message: String
    ) : WsMessageDto()
}

/**
 * A single parameter key-value entry within a [WsMessageDto.ParamsDumpDto].
 */
@Serializable
data class ParamEntryDto(
    val key: String,
    val value: Float,
    @SerialName("min_val") val minVal: Float = 0f,
    @SerialName("max_val") val maxVal: Float = 1f,
    val unit: String = "",
    val step: Float = 0.01f
)
