package com.dspcontroller.domain.repository

import com.dspcontroller.domain.model.Telemetry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for storing and querying device telemetry data.
 *
 * Telemetry data has a retention policy and row limit to prevent
 * unbounded growth of the local database.
 */
interface TelemetryRepository {

    /**
     * Observe telemetry readings for a device within a time window.
     *
     * @param deviceMac The MAC address of the device.
     * @param windowMs How far back in time to query, in milliseconds.
     */
    fun observeTelemetry(deviceMac: String, windowMs: Long): Flow<List<Telemetry>>

    /**
     * Insert a new telemetry reading.
     * Enforces the 10,000-row cap by deleting the oldest rows if necessary.
     */
    suspend fun insertTelemetry(telemetry: Telemetry): Result<Unit>

    /**
     * Delete telemetry records older than the given epoch timestamp.
     *
     * @param olderThanMs Epoch milliseconds threshold — all records before this are deleted.
     */
    suspend fun purgeOlderThan(olderThanMs: Long): Result<Int>

    /**
     * Get the total count of telemetry rows for a device.
     */
    suspend fun getCount(deviceMac: String): Result<Int>
}
