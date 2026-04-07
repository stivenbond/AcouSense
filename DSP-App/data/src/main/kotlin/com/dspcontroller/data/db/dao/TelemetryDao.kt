package com.dspcontroller.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dspcontroller.data.db.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [TelemetryEntity] operations.
 *
 * Supports windowed queries for chart rendering and bulk purge
 * of records older than a given timestamp.
 */
@Dao
interface TelemetryDao {

    /**
     * Observe telemetry for a device within a time window.
     * Returns records where `ts >= :sinceTs`, ordered by timestamp ascending.
     */
    @Query(
        "SELECT * FROM telemetry WHERE device_mac = :deviceMac AND ts >= :sinceTs ORDER BY ts ASC"
    )
    fun observeWindow(deviceMac: String, sinceTs: Long): Flow<List<TelemetryEntity>>

    /**
     * Insert a single telemetry reading.
     */
    @Insert
    suspend fun insert(entity: TelemetryEntity): Long

    /**
     * Delete all telemetry records older than the given timestamp.
     *
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM telemetry WHERE ts < :olderThanTs")
    suspend fun deleteOlderThan(olderThanTs: Long): Int

    /**
     * Count total telemetry rows for a device.
     */
    @Query("SELECT COUNT(*) FROM telemetry WHERE device_mac = :deviceMac")
    suspend fun countByDevice(deviceMac: String): Int

    /**
     * Delete the oldest N rows for a device, used to enforce the 10,000-row cap.
     */
    @Query(
        """
        DELETE FROM telemetry WHERE id IN (
            SELECT id FROM telemetry 
            WHERE device_mac = :deviceMac 
            ORDER BY ts ASC 
            LIMIT :count
        )
        """
    )
    suspend fun deleteOldest(deviceMac: String, count: Int)
}
