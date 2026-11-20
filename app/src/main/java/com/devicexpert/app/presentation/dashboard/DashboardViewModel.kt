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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    deviceRepository: DeviceRepository,
    alertRepository: AlertRepository,
    private val sensorRepository: SensorRepository,
    settingsRepository: SettingsRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val _isLivePaused = MutableStateFlow(false)

    private val cpuRingBuffer = ArrayDeque<Float>(MAX_SPARKLINE_POINTS)
    private val rxRingBuffer = ArrayDeque<Float>(MAX_SPARKLINE_POINTS)
    private val txRingBuffer = ArrayDeque<Float>(MAX_SPARKLINE_POINTS)
    private var lastDeviceId: String? = null
    private var lastSnapshot: TelemetrySnapshot? = null
    private var lastCompassAzimuthDeg: Float? = null

    private val activeDeviceFlow = activeDeviceManager.observeActiveDevice()
        .distinctUntilChanged()

    private val pollingIntervalFlow = settingsRepository.observeAppSettings()
        .map { it.livePollingIntervalMs }
        .distinctUntilChanged()

    private val telemetryStreamFlow: Flow<Pair<TelemetrySnapshot?, String?>> = combine(
        activeDeviceFlow,
        _isLivePaused,
        pollingIntervalFlow
    ) { device, paused, intervalMs -> Triple(device, paused, intervalMs) }
        .flatMapLatest { (device, paused, intervalMs) ->
            if (device?.id != lastDeviceId) {
                lastDeviceId = device?.id
                lastSnapshot = null
                cpuRingBuffer.clear()
                rxRingBuffer.clear()
                txRingBuffer.clear()
            }

            val cachedForDevice = lastSnapshot?.takeIf { it.deviceId == device?.id }
            if (device == null || paused) {
                flowOf(cachedForDevice to null)
            } else {
                val connector = connectorRegistry.getConnector(device.category)
                connector.observeLiveTelemetry(device, intervalMs = intervalMs)
                    .onEach { snapshot ->
                        lastSnapshot = snapshot
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
                    }
                    .map<TelemetrySnapshot, Pair<TelemetrySnapshot?, String?>> { it to null }
                    .onStart { emit(cachedForDevice to null) }
                    .catch { e ->
                        emit(
                            (lastSnapshot?.takeIf { it.deviceId == device.id }) to
                                (e.localizedMessage ?: "Telemetry stream interrupted")
                        )
                    }
            }
        }

    private val compassStreamFlow: Flow<Float?> = combine(activeDeviceFlow, _isLivePaused) { device, paused ->
        device to paused
    }.flatMapLatest { (device, paused) ->
        if (device != null && device.isLocalHost && !paused) {
            sensorRepository.observeLiveSensors()
                .map { readings ->
                    lastCompassAzimuthDeg = readings.compassAzimuthDeg
                    readings.compassAzimuthDeg
                }
                .onStart { emit(lastCompassAzimuthDeg) }
        } else {
            flowOf(if (device?.isLocalHost == true) lastCompassAzimuthDeg else null)
        }
    }

    private val deviceAndAlertFlow = combine(
        activeDeviceFlow,
        deviceRepository.observeAllDevices(),
        alertRepository.observeUnacknowledgedEvents()
    ) { active, all, alerts -> Triple(active, all, alerts) }

    private val liveTelemetryBundleFlow = combine(
        telemetryStreamFlow,
        compassStreamFlow,
        _isLivePaused
    ) { telemetryResult, compass, paused -> Triple(telemetryResult, compass, paused) }

    // WhileSubscribed(0L) immediately stops upstream polling when navigating away from Dashboard
    // (e.g. to CpuDetailScreen), avoiding duplicate collectors while retaining ring-buffer history.
    val uiState: StateFlow<DashboardUiState> = combine(
        deviceAndAlertFlow,
        liveTelemetryBundleFlow
    ) { (active, all, alerts), (telemetryResult, compass, paused) ->
        val (snapshot, streamError) = telemetryResult
        val hasCritical = alerts.any { it.severity == AlertSeverity.CRITICAL }
        val hasWarning = alerts.any { it.severity == AlertSeverity.WARNING }
        val score = snapshot?.healthScore ?: 100
        val health = when {
            hasCritical || score < 60 -> HealthBannerState.CRITICAL
            hasWarning || alerts.isNotEmpty() || score < 80 -> HealthBannerState.WARNING
            else -> HealthBannerState.HEALTHY
        }

        DashboardUiState(
            isLoading = active == null && all.isEmpty(),
            activeDevice = active,
            allDevices = all,
            latestSnapshot = snapshot,
            isLivePaused = paused,
            cpuLoadHistory = cpuRingBuffer.toList(),
            rxSpeedHistory = rxRingBuffer.toList(),
            txSpeedHistory = txRingBuffer.toList(),
            compassAzimuthDeg = compass,
            activeAlertCount = alerts.size,
            healthState = health,
            streamError = streamError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0L),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun selectDevice(deviceId: String) = activeDeviceManager.selectDevice(deviceId)

    fun toggleLivePause() {
        _isLivePaused.update { !it }
    }

    private fun pushRingPoint(buffer: ArrayDeque<Float>, value: Float) {
        if (buffer.size >= MAX_SPARKLINE_POINTS) buffer.removeFirst()
        buffer.addLast(value)
    }

    companion object {
        private const val MAX_SPARKLINE_POINTS = 40
    }
}
