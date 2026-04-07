package com.dspcontroller.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved DSP preset configuration.
 *
 * Presets are scoped to a device via [deviceMac]. The [checksum] field stores
 * a SHA-256 hash of the sorted parameter JSON for sync comparison.
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "device_mac")
    val deviceMac: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "checksum")
    val checksum: String,

    @ColumnInfo(name = "synced")
    val synced: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
