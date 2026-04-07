package com.dspcontroller.domain.model

/**
 * Represents the current state of the WebSocket connection to the DSP device.
 */
sealed class ConnectionState {
    /** No active connection. */
    data object Disconnected : ConnectionState()

    /** Currently attempting to establish a connection. */
    data object Connecting : ConnectionState()

    /** Connection is active and ready for communication. */
    data object Connected : ConnectionState()

    /**
     * Connection failed or was interrupted.
     *
     * @property cause The exception that caused the error, if available.
     */
    data class Error(val cause: Throwable? = null) : ConnectionState()
}
