package com.devicexpert.app.data.connector

import com.devicexpert.app.data.local.entity.TelemetryPayloadDto
import com.devicexpert.app.data.mapper.deviceJson
import com.devicexpert.app.data.remote.mqtt.MqttTransportClient
import com.devicexpert.app.domain.connector.DeviceConnector
import com.devicexpert.app.domain.model.device.ComputerMetadata
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttComputerDeviceConnector @Inject constructor(
    private val mqttTransport: MqttTransportClient
) : DeviceConnector {

    override val supportedCategory: DeviceCategory = DeviceCategory.COMPUTER

    private fun resolveTopicPrefix(device: UnifiedDevice): String = "devicexpert/v1/local/${device.id}"

    override fun observeLiveTelemetry(device: UnifiedDevice, intervalMs: Long): Flow<TelemetrySnapshot> {
        val topic = "${resolveTopicPrefix(device)}/telemetry"
        val meta = device.metadata as? ComputerMetadata

        return mqttTransport.subscribeTopic(topic).mapNotNull { jsonStr ->
            runCatching {
                val dto = deviceJson.decodeFromString<TelemetryPayloadDto.Computer>(jsonStr)
                val payload = ComputerTelemetry(
                    cpuUsagePct = dto.cpuUsagePct.coerceIn(0f, 1f),
                    ramUsedBytes = dto.ramUsedBytes,
                    diskUsedBytes = dto.diskUsedBytes,
                    netRxBps = dto.netRxBps,
                    netTxBps = dto.netTxBps,
                    cpuTempC = dto.cpuTempC,
                    uptimeSeconds = dto.uptimeSeconds,
                    topProcessCount = dto.topProcessCount
                )
                var score = 100
                if (dto.cpuUsagePct > 0.90f) score -= 20
                if (dto.cpuTempC != null && dto.cpuTempC > 85f) score -= 25
                if (meta != null && meta.totalRamBytes > 0L) {
                    val ramRatio = dto.ramUsedBytes.toFloat() / meta.totalRamBytes.toFloat()
                    if (ramRatio > 0.90f) score -= 20
                }
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
            ?: error("MQTT timeout waiting for PC telemetry")
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
