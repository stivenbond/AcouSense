package com.dspcontroller.network

import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.WsCommand
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.network.dto.WsCommandDto
import com.dspcontroller.network.dto.WsMessageDto
import com.dspcontroller.network.mapper.WsMessageMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Manages the OkHttp WebSocket connection to the ESP32 DSP device.
 *
 * Features:
 * - Exposes [connectionState] as a [StateFlow] for UI observation.
 * - Exposes [inboundMessages] as a [SharedFlow] for domain-layer consumption.
 * - Auto-reconnect with exponential backoff (max 30 seconds).
 * - Heartbeat: sends a ping every 10 seconds while connected.
 * - Thread-safe via [Mutex].
 *
 * Paper Reference: Custom real-time IoT communication layer for DSP control.
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val TAG = "WebSocketManager"
        private const val HEARTBEAT_INTERVAL_MS = 10_000L
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val RECONNECT_BACKOFF_MULTIPLIER = 2.0
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private var webSocket: WebSocket? = null
    private var currentIp: String? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var shouldReconnect = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /** Current WebSocket connection state. */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _inboundMessages = MutableSharedFlow<WsMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Stream of parsed inbound messages from the device. */
    val inboundMessages: SharedFlow<WsMessage> = _inboundMessages.asSharedFlow()

    /**
     * Establish a WebSocket connection to the device at the given IP address.
     *
     * If already connected, the existing connection is closed first.
     *
     * @param ip The local network IP address of the ESP32 device.
     */
    suspend fun connect(ip: String) = mutex.withLock {
        Timber.tag(TAG).i("Connecting to ws://%s/ws", ip)
        disconnectInternal()

        currentIp = ip
        shouldReconnect = true
        reconnectAttempt = 0
        _connectionState.value = ConnectionState.Connecting

        openWebSocket(ip)
    }

    /**
     * Gracefully close the WebSocket connection and stop auto-reconnect.
     */
    suspend fun disconnect() = mutex.withLock {
        Timber.tag(TAG).i("Disconnecting (user-initiated)")
        shouldReconnect = false
        disconnectInternal()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Send a command to the device over the open WebSocket.
     *
     * @param command The [WsCommand] to serialize and send.
     * @return `true` if the message was successfully enqueued, `false` otherwise.
     */
    suspend fun send(command: WsCommand): Boolean = mutex.withLock {
        val ws = webSocket
        if (ws == null || _connectionState.value !is ConnectionState.Connected) {
            Timber.tag(TAG).w("Cannot send — not connected")
            return false
        }

        val jsonString = when (command) {
            is WsCommand.SetParam -> json.encodeToString(
                WsCommandDto.SetParamDto.serializer(),
                WsCommandDto.SetParamDto(key = command.key, value = command.value)
            )
            is WsCommand.LoadPreset -> json.encodeToString(
                WsCommandDto.LoadPresetDto.serializer(),
                WsCommandDto.LoadPresetDto(id = command.id)
            )
            is WsCommand.Ping -> json.encodeToString(
                WsCommandDto.PingDto.serializer(),
                WsCommandDto.PingDto()
            )
            is WsCommand.GetParams -> json.encodeToString(
                WsCommandDto.GetParamsDto.serializer(),
                WsCommandDto.GetParamsDto()
            )
        }

        val sent = ws.send(jsonString)
        Timber.tag(TAG).d("Sent: %s → success=%b", jsonString, sent)
        sent
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    private fun openWebSocket(ip: String) {
        val request = Request.Builder()
            .url("ws://$ip/ws")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.tag(TAG).i("WebSocket opened: %s", response.message)
                _connectionState.value = ConnectionState.Connected
                reconnectAttempt = 0
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.tag(TAG).d("Received: %s", text)
                try {
                    val dto = json.decodeFromString(WsMessageDto.serializer(), text)
                    val message = WsMessageMapper.toDomain(dto)
                    val emitted = _inboundMessages.tryEmit(message)
                    if (!emitted) {
                        Timber.tag(TAG).w("Inbound message buffer full — dropping message")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to parse inbound message: %s", text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.tag(TAG).i("WebSocket closing: code=%d reason=%s", code, reason)
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.tag(TAG).i("WebSocket closed: code=%d reason=%s", code, reason)
                stopHeartbeat()
                if (shouldReconnect) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.tag(TAG).e(t, "WebSocket failure: %s", response?.message)
                stopHeartbeat()
                _connectionState.value = ConnectionState.Error(t)
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun disconnectInternal() {
        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                val ws = webSocket
                if (ws != null && _connectionState.value is ConnectionState.Connected) {
                    val pingJson = json.encodeToString(
                        WsCommandDto.PingDto.serializer(),
                        WsCommandDto.PingDto()
                    )
                    ws.send(pingJson)
                    Timber.tag(TAG).d("Heartbeat ping sent")
                } else {
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            // WHY: Exponential backoff prevents hammering a potentially overloaded device.
            val delayMs = min(
                (INITIAL_RECONNECT_DELAY_MS * Math.pow(RECONNECT_BACKOFF_MULTIPLIER, reconnectAttempt.toDouble())).toLong(),
                MAX_RECONNECT_DELAY_MS
            )
            reconnectAttempt++
            Timber.tag(TAG).i("Reconnecting in %dms (attempt %d)", delayMs, reconnectAttempt)
            _connectionState.value = ConnectionState.Connecting
            delay(delayMs)

            val ip = currentIp ?: return@launch
            openWebSocket(ip)
        }
    }
}
