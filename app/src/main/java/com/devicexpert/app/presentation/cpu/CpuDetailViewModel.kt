package com.devicexpert.app.presentation.cpu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.model.telemetry.TelemetryPayload
import com.devicexpert.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CpuDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val payload: TelemetryPayload? = null,
    val liveLoadHistory: List<Float> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CpuDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    private val _uiState = MutableStateFlow(CpuDetailUiState())
    val uiState: StateFlow<CpuDetailUiState> = _uiState.asStateFlow()
    private val loadRingBuffer = ArrayDeque<Float>(60)

    init { observeCpuStream() }

    private fun observeCpuStream() {
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
                .collect { snapshot ->
                    val sample = when (val pl = snapshot.payload) {
                        is AndroidTelemetry -> (pl.cpuFrequencyLoadPct ?: 0f) * 100f
                        is ComputerTelemetry -> (pl.cpuUsagePct * 100f).coerceIn(0f, 100f)
                        else -> null
                    }
                    if (sample != null) {
                        if (loadRingBuffer.size >= 60) loadRingBuffer.removeFirst()
                        loadRingBuffer.addLast(sample)
                    }
                    _uiState.update { it.copy(payload = snapshot.payload, liveLoadHistory = loadRingBuffer.toList()) }
                }
        }
    }
}
