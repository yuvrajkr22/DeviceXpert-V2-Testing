package com.devicexpert.app.presentation.sensors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.sensor.HardwareSensor
import com.devicexpert.app.domain.model.sensor.LiveSensorReadings
import com.devicexpert.app.domain.repository.DeviceRepository
import com.devicexpert.app.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

data class SensorsDetailUiState(
    val isLoading: Boolean = true,
    val device: UnifiedDevice? = null,
    val isLocalHost: Boolean = true,
    val liveReadings: LiveSensorReadings = LiveSensorReadings(),
    val accelMagnitudeHistory: List<Float> = emptyList(),
    val allSensors: List<HardwareSensor> = emptyList(),
    val searchQuery: String = ""
) {
    val filteredSensors: List<HardwareSensor>
        get() = if (searchQuery.isBlank()) allSensors else {
            val q = searchQuery.trim().lowercase()
            allSensors.filter { it.name.lowercase().contains(q) || it.vendor.lowercase().contains(q) || it.stringType.lowercase().contains(q) }
        }
}

@HiltViewModel
class SensorsDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    private val _uiState = MutableStateFlow(SensorsDetailUiState())
    val uiState: StateFlow<SensorsDetailUiState> = _uiState.asStateFlow()
    private val accelRingBuffer = ArrayDeque<Float>(50)

    init { loadSensors() }

    fun onSearchQueryChanged(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    private fun loadSensors() {
        viewModelScope.launch {
            val device = deviceRepository.getDeviceById(deviceId)
            val isLocal = device?.isLocalHost != false
            if (!isLocal) {
                _uiState.update { it.copy(isLoading = false, device = device, isLocalHost = false) }
                return@launch
            }
            val inventory = sensorRepository.getAllHardwareSensors()
            _uiState.update { it.copy(isLoading = false, device = device, isLocalHost = true, allSensors = inventory) }
            sensorRepository.observeLiveSensors().catch { }.collect { readings ->
                readings.accelerometer?.let { (x, y, z) ->
                    val mag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    if (accelRingBuffer.size >= 50) accelRingBuffer.removeFirst()
                    accelRingBuffer.addLast(mag)
                }
                _uiState.update { it.copy(liveReadings = readings, accelMagnitudeHistory = accelRingBuffer.toList()) }
            }
        }
    }
}
