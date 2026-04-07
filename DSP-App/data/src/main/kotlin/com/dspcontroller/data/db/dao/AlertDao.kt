package com.dspcontroller.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dspcontroller.data.db.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [AlertEntity] operations.
 */
@Dao
interface AlertDao {

    /**
     * Observe all alerts for a given device.
     */
    @Query("SELECT * FROM alerts WHERE device_mac = :deviceMac ORDER BY id ASC")
    fun observeByDeviceMac(deviceMac: String): Flow<List<AlertEntity>>

    /**
     * Get a single alert by its ID.
     */
    @Query("SELECT * FROM alerts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AlertEntity?

    /**
     * Insert or update an alert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: AlertEntity): Long

    /**
     * Delete an alert by ID.
     */
    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Update the triggered state of an alert.
     */
    @Query("UPDATE alerts SET triggered = :triggered WHERE id = :id")
    suspend fun setTriggered(id: Long, triggered: Int)

    /**
     * Get all alerts with notifications enabled for a device.
     */
    @Query("SELECT * FROM alerts WHERE device_mac = :deviceMac AND notify = 1")
    suspend fun getActiveAlerts(deviceMac: String): List<AlertEntity>
}
