package com.devicexpert.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.manager.ActiveDeviceManager
import com.devicexpert.app.domain.model.alert.AlertSeverity
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import com.devicexpert.app.domain.repository.AlertRepository
import com.devicexpert.app.domain.repository.DeviceRepository
import com.devicexpert.app.domain.repository.SensorRepository
import com.devicexpert.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HealthBannerState { HEALTHY, WARNING, CRITICAL }

data class DashboardUiState(
    val isLoading: Boolean = true,
    val activeDevice: UnifiedDevice? = null,
    val allDevices: List<UnifiedDevice> = emptyList(),
    val latestSnapshot: TelemetrySnapshot? = null,
    val isLivePaused: Boolean = false,
    val cpuLoadHistory: List<Float> = emptyList(),
    val rxSpeedHistory: List<Float> = emptyList(),
    val txSpeedHistory: List<Float> = emptyList(),
    val compassAzimuthDeg: Float? = null,
    val activeAlertCount: Int = 0,
    val healthState: HealthBannerState = HealthBannerState.HEALTHY,
    val streamError: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val activeDeviceManager: ActiveDeviceManager,
    private val deviceRepository: DeviceRepository,
    private val alertRepository: AlertRepository,
    private val sensorRepository: SensorRepository,
    private val settingsRepository: SettingsRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val _isLivePaused = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val cpuRingBuffer = ArrayDeque<Float>(MAX_SPARKLINE_POINTS)
    private val rxRingBuffer = ArrayDeque<Float>(MAX_SPARKLINE_POINTS)
    private val txRingBuffer = ArrayDeque<Float>(MAX_SPARKLINE_POINTS)
    private var lastDeviceId: String? = null

    init {
        observeDevicesAndAlerts()
        observeLiveTelemetryStream()
        observeLocalCompass()
    }

    fun selectDevice(deviceId: String) = activeDeviceManager.selectDevice(deviceId)

    fun toggleLivePause() {
        _isLivePaused.update { !it }
        _uiState.update { it.copy(isLivePaused = _isLivePaused.value) }
    }

    private fun observeDevicesAndAlerts() {
        viewModelScope.launch {
            combine(
                activeDeviceManager.observeActiveDevice(),
                deviceRepository.observeAllDevices(),
                alertRepository.observeUnacknowledgedEvents()
            ) { active, all, alerts -> Triple(active, all, alerts) }.collect { (active, all, alerts) ->
                if (active?.id != lastDeviceId) {
                    lastDeviceId = active?.id
                    cpuRingBuffer.clear()
                    rxRingBuffer.clear()
                    txRingBuffer.clear()
                }
                val hasCritical = alerts.any { it.severity == AlertSeverity.CRITICAL }
                val hasWarning = alerts.any { it.severity == AlertSeverity.WARNING }
                _uiState.update { state ->
                    val score = state.latestSnapshot?.healthScore ?: 100
                    val health = when {
                        hasCritical || score < 60 -> HealthBannerState.CRITICAL
                        hasWarning || score < 80 -> HealthBannerState.WARNING
                        else -> HealthBannerState.HEALTHY
                    }
                    state.copy(
                        isLoading = active == null && all.isEmpty(),
                        activeDevice = active,
                        allDevices = all,
                        activeAlertCount = alerts.size,
                        healthState = health,
                        cpuLoadHistory = cpuRingBuffer.toList(),
                        rxSpeedHistory = rxRingBuffer.toList(),
                        txSpeedHistory = txRingBuffer.toList()
                    )
                }
            }
        }
    }

    private fun observeLiveTelemetryStream() {
        viewModelScope.launch {
            val pollingFlow = settingsRepository.observeAppSettings()
                .map { it.livePollingIntervalMs }
                .distinctUntilChanged()

            combine(
                activeDeviceManager.observeActiveDevice(),
                _isLivePaused,
                pollingFlow
            ) { device, paused, intervalMs -> Triple(device, paused, intervalMs) }
                .flatMapLatest { (device, paused, intervalMs) ->
                    if (device == null || paused) {
                        emptyFlow()
                    } else {
                        val connector = connectorRegistry.getConnector(device.category)
                        _uiState.update { it.copy(streamError = null) }
                        connector.observeLiveTelemetry(device, intervalMs = intervalMs)
                            .catch { e ->
                                _uiState.update { it.copy(streamError = e.localizedMessage ?: "Telemetry stream interrupted") }
                            }
                    }
                }.collect { snapshot ->
                    when (val pl = snapshot.payload) {
                        is AndroidTelemetry -> {
                            pushRingPoint(cpuRingBuffer, (pl.cpuFrequencyLoadPct ?: 0f) * 100f)
                            pushRingPoint(rxRingBuffer, pl.rxBytesPerSec.toFloat())
                            pushRingPoint(txRingBuffer, pl.txBytesPerSec.toFloat())
                        }
                        is ComputerTelemetry -> {
                            pushRingPoint(cpuRingBuffer, (pl.cpuUsagePct * 100f).coerceIn(0f, 100f))
                            pushRingPoint(rxRingBuffer, pl.netRxBps.toFloat())
                            pushRingPoint(txRingBuffer, pl.netTxBps.toFloat())
                        }
                        else -> Unit
                    }
                    _uiState.update { state ->
                        val health = when {
                            state.healthState == HealthBannerState.CRITICAL || snapshot.healthScore < 60 -> HealthBannerState.CRITICAL
                            state.activeAlertCount > 0 || snapshot.healthScore < 80 -> HealthBannerState.WARNING
                            else -> HealthBannerState.HEALTHY
                        }
                        state.copy(
                            isLoading = false,
                            latestSnapshot = snapshot,
                            cpuLoadHistory = cpuRingBuffer.toList(),
                            rxSpeedHistory = rxRingBuffer.toList(),
                            txSpeedHistory = txRingBuffer.toList(),
                            healthState = health
                        )
                    }
                }
        }
    }

    private fun observeLocalCompass() {
        viewModelScope.launch {
            combine(activeDeviceManager.observeActiveDevice(), _isLivePaused) { d, p -> d to p }
                .flatMapLatest { (device, paused) ->
                    if (device != null && device.isLocalHost && !paused) sensorRepository.observeLiveSensors()
                    else emptyFlow()
                }.collect { readings ->
                    _uiState.update { it.copy(compassAzimuthDeg = readings.compassAzimuthDeg) }
                }
        }
    }

    private fun pushRingPoint(buffer: ArrayDeque<Float>, value: Float) {
        if (buffer.size >= MAX_SPARKLINE_POINTS) buffer.removeFirst()
        buffer.addLast(value)
    }

    companion object {
        private const val MAX_SPARKLINE_POINTS = 40
    }
}
