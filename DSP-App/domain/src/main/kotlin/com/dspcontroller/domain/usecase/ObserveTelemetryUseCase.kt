package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.Telemetry
import com.dspcontroller.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Provides a reactive stream of telemetry data for the connected device
 * within a specified time window.
 *
 * This use case returns a cold [Flow] that automatically emits new readings
 * as they are inserted into the database.
 */
class ObserveTelemetryUseCase(
    private val telemetryRepository: TelemetryRepository
) {

    /**
     * Observe telemetry for a device within a rolling time window.
     *
     * @param deviceMac The MAC address of the device to monitor.
     * @param windowMs The size of the observation window in milliseconds.
     * @return A [Flow] emitting lists of [Telemetry] readings as they update.
     */
    operator fun invoke(deviceMac: String, windowMs: Long): Flow<List<Telemetry>> {
        return telemetryRepository.observeTelemetry(deviceMac, windowMs)
    }
}
