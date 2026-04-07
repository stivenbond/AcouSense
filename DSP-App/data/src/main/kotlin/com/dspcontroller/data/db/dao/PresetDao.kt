package com.dspcontroller.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dspcontroller.data.db.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [PresetEntity] operations.
 */
@Dao
interface PresetDao {

    /**
     * Observe all presets for a given device MAC, ordered by updated time (newest first).
     */
    @Query("SELECT * FROM presets WHERE device_mac = :deviceMac ORDER BY updated_at DESC")
    fun observeByDeviceMac(deviceMac: String): Flow<List<PresetEntity>>

    /**
     * Get a single preset by its ID.
     */
    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PresetEntity?

    /**
     * Insert a new preset and return its generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity): Long

    /**
     * Delete a preset by ID. Associated [DspParamEntity] rows
     * are cascade-deleted by the foreign key constraint.
     */
    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Mark a preset as synced (synced = 1).
     */
    @Query("UPDATE presets SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    /**
     * Get all presets for a device MAC (non-reactive snapshot).
     */
    @Query("SELECT * FROM presets WHERE device_mac = :deviceMac")
    suspend fun getAllByDeviceMac(deviceMac: String): List<PresetEntity>
}
