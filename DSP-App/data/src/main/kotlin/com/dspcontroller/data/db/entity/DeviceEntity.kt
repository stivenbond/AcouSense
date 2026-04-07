package com.dspcontroller.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a DSP device record.
 *
 * The [mac] column has a unique constraint to prevent duplicate device entries.
 */
@Entity(
    tableName = "devices",
    indices = [Index(value = ["mac"], unique = true)]
)
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "mac")
    val mac: String,

    @ColumnInfo(name = "label")
    val label: String = "",

    @ColumnInfo(name = "ip_last")
    val ipLast: String,

    @ColumnInfo(name = "firmware")
    val firmware: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long
)
