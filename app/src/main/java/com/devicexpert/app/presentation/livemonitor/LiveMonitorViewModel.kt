package com.devicexpert.app.presentation.livemonitor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import com.devicexpert.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveMonitorUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val latestSnapshot: TelemetrySnapshot? = null,
    val error: String? = null
)

@HiltViewModel
class LiveMonitorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    private val _uiState = MutableStateFlow(LiveMonitorUiState())
    val uiState: StateFlow<LiveMonitorUiState> = _uiState.asStateFlow()

    init { startMonitoring() }

    private fun startMonitoring() {
        viewModelScope.launch {
            val device = deviceRepository.getDeviceById(deviceId)
            if (device == null) {
                _uiState.update { it.copy(isLoading = false, error = "Device not found") }
                return@launch
            }
            _uiState.update { it.copy(device = device, isLoading = false) }
            val connector = connectorRegistry.getConnector(device.category)
            connector.observeLiveTelemetry(device, intervalMs = 1000L)
                .catch { e -> _uiState.update { it.copy(error = e.localizedMessage) } }
                .collect { snapshot -> _uiState.update { it.copy(latestSnapshot = snapshot) } }
        }
    }
}
