package com.dspcontroller.ui.parameter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dspcontroller.domain.model.DspParam
import com.dspcontroller.domain.model.WsCommand
import com.dspcontroller.domain.model.WsMessage
import com.dspcontroller.domain.repository.ConnectionRepository
import com.dspcontroller.domain.usecase.SendParamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State representing the outcome of a parameter send operation.
 */
sealed class SendState {
    data object Idle : SendState()
    data object Sending : SendState()
    data class Ack(val key: String) : SendState()
    data class Error(val message: String) : SendState()
}

/**
 * ViewModel for the parameter control screen.
 *
 * Manages parameter state, sends changes to the device with 150ms debounce,
 * and processes acknowledgements.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ParameterViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val sendParamUseCase: SendParamUseCase
) : ViewModel() {

    private val _params = MutableStateFlow<List<DspParam>>(emptyList())

    /** Current list of DSP parameters. */
    val params: StateFlow<List<DspParam>> = _params.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)

    /** Current send operation state. */
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    // WHY: MutableSharedFlow acts as a channel for debouncing slider changes.
    private val paramChangeFlow = MutableSharedFlow<Pair<String, Float>>(extraBufferCapacity = 64)

    init {
        collectAcks()
        collectParamsDump()
        setupDebounce()
    }

    /**
     * Called when a slider value changes. Debounced 150ms before sending.
     */
    fun onParamChanged(key: String, value: Float) {
        // Update local state immediately for responsive UI
        _params.value = _params.value.map { param ->
            if (param.key == key) param.copy(value = value) else param
        }
        paramChangeFlow.tryEmit(key to value)
    }

    /**
     * Request a full parameter dump from the device.
     */
    fun requestParamDump() {
        viewModelScope.launch {
            connectionRepository.sendCommand(WsCommand.GetParams)
        }
    }

    private fun setupDebounce() {
        viewModelScope.launch {
            // WHY: 150ms debounce prevents flooding the ESP32 with rapid slider changes.
            paramChangeFlow
                .debounce(150)
                .collect { (key, value) ->
                    _sendState.value = SendState.Sending
                    sendParamUseCase(key, value)
                        .onSuccess { sent ->
                            if (!sent) {
                                _sendState.value = SendState.Error("Not connected")
                            }
                        }
                        .onFailure { error ->
                            Timber.e(error, "Failed to send param %s = %f", key, value)
                            _sendState.value = SendState.Error(error.message ?: "Send failed")
                        }
                }
        }
    }

    private fun collectAcks() {
        viewModelScope.launch {
            connectionRepository.inboundMessages.collect { message ->
                if (message is WsMessage.ParamAck) {
                    if (message.success) {
                        _sendState.value = SendState.Ack(message.key)
                    } else {
                        _sendState.value = SendState.Error("Device rejected: ${message.key}")
                    }
                }
            }
        }
    }

    private fun collectParamsDump() {
        viewModelScope.launch {
            connectionRepository.inboundMessages.collect { message ->
                if (message is WsMessage.ParamsDump) {
                    _params.value = message.params
                }
            }
        }
    }
}
