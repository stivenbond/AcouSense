package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.Preset
import com.dspcontroller.domain.repository.PresetRepository

/**
 * Synchronizes presets between the local database and the ESP32 device.
 *
 * Compares checksums of local and remote presets:
 * - If a local preset has no remote match, it is pushed to the device.
 * - If a remote preset has no local match, it is pulled and saved locally.
 * - If checksums differ, the most recently updated version wins.
 *
 * @property presetRepository Repository for local and remote preset operations.
 */
class SyncPresetsUseCase(
    private val presetRepository: PresetRepository
) {

    /**
     * Perform a full bidirectional sync of presets for the given device.
     *
     * @param deviceMac The MAC address of the target device.
     * @param deviceIp The current IP address of the device for REST API calls.
     * @return [Result.success] with the merged list of presets,
     *         or [Result.failure] on network or database error.
     */
    suspend operator fun invoke(deviceMac: String, deviceIp: String): Result<List<Preset>> =
        runCatching {
            val remoteResult = presetRepository.fetchRemotePresets(deviceIp)
            val remotePresets = remoteResult.getOrThrow()

            val localPresets = mutableListOf<Preset>()

            // WHY: Collect the current local presets for comparison.
            // We use a snapshot rather than a Flow here because sync is a one-shot operation.
            presetRepository.observePresetsForDevice(deviceMac).collect { presets ->
                localPresets.addAll(presets)
                return@collect
            }

            val localChecksums = localPresets.associateBy { it.checksum }
            val remoteChecksums = remotePresets.associateBy { it.checksum }

            // Push local-only presets to device
            for (localPreset in localPresets) {
                if (localPreset.checksum !in remoteChecksums && !localPreset.synced) {
                    presetRepository.pushPresetToDevice(deviceIp, localPreset).getOrThrow()
                    presetRepository.markSynced(localPreset.id).getOrThrow()
                }
            }

            // Pull remote-only presets to local database
            for (remotePreset in remotePresets) {
                if (remotePreset.checksum !in localChecksums) {
                    val toSave = remotePreset.copy(
                        deviceMac = deviceMac,
                        synced = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    presetRepository.savePreset(toSave).getOrThrow()
                }
            }

            // Return the final merged list
            val finalList = mutableListOf<Preset>()
            presetRepository.observePresetsForDevice(deviceMac).collect { presets ->
                finalList.addAll(presets)
                return@collect
            }
            finalList
        }
}
