package com.devicexpert.app.presentation.thermal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.repository.DeviceRepository
import com.devicexpert.app.domain.repository.TelemetryRepository
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThermalDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val telemetry: AndroidTelemetry? = null,
    val liveTempHistory: List<Float> = emptyList(),
    val hasHistoryChart: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ThermalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val telemetryRepository: TelemetryRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    val thermalHistoryProducer = CartesianChartModelProducer()
    private val _uiState = MutableStateFlow(ThermalDetailUiState())
    val uiState: StateFlow<ThermalDetailUiState> = _uiState.asStateFlow()
    private val tempRingBuffer = ArrayDeque<Float>(60)

    init {
        observeLiveThermal()
        observeThermalHistory()
    }

    private fun observeLiveThermal() {
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
                    val payload = snapshot.payload as? AndroidTelemetry ?: return@collect
                    if (tempRingBuffer.size >= 60) tempRingBuffer.removeFirst()
                    tempRingBuffer.addLast(payload.batteryTempC)
                    _uiState.update { it.copy(telemetry = payload, liveTempHistory = tempRingBuffer.toList()) }
                }
        }
    }

    private fun observeThermalHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val since = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
            telemetryRepository.observeTelemetryHistory(deviceId, since)
                .catch { }
                .collect { snapshots ->
                    val points = snapshots.mapNotNull { (it.payload as? AndroidTelemetry)?.batteryTempC }
                    if (points.size >= 2) {
                        thermalHistoryProducer.runTransaction { lineSeries { series(points) } }
                        _uiState.update { it.copy(hasHistoryChart = true) }
                    }
                }
        }
    }
}
