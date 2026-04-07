package com.dspcontroller.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dspcontroller.data.db.entity.DspParamEntity

/**
 * Data Access Object for [DspParamEntity] operations.
 */
@Dao
interface DspParamDao {

    /**
     * Get all parameters for a given preset ID.
     */
    @Query("SELECT * FROM dsp_params WHERE preset_id = :presetId ORDER BY key ASC")
    suspend fun getByPresetId(presetId: Long): List<DspParamEntity>

    /**
     * Insert a list of parameters in a single transaction.
     * Uses REPLACE to handle upsert on the (preset_id, key) unique constraint.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(params: List<DspParamEntity>)

    /**
     * Delete all parameters for a given preset ID.
     * Typically used before re-inserting an updated parameter set.
     */
    @Query("DELETE FROM dsp_params WHERE preset_id = :presetId")
    suspend fun deleteByPresetId(presetId: Long)
}
