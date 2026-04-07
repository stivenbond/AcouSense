package com.dspcontroller.domain.model

/**
 * Sealed class hierarchy representing all possible inbound WebSocket messages
 * received from the ESP32 DSP device.
 *
 * Each subclass corresponds to a distinct `type` discriminator in the JSON protocol.
 */
sealed class WsMessage {

    /**
     * Real-time telemetry data sent by the device every 500ms.
     *
     * @property cpu CPU usage percentage (0–100).
     * @property heap Free heap memory in bytes.
     * @property signalRms Root Mean Square of the audio signal.
     * @property timestamp Epoch seconds from the device clock.
     */
    data class TelemetryMessage(
        val cpu: Int,
        val heap: Long,
        val signalRms: Float,
        val timestamp: Long
    ) : WsMessage()

    /**
     * Acknowledgement that a parameter was successfully set on the device.
     *
     * @property key The parameter key that was set.
     * @property value The confirmed value.
     * @property success Whether the operation succeeded.
     */
    data class ParamAck(
        val key: String,
        val value: Float,
        val success: Boolean
    ) : WsMessage()

    /**
     * Full dump of all current device parameters, sent in response to a get_params command.
     *
     * @property params List of key-value pairs representing all device parameters.
     */
    data class ParamsDump(
        val params: List<DspParam>
    ) : WsMessage()

    /**
     * Confirmation that a preset was loaded on the device.
     *
     * @property id The preset ID that was loaded.
     * @property name The name of the loaded preset.
     */
    data class PresetLoaded(
        val id: Int,
        val name: String
    ) : WsMessage()

    /**
     * Device information sent upon initial WebSocket connection.
     *
     * @property mac Device MAC address.
     * @property firmware Firmware version string.
     * @property uptimeSeconds Device uptime in seconds since last boot.
     */
    data class DeviceInfo(
        val mac: String,
        val firmware: String,
        val uptimeSeconds: Long
    ) : WsMessage()

    /**
     * Error message from the device indicating a failed operation.
     *
     * @property code HTTP-style error code.
     * @property message Human-readable error description.
     */
    data class Error(
        val code: Int,
        val message: String
    ) : WsMessage()
}
