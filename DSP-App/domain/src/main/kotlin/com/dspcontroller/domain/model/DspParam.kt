package com.dspcontroller.domain.model

/**
 * Represents a single DSP parameter with its current value and valid range.
 *
 * @property id Local database primary key.
 * @property presetId The ID of the preset this parameter belongs to.
 * @property key Machine-readable parameter identifier (e.g., "gain", "low_cutoff_hz").
 * @property value Current value of the parameter.
 * @property minVal Minimum allowed value for this parameter.
 * @property maxVal Maximum allowed value for this parameter.
 * @property unit Display unit string (e.g., "dB", "Hz", "%").
 * @property step Minimum increment step for the parameter slider.
 * @property updatedAt Epoch milliseconds of the last update.
 */
data class DspParam(
    val id: Long = 0L,
    val presetId: Long = 0L,
    val key: String,
    val value: Float,
    val minVal: Float,
    val maxVal: Float,
    val unit: String = "",
    val step: Float = 0.01f,
    val updatedAt: Long = System.currentTimeMillis()
)
