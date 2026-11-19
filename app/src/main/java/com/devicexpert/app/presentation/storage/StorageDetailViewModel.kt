package com.devicexpert.app.presentation.storage

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val usedRatio: Float = 0f,
    val freeRatio: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class StorageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val connectorRegistry: ConnectorRegistry
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    private val _uiState = MutableStateFlow(StorageDetailUiState())
    val uiState: StateFlow<StorageDetailUiState> = _uiState.asStateFlow()

    init { observeStorage() }

    private fun observeStorage() {
        viewModelScope.launch {
            val device = deviceRepository.getDeviceById(deviceId)
            if (device == null) {
                _uiState.update { it.copy(isLoading = false, error = "Device not found") }
                return@launch
            }
            _uiState.update { it.copy(device = device, isLoading = false) }
            val connector = connectorRegistry.getConnector(device.category)
            connector.observeLiveTelemetry(device, intervalMs = 5000L)
                .catch { e -> _uiState.update { it.copy(error = e.localizedMessage) } }
                .collect { snapshot ->
                    val metaTotal = when (val meta = device.metadata) {
                        is AndroidMetadata -> meta.totalStorageBytes
                        is ComputerMetadata -> meta.totalDiskBytes
                        else -> 0L
                    }
                    var total = metaTotal
                    var used = 0L
                    var avail = 0L
                    when (val pl = snapshot.payload) {
                        is AndroidTelemetry -> {
                            total = pl.storageTotalBytes.takeIf { it > 0L } ?: metaTotal
                            used = pl.storageUsedBytes
                            avail = pl.storageAvailableBytes.takeIf { it > 0L } ?: (total - used).coerceAtLeast(0L)
                        }
                        is ComputerTelemetry -> {
                            used = pl.diskUsedBytes
                            avail = (total - used).coerceAtLeast(0L)
                        }
                        else -> return@collect
                    }
                    val usedRatio = if (total > 0L) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    val freeRatio = if (total > 0L) (avail.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    _uiState.update { it.copy(totalBytes = total, usedBytes = used, availableBytes = avail, usedRatio = usedRatio, freeRatio = freeRatio) }
                }
        }
    }
}
