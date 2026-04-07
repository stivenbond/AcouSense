package com.dspcontroller.domain.repository

import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.WsCommand
import com.dspcontroller.domain.model.WsMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface abstracting the WebSocket communication layer.
 *
 * This allows the domain layer to interact with the WebSocket without
 * depending on OkHttp or any Android-specific network library.
 */
interface ConnectionRepository {

    /**
     * Current WebSocket connection state.
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Stream of inbound messages from the device.
     */
    val inboundMessages: SharedFlow<WsMessage>

    /**
     * Establish a WebSocket connection to the device at the given IP.
     */
    suspend fun connect(ip: String)

    /**
     * Gracefully close the WebSocket connection.
     */
    suspend fun disconnect()

    /**
     * Send a command to the device over the WebSocket.
     *
     * @return true if the message was successfully enqueued for sending.
     */
    suspend fun sendCommand(command: WsCommand): Boolean
}
