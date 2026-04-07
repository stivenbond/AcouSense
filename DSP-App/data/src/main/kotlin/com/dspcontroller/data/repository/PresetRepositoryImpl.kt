package com.dspcontroller.data.repository

import com.dspcontroller.data.db.dao.DspParamDao
import com.dspcontroller.data.db.dao.PresetDao
import com.dspcontroller.data.mapper.toDomain
import com.dspcontroller.data.mapper.toEntity
import com.dspcontroller.domain.model.Preset
import com.dspcontroller.domain.repository.PresetRepository
import com.dspcontroller.network.RestApiService
import com.dspcontroller.network.RetrofitClient
import com.dspcontroller.network.mapper.PresetMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PresetRepository] backed by Room database and REST API.
 *
 * Handles local preset CRUD, checksum computation (SHA-256 of sorted params JSON),
 * and remote preset synchronization via the ESP32 REST API.
 */
@Singleton
class PresetRepositoryImpl @Inject constructor(
    private val presetDao: PresetDao,
    private val dspParamDao: DspParamDao,
    private val okHttpClient: OkHttpClient
) : PresetRepository {

    companion object {
        private const val TAG = "PresetRepositoryImpl"
    }

    private val json = Json { encodeDefaults = true }

    override fun observePresetsForDevice(deviceMac: String): Flow<List<Preset>> {
        return presetDao.observeByDeviceMac(deviceMac).map { presetEntities ->
            presetEntities.map { presetEntity ->
                val params = dspParamDao.getByPresetId(presetEntity.id).map { it.toDomain() }
                presetEntity.toDomain(params)
            }
        }
    }

    override suspend fun getPresetById(id: Long): Result<Preset?> = runCatching {
        val entity = presetDao.getById(id) ?: return@runCatching null
        val params = dspParamDao.getByPresetId(entity.id).map { it.toDomain() }
        entity.toDomain(params)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to get preset by ID: %d", id) }

    override suspend fun savePreset(preset: Preset): Result<Long> = runCatching {
        val checksum = computeChecksum(preset)
        val now = System.currentTimeMillis()
        val presetWithChecksum = preset.copy(
            checksum = checksum,
            updatedAt = now,
            createdAt = if (preset.id == 0L) now else preset.createdAt
        )

        val presetId = presetDao.insert(presetWithChecksum.toEntity())

        // WHY: Delete existing params before re-inserting to ensure we don't have stale entries
        // from a previous version of the preset.
        dspParamDao.deleteByPresetId(presetId)
        val paramEntities = presetWithChecksum.params.map { it.toEntity(presetId) }
        dspParamDao.insertAll(paramEntities)

        presetId
    }.onFailure { Timber.tag(TAG).e(it, "Failed to save preset: %s", preset.name) }

    override suspend fun deletePreset(id: Long): Result<Unit> = runCatching {
        // WHY: DspParamEntity has CASCADE delete on the FK, so deleting the preset
        // automatically removes all its parameters.
        presetDao.deleteById(id)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to delete preset: %d", id) }

    override suspend fun fetchRemotePresets(deviceIp: String): Result<List<Preset>> = runCatching {
        val api = createApiService(deviceIp)
        val dtos = api.getPresets()
        // WHY: We don't know the device MAC from the REST API alone,
        // so the caller must set it when saving locally.
        dtos.map { PresetMapper.toDomain(it, deviceMac = "") }
    }.onFailure { Timber.tag(TAG).e(it, "Failed to fetch remote presets from %s", deviceIp) }

    override suspend fun pushPresetToDevice(deviceIp: String, preset: Preset): Result<Preset> =
        runCatching {
            val api = createApiService(deviceIp)
            val dto = PresetMapper.toDto(preset)
            val savedDto = api.savePreset(dto)
            PresetMapper.toDomain(savedDto, preset.deviceMac)
        }.onFailure { Timber.tag(TAG).e(it, "Failed to push preset to %s", deviceIp) }

    override suspend fun markSynced(presetId: Long): Result<Unit> = runCatching {
        presetDao.markSynced(presetId)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to mark preset synced: %d", presetId) }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    /**
     * Compute SHA-256 checksum of sorted parameter JSON for sync comparison.
     *
     * WHY: Sorting by key ensures the same parameters in different order
     * produce the same checksum, enabling correct diff detection during sync.
     */
    private fun computeChecksum(preset: Preset): String {
        val sortedParams = preset.params
            .sortedBy { it.key }
            .map { mapOf("key" to it.key, "value" to it.value) }
        val jsonString = json.encodeToString(sortedParams)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(jsonString.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun createApiService(deviceIp: String): RestApiService {
        return RetrofitClient.create(okHttpClient, deviceIp)
    }
}
