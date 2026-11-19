package com.devicexpert.app.data.initializer

import android.os.Build
import com.devicexpert.app.data.collector.DeviceInfoCollector
import com.devicexpert.app.domain.model.alert.AlertOperator
import com.devicexpert.app.domain.model.alert.AlertRule
import com.devicexpert.app.domain.model.alert.AlertSeverity
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.repository.AlertRepository
import com.devicexpert.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostDeviceInitializer @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val alertRepository: AlertRepository,
    private val deviceInfoCollector: DeviceInfoCollector
) {
    suspend fun ensureHostDeviceRegistered() {
        val existingDevices = deviceRepository.observeAllDevices().firstOrNull().orEmpty()
        val existingHost = existingDevices.firstOrNull { it.isLocalHost }
        val metadata = deviceInfoCollector.collectAndroidMetadata()
        val now = System.currentTimeMillis()

        val hostDevice = UnifiedDevice(
            id = existingHost?.id ?: LOCAL_HOST_DEVICE_ID,
            name = existingHost?.name ?: Build.MODEL,
            category = DeviceCategory.ANDROID,
            status = ConnectionStatus.ONLINE,
            isLocalHost = true,
            lastSeenMillis = now,
            metadata = metadata
        )
        deviceRepository.registerDevice(hostDevice)

        if (alertRepository.getActiveRules().isEmpty()) {
            alertRepository.saveRule(
                AlertRule(
                    ruleId = "default_high_temp",
                    deviceId = hostDevice.id,
                    metricKey = "TEMP_C",
                    operator = AlertOperator.GT,
                    thresholdValue = 43.0,
                    severity = AlertSeverity.CRITICAL,
                    isEnabled = true
                )
            )
            alertRepository.saveRule(
                AlertRule(
                    ruleId = "default_high_ram",
                    deviceId = hostDevice.id,
                    metricKey = "RAM_USED_PCT",
                    operator = AlertOperator.GT,
                    thresholdValue = 90.0,
                    severity = AlertSeverity.WARNING,
                    isEnabled = true
                )
            )
        }
    }

    companion object {
        const val LOCAL_HOST_DEVICE_ID = "local_android_host"
    }
}
