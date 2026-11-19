package com.devicexpert.app.presentation.ram

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.device.AndroidMetadata
import com.devicexpert.app.domain.model.device.ComputerMetadata
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
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

enum class MemoryPressureLevel { NORMAL, ELEVATED, CRITICAL }

data class RamDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val totalRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usageRatio: Float = 0f,
    val isLowMemoryFlag: Boolean = false,
    val pressureLevel: MemoryPressureLevel = MemoryPressureLevel.NORMAL,
    val liveUsageHistoryPct: List<Float> = emptyList(),
    val hasHistoryChart: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RamDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val telemetryRepository: TelemetryRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    val ramHistoryProducer = CartesianChartModelProducer()
    private val _uiState = MutableStateFlow(RamDetailUiState())
    val uiState: StateFlow<RamDetailUiState> = _uiState.asStateFlow()
    private val liveRingBuffer = ArrayDeque<Float>(60)

    init {
        observeLiveRam()
        observeHistoricalRam()
    }

    private fun observeLiveRam() {
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
                    val metaTotal = when (val meta = device.metadata) {
                        is AndroidMetadata -> meta.totalRamBytes
                        is ComputerMetadata -> meta.totalRamBytes
                        else -> 0L
                    }
                    var total = metaTotal
                    var used = 0L
                    var avail = 0L
                    var lowMem = false
                    when (val pl = snapshot.payload) {
                        is AndroidTelemetry -> {
                            total = pl.ramTotalBytes.takeIf { it > 0L } ?: metaTotal
                            used = pl.ramUsedBytes
                            avail = pl.ramAvailableBytes.takeIf { it > 0L } ?: (total - used).coerceAtLeast(0L)
                            lowMem = pl.ramLowMemory
                        }
                        is ComputerTelemetry -> {
                            used = pl.ramUsedBytes
                            avail = (total - used).coerceAtLeast(0L)
                        }
                        else -> return@collect
                    }
                    val ratio = if (total > 0L) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    val pressure = when {
                        lowMem || ratio >= 0.90f -> MemoryPressureLevel.CRITICAL
                        ratio >= 0.75f -> MemoryPressureLevel.ELEVATED
                        else -> MemoryPressureLevel.NORMAL
                    }
                    if (liveRingBuffer.size >= 60) liveRingBuffer.removeFirst()
                    liveRingBuffer.addLast(ratio * 100f)
                    _uiState.update {
                        it.copy(
                            totalRamBytes = total, usedRamBytes = used, availableRamBytes = avail,
                            usageRatio = ratio, isLowMemoryFlag = lowMem, pressureLevel = pressure,
                            liveUsageHistoryPct = liveRingBuffer.toList()
                        )
                    }
                }
        }
    }

    private fun observeHistoricalRam() {
        viewModelScope.launch(Dispatchers.IO) {
            val device = deviceRepository.getDeviceById(deviceId)
            val fallbackTotal = when (val meta = device?.metadata) {
                is AndroidMetadata -> meta.totalRamBytes
                is ComputerMetadata -> meta.totalRamBytes
                else -> 0L
            }
            val since = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
            telemetryRepository.observeTelemetryHistory(deviceId, since)
                .catch { }
                .collect { snapshots ->
                    val points = snapshots.mapNotNull { snap ->
                        when (val pl = snap.payload) {
                            is AndroidTelemetry -> {
                                val t = pl.ramTotalBytes.takeIf { it > 0L } ?: fallbackTotal
                                if (t > 0L) ((pl.ramUsedBytes.toFloat() / t.toFloat()) * 100f).coerceIn(0f, 100f) else null
                            }
                            is ComputerTelemetry -> {
                                if (fallbackTotal > 0L) ((pl.ramUsedBytes.toFloat() / fallbackTotal.toFloat()) * 100f).coerceIn(0f, 100f) else null
                            }
                            else -> null
                        }
                    }
                    if (points.size >= 2) {
                        ramHistoryProducer.runTransaction { lineSeries { series(points) } }
                        _uiState.update { it.copy(hasHistoryChart = true) }
                    }
                }
        }
    }
}
