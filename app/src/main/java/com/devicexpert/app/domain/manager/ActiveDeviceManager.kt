package com.devicexpert.app.domain.manager

import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveDeviceManager @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    fun selectDevice(deviceId: String) {
        _selectedDeviceId.value = deviceId
    }

    fun observeActiveDevice(): Flow<UnifiedDevice?> = combine(
        deviceRepository.observeAllDevices(),
        _selectedDeviceId
    ) { devices, selectedId ->
        val selected = selectedId?.let { id -> devices.find { it.id == id } }
        selected ?: devices.firstOrNull { it.isLocalHost } ?: devices.firstOrNull()
    }
}
