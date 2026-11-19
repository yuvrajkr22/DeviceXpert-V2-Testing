package com.devicexpert.app.data.local.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DeviceMetadataDto {
    @Serializable
    @SerialName("ANDROID")
    data class Android(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val sdkInt: Int,
        val socModel: String,
        val totalRamBytes: Long,
        val totalStorageBytes: Long,
        val sensorCount: Int,
        val brand: String = "",
        val board: String = "",
        val hardware: String = "",
        val securityPatch: String = "",
        val supportedAbis: List<String> = emptyList(),
        val cpuCoreCount: Int = 0,
        val displayWidthPx: Int = 0,
        val displayHeightPx: Int = 0,
        val displayDensityDpi: Int = 0,
        val displayDiagonalInches: Float = 0f,
        val displayRefreshRateHz: Float = 60f,
        val kernelVersion: String = ""
    ) : DeviceMetadataDto

    @Serializable
    @SerialName("COMPUTER")
    data class Computer(
        val hostname: String,
        val osName: String,
        val osVersion: String,
        val cpuModel: String,
        val coreCount: Int,
        val totalRamBytes: Long,
        val totalDiskBytes: Long,
        val agentVersion: String
    ) : DeviceMetadataDto

    @Serializable
    @SerialName("IOT")
    data class Iot(
        val boardModel: String,
        val firmwareVersion: String,
        val macAddress: String,
        val ipAddress: String,
        val mqttTopicPrefix: String,
        val capabilities: List<String>
    ) : DeviceMetadataDto
}

@Serializable
sealed interface TelemetryPayloadDto {
    @Serializable
    @SerialName("ANDROID")
    data class Android(
        val batteryPct: Float,
        val batteryTempC: Float,
        val isCharging: Boolean,
        val chargingSource: String,
        val batteryVoltageMv: Int = 0,
        val batteryCurrentMa: Float? = null,
        val batteryPowerWatts: Float? = null,
        val batteryHealth: String = "UNKNOWN",
        val batteryTechnology: String = "UNKNOWN",
        val batteryChargeCounterMah: Float? = null,
        val batteryEstimatedFullMah: Float? = null,
        val batteryChargeTimeRemainingMs: Long? = null,
        val batteryCycleCount: Int? = null,
        val ramUsedBytes: Long,
        val ramTotalBytes: Long = 0L,
        val ramAvailableBytes: Long = 0L,
        val ramLowMemory: Boolean = false,
        val storageUsedBytes: Long,
        val storageTotalBytes: Long = 0L,
        val storageAvailableBytes: Long = 0L,
        val cpuCoreFreqsMhz: List<Int>,
        val cpuCoreMinFreqsMhz: List<Int> = emptyList(),
        val cpuCoreMaxFreqsMhz: List<Int> = emptyList(),
        val cpuFrequencyLoadPct: Float? = null,
        val thermalStatus: Int,
        val thermalHeadroom: Float? = null,
        val netType: String,
        val ipAddress: String? = null,
        val rxBytesPerSec: Long,
        val txBytesPerSec: Long,
        val totalRxBytes: Long = 0L,
        val totalTxBytes: Long = 0L,
        val linkDownstreamKbps: Int? = null,
        val linkUpstreamKbps: Int? = null,
        val uptimeMillis: Long,
        val awakeUptimeMillis: Long = 0L,
        val compassAzimuthDeg: Float? = null,
        val activeSensorReadings: Map<String, Float> = emptyMap()
    ) : TelemetryPayloadDto

    @Serializable
    @SerialName("COMPUTER")
    data class Computer(
        val cpuUsagePct: Float,
        val ramUsedBytes: Long,
        val diskUsedBytes: Long,
        val netRxBps: Long,
        val netTxBps: Long,
        val cpuTempC: Float?,
        val uptimeSeconds: Long,
        val topProcessCount: Int
    ) : TelemetryPayloadDto

    @Serializable
    @SerialName("IOT")
    data class Iot(
        val temperatureC: Float?,
        val humidityPct: Float?,
        val voltageV: Float?,
        val currentMa: Float?,
        val powerMw: Float?,
        val rssiDbm: Int?,
        val uptimeSeconds: Long,
        val customMetrics: Map<String, Double>
    ) : TelemetryPayloadDto
}
