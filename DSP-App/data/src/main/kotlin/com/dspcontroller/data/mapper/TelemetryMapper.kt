package com.dspcontroller.data.mapper

import com.dspcontroller.data.db.entity.TelemetryEntity
import com.dspcontroller.domain.model.Telemetry

/**
 * Extension functions for mapping between [TelemetryEntity] and domain [Telemetry].
 */

/** Convert a [TelemetryEntity] to a domain [Telemetry]. */
fun TelemetryEntity.toDomain(): Telemetry = Telemetry(
    id = id,
    deviceMac = deviceMac,
    cpuPercent = cpuPct,
    heapBytes = heapBytes,
    signalRms = signalRms,
    timestamp = ts
)

/** Convert a domain [Telemetry] to a [TelemetryEntity]. */
fun Telemetry.toEntity(): TelemetryEntity = TelemetryEntity(
    id = id,
    deviceMac = deviceMac,
    cpuPct = cpuPercent,
    heapBytes = heapBytes,
    signalRms = signalRms,
    ts = timestamp
)
