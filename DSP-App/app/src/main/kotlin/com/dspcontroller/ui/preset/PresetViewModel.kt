package com.dspcontroller.ui.preset
 
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dspcontroller.data.datastore.AppSettingsDataStore
import com.dspcontroller.domain.model.DspParam
import com.dspcontroller.domain.model.Preset
import com.dspcontroller.domain.usecase.LoadPresetUseCase
import com.dspcontroller.domain.usecase.SavePresetUseCase
import com.dspcontroller.domain.usecase.SyncPresetsUseCase
import com.dspcontroller.domain.repository.PresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State representing the preset sync operation.
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * ViewModel for the preset management screen.
 *
 * Manages loading, saving, syncing, and deleting presets.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PresetViewModel @Inject constructor(

    private val presetRepository: PresetRepository,
    private val loadPresetUseCase: LoadPresetUseCase,
    private val savePresetUseCase: SavePresetUseCase,
    private val syncPresetsUseCase: SyncPresetsUseCase,
    private val settingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val deviceMac: StateFlow<String> = settingsDataStore.lastConnectedMac.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    /** Stream of presets for the current device. */
    val presets: StateFlow<List<Preset>> = settingsDataStore.lastConnectedMac
        .flatMapLatest { mac ->
            if (mac.isEmpty()) flowOf(emptyList())
            else presetRepository.observePresetsForDevice(mac)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    /** Current sync operation state. */
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Load a preset on the connected device.
     */
    fun loadPreset(id: Int) {
        viewModelScope.launch {
            loadPresetUseCase(id)
                .onSuccess { name -> Timber.i("Preset loaded: %s", name) }
                .onFailure { Timber.e(it, "Failed to load preset %d", id) }
        }
    }

    /**
     * Save the current parameter state as a new preset.
     */
    fun savePreset(name: String, description: String, currentParams: List<DspParam> = emptyList()) {
        viewModelScope.launch {
            val mac = deviceMac.value
            if (mac.isEmpty()) return@launch

            val preset = Preset(
                deviceMac = mac,
                name = name,
                description = description,
                params = currentParams
            )

            savePresetUseCase(preset)
                .onSuccess { Timber.i("Preset saved: %s (id=%d)", name, it) }
                .onFailure { Timber.e(it, "Failed to save preset") }
        }
    }

    /**
     * Synchronize local and remote presets.
     */
    fun syncPresets() {
        viewModelScope.launch {
            val mac = deviceMac.value
            val ip = settingsDataStore.lastConnectedIp.first()
            if (mac.isEmpty() || ip.isEmpty()) return@launch

            _syncState.value = SyncState.Syncing
            syncPresetsUseCase(mac, ip)
                .onSuccess { _syncState.value = SyncState.Success }
                .onFailure { error ->
                    Timber.e(error, "Sync failed")
                    _syncState.value = SyncState.Error(error.message ?: "Sync failed")
                }
        }
    }

    /**
     * Delete a preset by ID.
     */
    fun deletePreset(id: Long) {
        viewModelScope.launch {
            presetRepository.deletePreset(id)
                .onFailure { Timber.e(it, "Failed to delete preset %d", id) }
        }
    }
}
