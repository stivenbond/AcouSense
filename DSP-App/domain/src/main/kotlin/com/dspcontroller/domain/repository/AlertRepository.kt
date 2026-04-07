package com.dspcontroller.domain.repository

import com.dspcontroller.domain.model.Alert
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing metric alert thresholds.
 */
interface AlertRepository {

    /**
     * Observe all alerts for a given device.
     */
    fun observeAlerts(deviceMac: String): Flow<List<Alert>>

    /**
     * Get a single alert by ID.
     */
    suspend fun getAlertById(id: Long): Result<Alert?>

    /**
     * Insert or update an alert.
     */
    suspend fun upsertAlert(alert: Alert): Result<Long>

    /**
     * Delete an alert by ID.
     */
    suspend fun deleteAlert(id: Long): Result<Unit>

    /**
     * Update the triggered state of an alert.
     */
    suspend fun setTriggered(id: Long, triggered: Boolean): Result<Unit>

    /**
     * Get all alerts that have notifications enabled for a device.
     */
    suspend fun getActiveAlerts(deviceMac: String): Result<List<Alert>>
}
