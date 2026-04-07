package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.Preset
import com.dspcontroller.domain.repository.PresetRepository

/**
 * Saves a DSP preset (with all its parameters) to the local Room database.
 *
 * The repository implementation is responsible for computing the SHA-256
 * checksum from the sorted parameter JSON.
 */
class SavePresetUseCase(
    private val presetRepository: PresetRepository
) {

    /**
     * Save a preset locally.
     *
     * @param preset The preset to save, including its parameter list.
     * @return [Result.success] with the inserted/updated preset ID.
     */
    suspend operator fun invoke(preset: Preset): Result<Long> {
        return presetRepository.savePreset(preset)
    }
}
