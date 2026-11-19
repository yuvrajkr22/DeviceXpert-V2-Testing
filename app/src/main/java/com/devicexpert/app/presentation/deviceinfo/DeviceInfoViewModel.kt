package com.devicexpert.app.presentation.deviceinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.model.telemetry.IotTelemetry
import com.devicexpert.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceInfoUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val uptimeMillis: Long = 0L,
    val awakeUptimeMillis: Long = 0L,
    val deepSleepMillis: Long = 0L,
    val deepSleepPct: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class DeviceInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    init { loadDeviceAndUptime() }

    private fun loadDeviceAndUptime() {
        viewModelScope.launch {
            val device = deviceRepository.getDeviceById(deviceId)
            if (device == null) {
                _uiState.update { it.copy(isLoading = false, error = "Device not found") }
                return@launch
            }
            _uiState.update { it.copy(device = device, isLoading = false) }
            val connector = connectorRegistry.getConnector(device.category)
            connector.observeLiveTelemetry(device, intervalMs = 2000L).catch { }.collect { snapshot ->
                when (val pl = snapshot.payload) {
                    is AndroidTelemetry -> {
                        val deepSleep = (pl.uptimeMillis - pl.awakeUptimeMillis).coerceAtLeast(0L)
                        val sleepPct = if (pl.uptimeMillis > 0L) (deepSleep.toFloat() / pl.uptimeMillis.toFloat()).coerceIn(0f, 1f) else 0f
                        _uiState.update { it.copy(uptimeMillis = pl.uptimeMillis, awakeUptimeMillis = pl.awakeUptimeMillis, deepSleepMillis = deepSleep, deepSleepPct = sleepPct) }
                    }
                    is ComputerTelemetry -> _uiState.update { it.copy(uptimeMillis = pl.uptimeSeconds * 1000L) }
                    is IotTelemetry -> _uiState.update { it.copy(uptimeMillis = pl.uptimeSeconds * 1000L) }
                }
            }
        }
    }
}
