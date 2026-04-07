package com.dspcontroller.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dspcontroller.domain.model.ConnectionState
import com.dspcontroller.domain.model.DiscoveredDevice
import com.dspcontroller.domain.usecase.ConnectToDeviceUseCase
import com.dspcontroller.network.NsdDiscoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the device discovery screen.
 *
 * Manages mDNS device scanning and manual connection to devices.
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val nsdDiscoveryManager: NsdDiscoveryManager,
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val connectionRepositoryLazy: com.dspcontroller.domain.repository.ConnectionRepository
) : ViewModel() {

    /** Stream of discovered devices on the local network. */
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> =
        nsdDiscoveryManager.discoveredDevices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Current WebSocket connection state. */
    val connectionState: StateFlow<ConnectionState> = connectionRepositoryLazy.connectionState

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Start mDNS device discovery. */
    fun startDiscovery() {
        _isScanning.value = true
        nsdDiscoveryManager.startDiscovery()
    }

    /** Stop mDNS device discovery. */
    fun stopDiscovery() {
        _isScanning.value = false
        nsdDiscoveryManager.stopDiscovery()
    }

    /**
     * Attempt to connect to a device at the given IP address.
     *
     * @param ip The local network IP address of the ESP32 device.
     */
    fun connectToDevice(ip: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = connectToDeviceUseCase(ip)
            result.onFailure { error ->
                Timber.e(error, "Connection failed to %s", ip)
                _errorMessage.value = error.message ?: "Connection failed"
            }
        }
    }

    /** Clear the current error message. */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        nsdDiscoveryManager.stopDiscovery()
    }
}
