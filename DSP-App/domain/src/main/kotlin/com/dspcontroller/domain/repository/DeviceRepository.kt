package com.dspcontroller.domain.repository

import com.dspcontroller.domain.model.Device
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing DSP device records.
 *
 * All implementations must return [Result] wrappers to avoid throwing
 * exceptions through to the ViewModel layer.
 */
interface DeviceRepository {

    /**
     * Observe all saved devices as a reactive stream.
     */
    fun observeAllDevices(): Flow<List<Device>>

    /**
     * Retrieve a device by its unique MAC address.
     */
    suspend fun getDeviceByMac(mac: String): Result<Device?>

    /**
     * Insert or update a device record.
     * If a device with the same MAC already exists, it is updated.
     */
    suspend fun upsertDevice(device: Device): Result<Long>

    /**
     * Delete a device record by its MAC address.
     */
    suspend fun deleteDevice(mac: String): Result<Unit>

    /**
     * Update the last-seen timestamp and IP for a device.
     */
    suspend fun updateLastSeen(mac: String, ip: String, timestamp: Long): Result<Unit>
}
