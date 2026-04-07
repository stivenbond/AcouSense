package com.dspcontroller.domain.model

/**
 * Sealed class hierarchy representing all outbound WebSocket commands
 * sent from the app to the ESP32 DSP device.
 *
 * Each subclass corresponds to a distinct `type` discriminator in the JSON protocol.
 */
sealed class WsCommand {

    /**
     * Command to set a single DSP parameter on the device.
     *
     * @property key The parameter identifier (e.g., "gain", "low_cutoff_hz").
     * @property value The new value to set.
     */
    data class SetParam(
        val key: String,
        val value: Float
    ) : WsCommand()

    /**
     * Command to load a saved preset by its ID on the device.
     *
     * @property id The preset ID to load.
     */
    data class LoadPreset(
        val id: Int
    ) : WsCommand()

    /**
     * Ping command requesting a full telemetry snapshot from the device.
     */
    data object Ping : WsCommand()

    /**
     * Command requesting a full dump of all current parameter values.
     */
    data object GetParams : WsCommand()
}
