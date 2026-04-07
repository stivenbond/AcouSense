package com.dspcontroller.network.mapper

import com.dspcontroller.domain.model.DspParam
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.network.dto.WsMessageDto

/**
 * Maps inbound WebSocket DTOs to domain model [WsMessage] instances.
 */
object WsMessageMapper {

    /**
     * Convert a [WsMessageDto] to its domain-layer [WsMessage] equivalent.
     */
    fun toDomain(dto: WsMessageDto): WsMessage = when (dto) {
        is WsMessageDto.TelemetryDto -> WsMessage.TelemetryMessage(
            cpu = dto.cpu,
            heap = dto.heap,
            signalRms = dto.signalRms,
            timestamp = dto.ts
        )

        is WsMessageDto.ParamAckDto -> WsMessage.ParamAck(
            key = dto.key,
            value = dto.value,
            success = dto.success
        )

        is WsMessageDto.ParamsDumpDto -> WsMessage.ParamsDump(
            params = dto.params.map { entry ->
                DspParam(
                    key = entry.key,
                    value = entry.value,
                    minVal = entry.minVal,
                    maxVal = entry.maxVal,
                    unit = entry.unit,
                    step = entry.step
                )
            }
        )

        is WsMessageDto.PresetLoadedDto -> WsMessage.PresetLoaded(
            id = dto.id,
            name = dto.name
        )

        is WsMessageDto.DeviceInfoMessageDto -> WsMessage.DeviceInfo(
            mac = dto.mac,
            firmware = dto.firmware,
            uptimeSeconds = dto.uptimeSeconds
        )

        is WsMessageDto.ErrorDto -> WsMessage.Error(
            code = dto.code,
            message = dto.message
        )
    }
}
