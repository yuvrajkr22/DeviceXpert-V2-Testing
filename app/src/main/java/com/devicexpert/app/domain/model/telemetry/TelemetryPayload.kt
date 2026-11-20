package com.devicexpert.app.domain.model.telemetry

sealed interface TelemetryPayload

data class AndroidTelemetry(
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
    val cpuAvgFreqMhz: Int? = null,
    val cpuPeakFreqMhz: Int? = null,
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
) : TelemetryPayload

data class ComputerTelemetry(
    val cpuUsagePct: Float,
    val ramUsedBytes: Long,
    val diskUsedBytes: Long,
    val netRxBps: Long,
    val netTxBps: Long,
    val cpuTempC: Float?,
    val uptimeSeconds: Long,
    val topProcessCount: Int
) : TelemetryPayload

data class IotTelemetry(
    val temperatureC: Float?,
    val humidityPct: Float?,
    val voltageV: Float?,
    val currentMa: Float?,
    val powerMw: Float?,
    val rssiDbm: Int?,
    val uptimeSeconds: Long,
    val customMetrics: Map<String, Double>
) : TelemetryPayload
