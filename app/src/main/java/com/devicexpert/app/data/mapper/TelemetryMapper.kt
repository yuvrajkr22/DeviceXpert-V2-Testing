package com.devicexpert.app.data.mapper

import com.devicexpert.app.data.collector.CpuThermalCollector
import com.devicexpert.app.data.local.entity.TelemetryEntity
import com.devicexpert.app.data.local.entity.TelemetryPayloadDto
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.model.telemetry.IotTelemetry
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import kotlinx.serialization.encodeToString

fun TelemetryEntity.toDomain(): TelemetrySnapshot {
    val payloadDto = deviceJson.decodeFromString<TelemetryPayloadDto>(this.payloadJson)
    val payloadDomain = when (payloadDto) {
        is TelemetryPayloadDto.Android -> AndroidTelemetry(
            batteryPct = payloadDto.batteryPct,
            batteryTempC = payloadDto.batteryTempC,
            isCharging = payloadDto.isCharging,
            chargingSource = payloadDto.chargingSource,
            batteryVoltageMv = payloadDto.batteryVoltageMv,
            batteryCurrentMa = payloadDto.batteryCurrentMa,
            batteryPowerWatts = payloadDto.batteryPowerWatts,
            batteryHealth = payloadDto.batteryHealth,
            batteryTechnology = payloadDto.batteryTechnology,
            batteryChargeCounterMah = payloadDto.batteryChargeCounterMah,
            batteryEstimatedFullMah = payloadDto.batteryEstimatedFullMah,
            batteryChargeTimeRemainingMs = payloadDto.batteryChargeTimeRemainingMs,
            batteryCycleCount = payloadDto.batteryCycleCount,
            ramUsedBytes = payloadDto.ramUsedBytes,
            ramTotalBytes = payloadDto.ramTotalBytes,
            ramAvailableBytes = payloadDto.ramAvailableBytes,
            ramLowMemory = payloadDto.ramLowMemory,
            storageUsedBytes = payloadDto.storageUsedBytes,
            storageTotalBytes = payloadDto.storageTotalBytes,
            storageAvailableBytes = payloadDto.storageAvailableBytes,
            cpuCoreFreqsMhz = payloadDto.cpuCoreFreqsMhz,
            cpuCoreMinFreqsMhz = payloadDto.cpuCoreMinFreqsMhz,
            cpuCoreMaxFreqsMhz = payloadDto.cpuCoreMaxFreqsMhz,
            cpuAvgFreqMhz = payloadDto.cpuAvgFreqMhz ?: CpuThermalCollector.computeAverageMhz(payloadDto.cpuCoreFreqsMhz),
            cpuPeakFreqMhz = payloadDto.cpuPeakFreqMhz ?: CpuThermalCollector.computePeakMhz(payloadDto.cpuCoreFreqsMhz),
            cpuFrequencyLoadPct = payloadDto.cpuFrequencyLoadPct,
            thermalStatus = payloadDto.thermalStatus,
            thermalHeadroom = payloadDto.thermalHeadroom,
            netType = payloadDto.netType,
            ipAddress = payloadDto.ipAddress,
            rxBytesPerSec = payloadDto.rxBytesPerSec,
            txBytesPerSec = payloadDto.txBytesPerSec,
            totalRxBytes = payloadDto.totalRxBytes,
            totalTxBytes = payloadDto.totalTxBytes,
            linkDownstreamKbps = payloadDto.linkDownstreamKbps,
            linkUpstreamKbps = payloadDto.linkUpstreamKbps,
            uptimeMillis = payloadDto.uptimeMillis,
            awakeUptimeMillis = payloadDto.awakeUptimeMillis,
            compassAzimuthDeg = payloadDto.compassAzimuthDeg,
            activeSensorReadings = payloadDto.activeSensorReadings
        )
        is TelemetryPayloadDto.Computer -> ComputerTelemetry(
            cpuUsagePct = payloadDto.cpuUsagePct,
            ramUsedBytes = payloadDto.ramUsedBytes,
            diskUsedBytes = payloadDto.diskUsedBytes,
            netRxBps = payloadDto.netRxBps,
            netTxBps = payloadDto.netTxBps,
            cpuTempC = payloadDto.cpuTempC,
            uptimeSeconds = payloadDto.uptimeSeconds,
            topProcessCount = payloadDto.topProcessCount
        )
        is TelemetryPayloadDto.Iot -> IotTelemetry(
            temperatureC = payloadDto.temperatureC,
            humidityPct = payloadDto.humidityPct,
            voltageV = payloadDto.voltageV,
            currentMa = payloadDto.currentMa,
            powerMw = payloadDto.powerMw,
            rssiDbm = payloadDto.rssiDbm,
            uptimeSeconds = payloadDto.uptimeSeconds,
            customMetrics = payloadDto.customMetrics
        )
    }

    return TelemetrySnapshot(
        deviceId = this.deviceId,
        timestamp = this.timestamp,
        healthScore = this.healthScore,
        payload = payloadDomain
    )
}

fun TelemetrySnapshot.toEntity(): TelemetryEntity {
    val payloadDto: TelemetryPayloadDto = when (val pl = this.payload) {
        is AndroidTelemetry -> TelemetryPayloadDto.Android(
            batteryPct = pl.batteryPct,
            batteryTempC = pl.batteryTempC,
            isCharging = pl.isCharging,
            chargingSource = pl.chargingSource,
            batteryVoltageMv = pl.batteryVoltageMv,
            batteryCurrentMa = pl.batteryCurrentMa,
            batteryPowerWatts = pl.batteryPowerWatts,
            batteryHealth = pl.batteryHealth,
            batteryTechnology = pl.batteryTechnology,
            batteryChargeCounterMah = pl.batteryChargeCounterMah,
            batteryEstimatedFullMah = pl.batteryEstimatedFullMah,
            batteryChargeTimeRemainingMs = pl.batteryChargeTimeRemainingMs,
            batteryCycleCount = pl.batteryCycleCount,
            ramUsedBytes = pl.ramUsedBytes,
            ramTotalBytes = pl.ramTotalBytes,
            ramAvailableBytes = pl.ramAvailableBytes,
            ramLowMemory = pl.ramLowMemory,
            storageUsedBytes = pl.storageUsedBytes,
            storageTotalBytes = pl.storageTotalBytes,
            storageAvailableBytes = pl.storageAvailableBytes,
            cpuCoreFreqsMhz = pl.cpuCoreFreqsMhz,
            cpuCoreMinFreqsMhz = pl.cpuCoreMinFreqsMhz,
            cpuCoreMaxFreqsMhz = pl.cpuCoreMaxFreqsMhz,
            cpuAvgFreqMhz = pl.cpuAvgFreqMhz,
            cpuPeakFreqMhz = pl.cpuPeakFreqMhz,
            cpuFrequencyLoadPct = pl.cpuFrequencyLoadPct,
            thermalStatus = pl.thermalStatus,
            thermalHeadroom = pl.thermalHeadroom,
            netType = pl.netType,
            ipAddress = pl.ipAddress,
            rxBytesPerSec = pl.rxBytesPerSec,
            txBytesPerSec = pl.txBytesPerSec,
            totalRxBytes = pl.totalRxBytes,
            totalTxBytes = pl.totalTxBytes,
            linkDownstreamKbps = pl.linkDownstreamKbps,
            linkUpstreamKbps = pl.linkUpstreamKbps,
            uptimeMillis = pl.uptimeMillis,
            awakeUptimeMillis = pl.awakeUptimeMillis,
            compassAzimuthDeg = pl.compassAzimuthDeg,
            activeSensorReadings = pl.activeSensorReadings
        )
        is ComputerTelemetry -> TelemetryPayloadDto.Computer(
            cpuUsagePct = pl.cpuUsagePct,
            ramUsedBytes = pl.ramUsedBytes,
            diskUsedBytes = pl.diskUsedBytes,
            netRxBps = pl.netRxBps,
            netTxBps = pl.netTxBps,
            cpuTempC = pl.cpuTempC,
            uptimeSeconds = pl.uptimeSeconds,
            topProcessCount = pl.topProcessCount
        )
        is IotTelemetry -> TelemetryPayloadDto.Iot(
            temperatureC = pl.temperatureC,
            humidityPct = pl.humidityPct,
            voltageV = pl.voltageV,
            currentMa = pl.currentMa,
            powerMw = pl.powerMw,
            rssiDbm = pl.rssiDbm,
            uptimeSeconds = pl.uptimeSeconds,
            customMetrics = pl.customMetrics
        )
    }

    return TelemetryEntity(
        deviceId = this.deviceId,
        timestamp = this.timestamp,
        healthScore = this.healthScore,
        payloadJson = deviceJson.encodeToString(payloadDto)
    )
}
