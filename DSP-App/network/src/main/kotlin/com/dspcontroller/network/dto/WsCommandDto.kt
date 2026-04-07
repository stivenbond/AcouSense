package com.dspcontroller.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class hierarchy for all outbound WebSocket JSON commands
 * sent from the app to the ESP32 device.
 */
@Serializable
sealed class WsCommandDto {

    /**
     * Command to set a DSP parameter.
     */
    @Serializable
    @SerialName("set_param")
    data class SetParamDto(
        val type: String = "set_param",
        val key: String,
        val value: Float
    ) : WsCommandDto()

    /**
     * Command to load a preset by ID.
     */
    @Serializable
    @SerialName("load_preset")
    data class LoadPresetDto(
        val type: String = "load_preset",
        val id: Int
    ) : WsCommandDto()

    /**
     * Ping command requesting telemetry snapshot.
     */
    @Serializable
    @SerialName("ping")
    data class PingDto(
        val type: String = "ping"
    ) : WsCommandDto()

    /**
     * Command requesting full parameter dump.
     */
    @Serializable
    @SerialName("get_params")
    data class GetParamsDto(
        val type: String = "get_params"
    ) : WsCommandDto()
}
