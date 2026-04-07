package com.dspcontroller.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-defined metric alert threshold.
 */
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "device_mac")
    val deviceMac: String,

    @ColumnInfo(name = "metric")
    val metric: String,

    @ColumnInfo(name = "condition")
    val condition: String,

    @ColumnInfo(name = "threshold")
    val threshold: Float,

    @ColumnInfo(name = "notify")
    val notify: Int = 1,

    @ColumnInfo(name = "triggered")
    val triggered: Int = 0
)
