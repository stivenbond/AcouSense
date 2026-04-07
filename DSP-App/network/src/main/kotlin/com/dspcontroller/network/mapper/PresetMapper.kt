package com.dspcontroller.network.mapper

import com.dspcontroller.domain.model.DspParam
import com.dspcontroller.domain.model.Preset
import com.dspcontroller.network.dto.ParamEntryDto
import com.dspcontroller.network.dto.PresetDto

/**
 * Maps between REST API preset DTOs and domain-layer [Preset] models.
 */
object PresetMapper {

    /**
     * Convert a [PresetDto] (from REST API) to a domain [Preset].
     *
     * @param dto The DTO received from the device REST API.
     * @param deviceMac The MAC address of the device this preset belongs to.
     * @return A domain [Preset] with parameters populated.
     */
    fun toDomain(dto: PresetDto, deviceMac: String): Preset {
        val now = System.currentTimeMillis()
        return Preset(
            deviceMac = deviceMac,
            name = dto.name,
            description = dto.description,
            params = dto.params.map { entry ->
                DspParam(
                    key = entry.key,
                    value = entry.value,
                    minVal = entry.minVal,
                    maxVal = entry.maxVal,
                    unit = entry.unit,
                    step = entry.step
                )
            },
            synced = true,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Convert a domain [Preset] to a [PresetDto] for sending to the device REST API.
     *
     * @param preset The domain preset to convert.
     * @return A [PresetDto] ready for serialization and transmission.
     */
    fun toDto(preset: Preset): PresetDto {
        return PresetDto(
            id = preset.id.toInt(),
            name = preset.name,
            description = preset.description,
            params = preset.params.map { param ->
                ParamEntryDto(
                    key = param.key,
                    value = param.value,
                    minVal = param.minVal,
                    maxVal = param.maxVal,
                    unit = param.unit,
                    step = param.step
                )
            }
        )
    }
}
