package com.devicexpert.app.presentation.battery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.battery.BatterySession
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.repository.BatterySessionRepository
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

data class BatteryDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val telemetry: AndroidTelemetry? = null,
    val livePowerHistoryWatts: List<Float> = emptyList(),
    val hasHistoryCharts: Boolean = false,
    val recentSessions: List<BatterySession> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class BatteryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val telemetryRepository: TelemetryRepository,
    private val batterySessionRepository: BatterySessionRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    val batteryLevelChartProducer = CartesianChartModelProducer()
    val batteryTempChartProducer = CartesianChartModelProducer()

    private val _uiState = MutableStateFlow(BatteryDetailUiState())
    val uiState: StateFlow<BatteryDetailUiState> = _uiState.asStateFlow()

    private val powerRingBuffer = ArrayDeque<Float>(40)
    private var lastSessionWriteMs = 0L
    private var lastChargingState: Boolean? = null

    init {
        observeLiveBattery()
        observeHistoricalData()
        observeSessions()
    }

    private fun observeLiveBattery() {
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
                    payload.batteryPowerWatts?.let { watts ->
                        if (powerRingBuffer.size >= 40) powerRingBuffer.removeFirst()
                        powerRingBuffer.addLast(watts)
                    }
                    _uiState.update { it.copy(telemetry = payload, livePowerHistoryWatts = powerRingBuffer.toList()) }

                    val stateFlipped = lastChargingState != null && lastChargingState != payload.isCharging
                    val shouldPersist = lastSessionWriteMs == 0L || stateFlipped || (snapshot.timestamp - lastSessionWriteMs) >= 60_000L
                    if (shouldPersist) {
                        lastSessionWriteMs = snapshot.timestamp
                        lastChargingState = payload.isCharging
                        viewModelScope.launch(Dispatchers.IO) {
                            batterySessionRepository.recordSample(
                                deviceId = device.id,
                                isCharging = payload.isCharging,
                                batteryPctInt = (payload.batteryPct * 100f).toInt(),
                                tempC = payload.batteryTempC,
                                currentMa = payload.batteryCurrentMa,
                                timestampMillis = snapshot.timestamp
                            )
                        }
                    }
                }
        }
    }

    private fun observeHistoricalData() {
        viewModelScope.launch(Dispatchers.IO) {
            val since = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
            telemetryRepository.observeTelemetryHistory(deviceId, since)
                .catch { }
                .collect { snapshots ->
                    val levelPoints = mutableListOf<Float>()
                    val tempPoints = mutableListOf<Float>()
                    snapshots.forEach { snap ->
                        val pl = snap.payload as? AndroidTelemetry ?: return@forEach
                        levelPoints.add(pl.batteryPct * 100f)
                        tempPoints.add(pl.batteryTempC)
                    }
                    if (levelPoints.size >= 2) {
                        batteryLevelChartProducer.runTransaction { lineSeries { series(levelPoints) } }
                        batteryTempChartProducer.runTransaction { lineSeries { series(tempPoints) } }
                        _uiState.update { it.copy(hasHistoryCharts = true) }
                    }
                }
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            batterySessionRepository.observeRecentSessions(deviceId, limit = 15)
                .catch { }
                .collect { sessions -> _uiState.update { it.copy(recentSessions = sessions) } }
        }
    }
}
