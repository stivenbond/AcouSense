package com.dspcontroller.data.repository

import com.dspcontroller.data.db.dao.TelemetryDao
import com.dspcontroller.data.mapper.toDomain
import com.dspcontroller.data.mapper.toEntity
import com.dspcontroller.domain.model.Telemetry
import com.dspcontroller.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [TelemetryRepository] backed by Room database.
 *
 * Enforces a 10,000-row cap per device to prevent unbounded storage growth.
 */
@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val telemetryDao: TelemetryDao
) : TelemetryRepository {

    companion object {
        private const val TAG = "TelemetryRepoImpl"
        private const val MAX_ROWS_PER_DEVICE = 10_000
    }

    override fun observeTelemetry(deviceMac: String, windowMs: Long): Flow<List<Telemetry>> {
        val sinceTs = System.currentTimeMillis() - windowMs
        return telemetryDao.observeWindow(deviceMac, sinceTs).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertTelemetry(telemetry: Telemetry): Result<Unit> = runCatching {
        telemetryDao.insert(telemetry.toEntity())

        // WHY: Enforce the row cap by deleting excess oldest rows after each insert.
        // This is cheaper than checking before every insert because overflow is rare.
        val count = telemetryDao.countByDevice(telemetry.deviceMac)
        if (count > MAX_ROWS_PER_DEVICE) {
            val excess = count - MAX_ROWS_PER_DEVICE
            telemetryDao.deleteOldest(telemetry.deviceMac, excess)
            Timber.tag(TAG).d("Pruned %d excess telemetry rows for %s", excess, telemetry.deviceMac)
        }
    }.onFailure { Timber.tag(TAG).e(it, "Failed to insert telemetry") }

    override suspend fun purgeOlderThan(olderThanMs: Long): Result<Int> = runCatching {
        val deleted = telemetryDao.deleteOlderThan(olderThanMs)
        Timber.tag(TAG).i("Purged %d telemetry rows older than %d", deleted, olderThanMs)
        deleted
    }.onFailure { Timber.tag(TAG).e(it, "Failed to purge old telemetry") }

    override suspend fun getCount(deviceMac: String): Result<Int> = runCatching {
        telemetryDao.countByDevice(deviceMac)
    }.onFailure { Timber.tag(TAG).e(it, "Failed to get telemetry count for %s", deviceMac) }
}
