package com.devicexpert.app.presentation.network

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

data class NetworkDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val netType: String = "UNKNOWN",
    val ipAddress: String? = null,
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L,
    val peakRxBytesPerSec: Long = 0L,
    val peakTxBytesPerSec: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L,
    val totalBootRxBytes: Long = 0L,
    val totalBootTxBytes: Long = 0L,
    val linkDownstreamKbps: Int? = null,
    val linkUpstreamKbps: Int? = null,
    val rxHistory: List<Float> = emptyList(),
    val txHistory: List<Float> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NetworkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    private val _uiState = MutableStateFlow(NetworkDetailUiState())
    val uiState: StateFlow<NetworkDetailUiState> = _uiState.asStateFlow()

    private val rxRingBuffer = ArrayDeque<Float>(60)
    private val txRingBuffer = ArrayDeque<Float>(60)
    private var initialBootRx: Long? = null
    private var initialBootTx: Long? = null

    init { observeLiveNetwork() }

    private fun observeLiveNetwork() {
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
                    when (val pl = snapshot.payload) {
                        is AndroidTelemetry -> {
                            if (initialBootRx == null && pl.totalRxBytes > 0L) initialBootRx = pl.totalRxBytes
                            if (initialBootTx == null && pl.totalTxBytes > 0L) initialBootTx = pl.totalTxBytes
                            pushPoint(rxRingBuffer, pl.rxBytesPerSec.toFloat())
                            pushPoint(txRingBuffer, pl.txBytesPerSec.toFloat())
                            val sessionRx = initialBootRx?.let { (pl.totalRxBytes - it).coerceAtLeast(0L) } ?: 0L
                            val sessionTx = initialBootTx?.let { (pl.totalTxBytes - it).coerceAtLeast(0L) } ?: 0L
                            _uiState.update { s ->
                                s.copy(
                                    netType = pl.netType, ipAddress = pl.ipAddress,
                                    rxBytesPerSec = pl.rxBytesPerSec, txBytesPerSec = pl.txBytesPerSec,
                                    peakRxBytesPerSec = max(s.peakRxBytesPerSec, pl.rxBytesPerSec),
                                    peakTxBytesPerSec = max(s.peakTxBytesPerSec, pl.txBytesPerSec),
                                    sessionRxBytes = sessionRx, sessionTxBytes = sessionTx,
                                    totalBootRxBytes = pl.totalRxBytes, totalBootTxBytes = pl.totalTxBytes,
                                    linkDownstreamKbps = pl.linkDownstreamKbps, linkUpstreamKbps = pl.linkUpstreamKbps,
                                    rxHistory = rxRingBuffer.toList(), txHistory = txRingBuffer.toList()
                                )
                            }
                        }
                        is ComputerTelemetry -> {
                            pushPoint(rxRingBuffer, pl.netRxBps.toFloat())
                            pushPoint(txRingBuffer, pl.netTxBps.toFloat())
                            _uiState.update { s ->
                                s.copy(
                                    netType = "LAN / WAN",
                                    rxBytesPerSec = pl.netRxBps, txBytesPerSec = pl.netTxBps,
                                    peakRxBytesPerSec = max(s.peakRxBytesPerSec, pl.netRxBps),
                                    peakTxBytesPerSec = max(s.peakTxBytesPerSec, pl.netTxBps),
                                    rxHistory = rxRingBuffer.toList(), txHistory = txRingBuffer.toList()
                                )
                            }
                        }
                        else -> Unit
                    }
                }
        }
    }

    private fun pushPoint(buffer: ArrayDeque<Float>, value: Float) {
        if (buffer.size >= 60) buffer.removeFirst()
        buffer.addLast(value)
    }
}
