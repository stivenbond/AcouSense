package com.dspcontroller.data.repository

import com.dspcontroller.data.db.dao.DeviceDao
import com.dspcontroller.data.mapper.toDomain
import com.dspcontroller.data.mapper.toEntity
import com.dspcontroller.domain.model.Device
import com.dspcontroller.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DeviceRepository] backed by Room database.
 *
 * All public functions return [Result] to prevent exceptions from
 * propagating to the ViewModel layer.
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    companion object {
        private const val TAG = "DeviceRepositoryImpl"
    }

    override fun observeAllDevices(): Flow<List<Device>> {
        return deviceDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDeviceByMac(mac: String): Result<Device?> = runCatching {
        deviceDao.getByMac(mac)?.toDomain()
    }.onFailure { Timber.tag(TAG).e(it, "Failed to get device by MAC: %s", mac) }

    override suspend fun upsertDevice(device: Device): Result<Long> = runCatching {
        deviceDao.upsert(device.toEntity())
    }.onFailure { Timber.tag(TAG).e(it, "Failed to upsert device: %s", device.mac) }

    override suspend fun deleteDevice(mac: String): Result<Unit> = runCatching {
        deviceDao.deleteByMac(mac)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to delete device: %s", mac) }

    override suspend fun updateLastSeen(mac: String, ip: String, timestamp: Long): Result<Unit> =
        runCatching {
            deviceDao.updateLastSeen(mac, ip, timestamp)
        }.onFailure { Timber.tag(TAG).e(it, "Failed to update last seen: %s", mac) }
}
