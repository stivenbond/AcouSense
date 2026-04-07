package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.Alert
import com.dspcontroller.domain.model.Telemetry
import com.dspcontroller.domain.repository.AlertRepository

/**
 * Evaluates the latest telemetry reading against all active alert thresholds,
 * updating the triggered state of each alert accordingly.
 *
 * This use case is invoked every time a new telemetry reading arrives.
 * It compares the telemetry values (CPU %, heap bytes, signal RMS) against
 * each alert's threshold and condition, then updates the alert's triggered flag.
 */
class EvaluateAlertsUseCase(
    private val alertRepository: AlertRepository
) {

    /**
     * Evaluate all active alerts for a device against the latest telemetry.
     *
     * @param deviceMac The MAC address of the device.
     * @param telemetry The latest telemetry reading to evaluate.
     * @return [Result.success] with the list of currently triggered alerts.
     */
    suspend operator fun invoke(deviceMac: String, telemetry: Telemetry): Result<List<Alert>> =
        runCatching {
            val alerts = alertRepository.getActiveAlerts(deviceMac).getOrThrow()
            val triggeredAlerts = mutableListOf<Alert>()

            for (alert in alerts) {
                val metricValue = when (alert.metric) {
                    "cpu" -> telemetry.cpuPercent.toFloat()
                    "heap" -> telemetry.heapBytes.toFloat()
                    "signal_rms" -> telemetry.signalRms
                    else -> continue
                }

                val isTriggered = when (alert.condition) {
                    "gt" -> metricValue > alert.threshold
                    "lt" -> metricValue < alert.threshold
                    "eq" -> metricValue == alert.threshold
                    else -> false
                }

                // WHY: Only update the database if the triggered state actually changed
                // to avoid unnecessary writes on every telemetry tick.
                if (isTriggered != alert.triggered) {
                    alertRepository.setTriggered(alert.id, isTriggered)
                }

                if (isTriggered) {
                    triggeredAlerts.add(alert.copy(triggered = true))
                }
            }

            triggeredAlerts
        }
}
