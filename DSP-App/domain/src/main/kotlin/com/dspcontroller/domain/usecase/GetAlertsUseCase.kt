package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.Alert
import com.dspcontroller.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow

/**
 * Retrieves all alert definitions for a specific device as a reactive stream.
 */
class GetAlertsUseCase(
    private val alertRepository: AlertRepository
) {

    /**
     * Observe all alerts configured for a device.
     *
     * @param deviceMac The MAC address of the device.
     * @return A [Flow] emitting the current list of [Alert] definitions.
     */
    operator fun invoke(deviceMac: String): Flow<List<Alert>> {
        return alertRepository.observeAlerts(deviceMac)
    }
}
