package com.dspcontroller.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dspcontroller.data.db.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [DeviceEntity] operations.
 */
@Dao
interface DeviceDao {

    /**
     * Observe all devices as a reactive stream, ordered by last seen (newest first).
     */
    @Query("SELECT * FROM devices ORDER BY last_seen DESC")
    fun observeAll(): Flow<List<DeviceEntity>>

    /**
     * Get a device by its unique MAC address.
     */
    @Query("SELECT * FROM devices WHERE mac = :mac LIMIT 1")
    suspend fun getByMac(mac: String): DeviceEntity?

    /**
     * Insert or update a device record.
     * Uses REPLACE strategy: if a device with the same MAC exists, it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity): Long

    /**
     * Delete a device by its MAC address.
     */
    @Query("DELETE FROM devices WHERE mac = :mac")
    suspend fun deleteByMac(mac: String)

    /**
     * Update the last-seen timestamp and IP address for a device.
     */
    @Query("UPDATE devices SET last_seen = :timestamp, ip_last = :ip WHERE mac = :mac")
    suspend fun updateLastSeen(mac: String, ip: String, timestamp: Long)
}
