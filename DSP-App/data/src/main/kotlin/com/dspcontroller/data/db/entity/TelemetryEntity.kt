package com.dspcontroller.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single telemetry snapshot from a DSP device.
 *
 * Indexed on (device_mac, ts) for efficient windowed queries.
 * Purge policy: delete rows older than 7 days on app start.
 * Row cap: 10,000 rows maximum per device.
 */
@Entity(
    tableName = "telemetry",
    indices = [Index(value = ["device_mac", "ts"])]
)
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "device_mac")
    val deviceMac: String,

    @ColumnInfo(name = "cpu_pct")
    val cpuPct: Int,

    @ColumnInfo(name = "heap_bytes")
    val heapBytes: Long,

    @ColumnInfo(name = "signal_rms")
    val signalRms: Float,

    @ColumnInfo(name = "ts")
    val ts: Long
)
