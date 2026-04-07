package com.dspcontroller.data.repository

import com.dspcontroller.data.db.dao.AlertDao
import com.dspcontroller.data.mapper.toDomain
import com.dspcontroller.data.mapper.toEntity
import com.dspcontroller.domain.model.Alert
import com.dspcontroller.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [AlertRepository] backed by Room database.
 */
@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao
) : AlertRepository {

    companion object {
        private const val TAG = "AlertRepositoryImpl"
    }

    override fun observeAlerts(deviceMac: String): Flow<List<Alert>> {
        return alertDao.observeByDeviceMac(deviceMac).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAlertById(id: Long): Result<Alert?> = runCatching {
        alertDao.getById(id)?.toDomain()
    }.onFailure { Timber.tag(TAG).e(it, "Failed to get alert by ID: %d", id) }

    override suspend fun upsertAlert(alert: Alert): Result<Long> = runCatching {
        alertDao.upsert(alert.toEntity())
    }.onFailure { Timber.tag(TAG).e(it, "Failed to upsert alert") }

    override suspend fun deleteAlert(id: Long): Result<Unit> = runCatching {
        alertDao.deleteById(id)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to delete alert: %d", id) }

    override suspend fun setTriggered(id: Long, triggered: Boolean): Result<Unit> = runCatching {
        alertDao.setTriggered(id, if (triggered) 1 else 0)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to set triggered state for alert: %d", id) }

    override suspend fun getActiveAlerts(deviceMac: String): Result<List<Alert>> = runCatching {
        alertDao.getActiveAlerts(deviceMac).map { it.toDomain() }
    }.onFailure { Timber.tag(TAG).e(it, "Failed to get active alerts for: %s", deviceMac) }
}
