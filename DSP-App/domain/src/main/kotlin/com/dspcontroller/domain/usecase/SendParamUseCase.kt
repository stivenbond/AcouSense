package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.WsCommand
import com.dspcontroller.domain.repository.ConnectionRepository

/**
 * Sends a DSP parameter change command to the connected device over WebSocket.
 *
 * This is a fire-and-forget operation; the caller should observe
 * [com.dspcontroller.domain.model.WsMessage.ParamAck] on the inbound message
 * stream to confirm the change was accepted.
 */
class SendParamUseCase(
    private val connectionRepository: ConnectionRepository
) {

    /**
     * Send a parameter update to the device.
     *
     * @param key The parameter identifier (e.g., "gain").
     * @param value The new value to set.
     * @return [Result.success] with `true` if the message was enqueued, `false` otherwise.
     */
    suspend operator fun invoke(key: String, value: Float): Result<Boolean> = runCatching {
        connectionRepository.sendCommand(WsCommand.SetParam(key = key, value = value))
    }
}
