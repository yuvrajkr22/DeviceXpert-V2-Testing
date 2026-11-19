package com.devicexpert.app.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.domain.manager.ActiveDeviceManager
import com.devicexpert.app.domain.model.device.ComputerMetadata
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.IotMetadata
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
    val devices: List<UnifiedDevice> = emptyList(),
    val activeDeviceId: String? = null,
    val selectedFilter: DeviceCategory? = null
) {
    val filteredDevices: List<UnifiedDevice>
        get() = selectedFilter?.let { cat -> devices.filter { it.category == cat } } ?: devices
}

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val activeDeviceManager: ActiveDeviceManager
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<DeviceCategory?>(null)

    val uiState: StateFlow<DevicesUiState> = combine(
        deviceRepository.observeAllDevices(),
        activeDeviceManager.observeActiveDevice(),
        selectedFilter
    ) { all, active, filter -> DevicesUiState(all, active?.id, filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DevicesUiState())

    fun setFilter(category: DeviceCategory?) { selectedFilter.value = category }
    fun selectActiveDevice(deviceId: String) { activeDeviceManager.selectDevice(deviceId) }

    fun registerComputerDevice(deviceId: String, name: String, osName: String, cpuModel: String, coreCount: Int, totalRamGb: Int) {
        val cleanId = deviceId.trim().ifBlank { "pc_${System.currentTimeMillis() % 10000}" }
        val device = UnifiedDevice(
            id = cleanId, name = name.trim().ifBlank { cleanId }, category = DeviceCategory.COMPUTER,
            status = ConnectionStatus.OFFLINE, isLocalHost = false, lastSeenMillis = System.currentTimeMillis(),
            metadata = ComputerMetadata(
                hostname = name.trim().ifBlank { cleanId }, osName = osName.trim().ifBlank { "Linux / Windows" },
                osVersion = "Agent", cpuModel = cpuModel.trim().ifBlank { "x86_64" }, coreCount = coreCount.coerceAtLeast(1),
                totalRamBytes = totalRamGb.coerceAtLeast(1).toLong() * 1024L * 1024L * 1024L, totalDiskBytes = 0L, agentVersion = "2.0"
            )
        )
        viewModelScope.launch { deviceRepository.registerDevice(device) }
    }

    fun registerIotDevice(deviceId: String, name: String, boardModel: String, topicPrefix: String) {
        val cleanId = deviceId.trim().ifBlank { "esp32_${System.currentTimeMillis() % 10000}" }
        val resolvedPrefix = topicPrefix.trim().ifBlank { "devicexpert/v1/local/$cleanId" }
        val device = UnifiedDevice(
            id = cleanId, name = name.trim().ifBlank { cleanId }, category = DeviceCategory.IOT,
            status = ConnectionStatus.OFFLINE, isLocalHost = false, lastSeenMillis = System.currentTimeMillis(),
            metadata = IotMetadata(
                boardModel = boardModel.trim().ifBlank { "ESP32" }, firmwareVersion = "2.0",
                macAddress = "MQTT", ipAddress = "Remote", mqttTopicPrefix = resolvedPrefix,
                capabilities = listOf("Temperature", "Humidity", "Power", "Custom")
            )
        )
        viewModelScope.launch { deviceRepository.registerDevice(device) }
    }

    fun deleteDevice(device: UnifiedDevice) {
        if (device.isLocalHost) return
        viewModelScope.launch { deviceRepository.removeDevice(device.id) }
    }
}
