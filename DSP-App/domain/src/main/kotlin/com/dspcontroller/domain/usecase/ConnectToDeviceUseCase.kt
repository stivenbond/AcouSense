package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.Device
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Establishes a WebSocket connection to a DSP device and persists the device
 * record in the local database upon successful connection.
 *
 * Waits for the [WsMessage.DeviceInfo] message to confirm the connection,
 * then upserts the device with the received MAC address and firmware version.
 */
class ConnectToDeviceUseCase(
    private val connectionRepository: ConnectionRepository,
    private val deviceRepository: DeviceRepository
) {

    /**
     * Connect to the device at the given IP address.
     *
     * @param ip The local network IP address of the ESP32 device.
     * @return [Result.success] with the connected [Device], or [Result.failure] on error.
     */
    suspend operator fun invoke(ip: String): Result<Device> = runCatching {
        connectionRepository.connect(ip)

        // WHY: Wait up to 5 seconds for the device_info message that confirms the connection.
        val deviceInfo = withTimeoutOrNull(5_000L) {
            connectionRepository.inboundMessages.first { it is WsMessage.DeviceInfo }
        } as? WsMessage.DeviceInfo
            ?: throw IllegalStateException("Device did not respond with device_info within 5 seconds")

        val now = System.currentTimeMillis()
        val device = Device(
            mac = deviceInfo.mac,
            label = "",
            ipLast = ip,
            firmware = deviceInfo.firmware,
            addedAt = now,
            lastSeen = now
        )

        deviceRepository.upsertDevice(device)
        device
    }
}
