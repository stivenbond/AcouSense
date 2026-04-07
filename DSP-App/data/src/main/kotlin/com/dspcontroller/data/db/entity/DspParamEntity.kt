package com.dspcontroller.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single DSP parameter within a preset.
 *
 * Has a foreign key relationship with [PresetEntity] that cascades on delete,
 * ensuring all parameters are removed when their parent preset is deleted.
 */
@Entity(
    tableName = "dsp_params",
    foreignKeys = [
        ForeignKey(
            entity = PresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["preset_id", "key"], unique = true),
        Index(value = ["preset_id"])
    ]
)
data class DspParamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "preset_id")
    val presetId: Long,

    @ColumnInfo(name = "key")
    val key: String,

    @ColumnInfo(name = "value")
    val value: Float,

    @ColumnInfo(name = "min_val")
    val minVal: Float,

    @ColumnInfo(name = "max_val")
    val maxVal: Float,

    @ColumnInfo(name = "unit")
    val unit: String = "",

    @ColumnInfo(name = "step")
    val step: Float = 0.01f,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
