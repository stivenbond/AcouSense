package com.dspcontroller.data.mapper

import com.dspcontroller.data.db.entity.DspParamEntity
import com.dspcontroller.data.db.entity.PresetEntity
import com.dspcontroller.domain.model.DspParam
import com.dspcontroller.domain.model.Preset

/**
 * Extension functions for mapping between preset/param entities and domain models.
 */

/** Convert a [PresetEntity] to a domain [Preset], with parameters populated separately. */
fun PresetEntity.toDomain(params: List<DspParam> = emptyList()): Preset = Preset(
    id = id,
    deviceMac = deviceMac,
    name = name,
    description = description,
    params = params,
    checksum = checksum,
    synced = synced == 1,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Convert a domain [Preset] to a [PresetEntity]. */
fun Preset.toEntity(): PresetEntity = PresetEntity(
    id = id,
    deviceMac = deviceMac,
    name = name,
    description = description,
    checksum = checksum,
    synced = if (synced) 1 else 0,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Convert a [DspParamEntity] to a domain [DspParam]. */
fun DspParamEntity.toDomain(): DspParam = DspParam(
    id = id,
    presetId = presetId,
    key = key,
    value = value,
    minVal = minVal,
    maxVal = maxVal,
    unit = unit,
    step = step,
    updatedAt = updatedAt
)

/** Convert a domain [DspParam] to a [DspParamEntity] for a given preset ID. */
fun DspParam.toEntity(presetId: Long): DspParamEntity = DspParamEntity(
    id = id,
    presetId = presetId,
    key = key,
    value = value,
    minVal = minVal,
    maxVal = maxVal,
    unit = unit,
    step = step,
    updatedAt = updatedAt
)
