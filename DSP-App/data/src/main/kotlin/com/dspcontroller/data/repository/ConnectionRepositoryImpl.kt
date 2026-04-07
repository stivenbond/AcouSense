package com.dspcontroller.data.repository

import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.WsCommand
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.network.WebSocketManager
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ConnectionRepository] that delegates to [WebSocketManager].
 *
 * This adapter bridges the domain layer's interface with the network layer's
 * concrete WebSocket implementation.
 */
@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val webSocketManager: WebSocketManager
) : ConnectionRepository {

    override val connectionState: StateFlow<ConnectionState>
        get() = webSocketManager.connectionState

    override val inboundMessages: SharedFlow<WsMessage>
        get() = webSocketManager.inboundMessages

    override suspend fun connect(ip: String) {
        webSocketManager.connect(ip)
    }

    override suspend fun disconnect() {
        webSocketManager.disconnect()
    }

    override suspend fun sendCommand(command: WsCommand): Boolean {
        return webSocketManager.send(command)
    }
}
