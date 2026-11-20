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
import com.devicexpert.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CpuDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val payload: TelemetryPayload? = null,
    val liveLoadHistory: List<Float> = emptyList(),
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CpuDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    settingsRepository: SettingsRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String =
        savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID

    private val loadRingBuffer = ArrayDeque<Float>(60)
    private var lastPayload: TelemetryPayload? = null

    private val deviceFlow = flow {
        emit(deviceRepository.getDeviceById(deviceId))
    }

    private val pollingIntervalFlow = settingsRepository.observeAppSettings()
        .map { it.livePollingIntervalMs }
        .distinctUntilChanged()

    val uiState: StateFlow<CpuDetailUiState> = combine(
        deviceFlow,
        pollingIntervalFlow
    ) { device, intervalMs -> device to intervalMs }
        .flatMapLatest { (device, intervalMs) ->
            if (device == null) {
                flowOf(CpuDetailUiState(isLoading = false, error = "Device not found"))
            } else {
                val connector = connectorRegistry.getConnector(device.category)
                connector.observeLiveTelemetry(device, intervalMs = intervalMs)
                    .map { snapshot ->
                        val sample = when (val pl = snapshot.payload) {
                            is AndroidTelemetry -> (pl.cpuFrequencyLoadPct ?: 0f) * 100f
                            is ComputerTelemetry -> (pl.cpuUsagePct * 100f).coerceIn(0f, 100f)
                            else -> null
                        }
                        if (sample != null) {
                            if (loadRingBuffer.size >= 60) loadRingBuffer.removeFirst()
                            loadRingBuffer.addLast(sample)
                        }
                        lastPayload = snapshot.payload
                        CpuDetailUiState(
                            isLoading = false,
                            device = device,
                            payload = snapshot.payload,
                            liveLoadHistory = loadRingBuffer.toList()
                        )
                    }
                    .onStart {
                        emit(
                            CpuDetailUiState(
                                isLoading = false,
                                device = device,
                                payload = lastPayload,
                                liveLoadHistory = loadRingBuffer.toList()
                            )
                        )
                    }
                    .catch { e ->
                        emit(
                            CpuDetailUiState(
                                isLoading = false,
                                device = device,
                                payload = lastPayload,
                                liveLoadHistory = loadRingBuffer.toList(),
                                error = e.localizedMessage
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0L),
            initialValue = CpuDetailUiState(isLoading = true)
        )
}
