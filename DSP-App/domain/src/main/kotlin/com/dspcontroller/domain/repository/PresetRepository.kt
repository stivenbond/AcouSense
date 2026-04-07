package com.dspcontroller.domain.repository

import com.dspcontroller.domain.model.Preset
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing DSP presets and their parameters.
 *
 * Presets are scoped to a specific device identified by MAC address.
 */
interface PresetRepository {

    /**
     * Observe all presets for a given device as a reactive stream.
     */
    fun observePresetsForDevice(deviceMac: String): Flow<List<Preset>>

    /**
     * Get a single preset by ID, including all its parameters.
     */
    suspend fun getPresetById(id: Long): Result<Preset?>

    /**
     * Save a preset with all its parameters.
     * Computes the SHA-256 checksum from the sorted params JSON.
     *
     * @return The ID of the inserted or updated preset.
     */
    suspend fun savePreset(preset: Preset): Result<Long>

    /**
     * Delete a preset and all its associated parameters (cascading).
     */
    suspend fun deletePreset(id: Long): Result<Unit>

    /**
     * Retrieve all presets from the ESP32 device via REST API.
     */
    suspend fun fetchRemotePresets(deviceIp: String): Result<List<Preset>>

    /**
     * Push a local preset to the ESP32 device via REST API.
     */
    suspend fun pushPresetToDevice(deviceIp: String, preset: Preset): Result<Preset>

    /**
     * Mark a preset as synced after successful push/pull.
     */
    suspend fun markSynced(presetId: Long): Result<Unit>
}
