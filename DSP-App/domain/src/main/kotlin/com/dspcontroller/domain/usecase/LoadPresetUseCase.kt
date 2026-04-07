package com.dspcontroller.domain.usecase

import com.dspcontroller.domain.model.WsCommand
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Instructs the ESP32 device to load a preset by its ID.
 *
 * Sends the [WsCommand.LoadPreset] command and waits for confirmation
 * via [WsMessage.PresetLoaded].
 */
class LoadPresetUseCase(
    private val connectionRepository: ConnectionRepository
) {

    /**
     * Load a preset on the connected device.
     *
     * @param presetId The remote preset ID to load on the device.
     * @return [Result.success] with the loaded preset name, or failure on timeout/error.
     */
    suspend operator fun invoke(presetId: Int): Result<String> = runCatching {
        val sent = connectionRepository.sendCommand(WsCommand.LoadPreset(id = presetId))
        if (!sent) throw IllegalStateException("Failed to send load_preset command — not connected")

        // WHY: Wait up to 3 seconds for the device to confirm preset loading.
        val confirmation = withTimeoutOrNull(3_000L) {
            connectionRepository.inboundMessages.first { msg ->
                msg is WsMessage.PresetLoaded && msg.id == presetId
            }
        } as? WsMessage.PresetLoaded
            ?: throw IllegalStateException("Device did not confirm preset load within 3 seconds")

        confirmation.name
    }
}
