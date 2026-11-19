package com.devicexpert.app.data.connector

import com.devicexpert.app.data.local.entity.TelemetryPayloadDto
import com.devicexpert.app.data.mapper.deviceJson
import com.devicexpert.app.data.remote.mqtt.MqttTransportClient
import com.devicexpert.app.domain.connector.DeviceConnector
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.IotMetadata
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.IotTelemetry
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttIotDeviceConnector @Inject constructor(
    private val mqttTransport: MqttTransportClient
) : DeviceConnector {

    override val supportedCategory: DeviceCategory = DeviceCategory.IOT

    private fun resolveTopicPrefix(device: UnifiedDevice): String {
        val meta = device.metadata as? IotMetadata
        return meta?.mqttTopicPrefix?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
            ?: "devicexpert/v1/local/${device.id}"
    }

    override fun observeLiveTelemetry(device: UnifiedDevice, intervalMs: Long): Flow<TelemetrySnapshot> {
        val topic = "${resolveTopicPrefix(device)}/telemetry"
        return mqttTransport.subscribeTopic(topic).mapNotNull { jsonStr ->
            runCatching {
                val dto = deviceJson.decodeFromString<TelemetryPayloadDto.Iot>(jsonStr)
                val payload = IotTelemetry(
                    temperatureC = dto.temperatureC,
                    humidityPct = dto.humidityPct,
                    voltageV = dto.voltageV,
                    currentMa = dto.currentMa,
                    powerMw = dto.powerMw,
                    rssiDbm = dto.rssiDbm,
                    uptimeSeconds = dto.uptimeSeconds,
                    customMetrics = dto.customMetrics
                )
                var score = 100
                if (dto.temperatureC != null && dto.temperatureC > 75f) score -= 30
                if (dto.rssiDbm != null && dto.rssiDbm < -85) score -= 15
                TelemetrySnapshot(
                    deviceId = device.id,
                    timestamp = System.currentTimeMillis(),
                    healthScore = score.coerceIn(0, 100),
                    payload = payload
                )
            }.getOrNull()
        }
    }

    override suspend fun fetchSnapshotOnce(device: UnifiedDevice): Result<TelemetrySnapshot> = runCatching {
        withTimeoutOrNull(5000L) { observeLiveTelemetry(device, 0L).firstOrNull() }
            ?: error("MQTT timeout waiting for IoT telemetry")
    }

    override fun observeConnectionStatus(device: UnifiedDevice): Flow<ConnectionStatus> {
        val topic = "${resolveTopicPrefix(device)}/status"
        return mqttTransport.subscribeTopic(topic).map { raw ->
            when (raw.trim().uppercase()) {
                "ONLINE" -> ConnectionStatus.ONLINE
                "OFFLINE" -> ConnectionStatus.OFFLINE
                else -> ConnectionStatus.DEGRADED
            }
        }
    }
}
