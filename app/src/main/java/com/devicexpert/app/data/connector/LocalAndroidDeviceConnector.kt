package com.devicexpert.app.data.connector

import android.os.SystemClock
import com.devicexpert.app.data.collector.BatteryCollector
import com.devicexpert.app.data.collector.CpuThermalCollector
import com.devicexpert.app.data.collector.MemoryStorageCollector
import com.devicexpert.app.data.collector.NetworkCollector
import com.devicexpert.app.domain.connector.DeviceConnector
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAndroidDeviceConnector @Inject constructor(
    private val batteryCollector: BatteryCollector,
    private val memoryCollector: MemoryStorageCollector,
    private val cpuCollector: CpuThermalCollector,
    private val networkCollector: NetworkCollector
) : DeviceConnector {

    override val supportedCategory: DeviceCategory = DeviceCategory.ANDROID

    override fun observeLiveTelemetry(device: UnifiedDevice, intervalMs: Long): Flow<TelemetrySnapshot> = flow {
        val safeInterval = intervalMs.coerceAtLeast(500L)
        while (true) {
            fetchSnapshotOnce(device).onSuccess { emit(it) }
            delay(safeInterval)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun fetchSnapshotOnce(device: UnifiedDevice): Result<TelemetrySnapshot> =
        withContext(Dispatchers.IO) {
            try {
                val battery = batteryCollector.getBatteryMetrics()
                val memory = memoryCollector.getMemoryStorageMetrics()
                val cpu = cpuCollector.getCpuThermalMetrics()
                val net = networkCollector.getNetworkMetrics()

                val payload = AndroidTelemetry(
                    batteryPct = battery.percentage,
                    batteryTempC = battery.tempC,
                    isCharging = battery.isCharging,
                    chargingSource = battery.source,
                    batteryVoltageMv = battery.voltageMv,
                    batteryCurrentMa = battery.currentMa,
                    batteryPowerWatts = battery.powerWatts,
                    batteryHealth = battery.health,
                    batteryTechnology = battery.technology,
                    batteryChargeCounterMah = battery.chargeCounterMah,
                    batteryEstimatedFullMah = battery.estimatedFullMah,
                    batteryChargeTimeRemainingMs = battery.chargeTimeRemainingMs,
                    batteryCycleCount = battery.cycleCount,
                    ramUsedBytes = memory.usedRamBytes,
                    ramTotalBytes = memory.totalRamBytes,
                    ramAvailableBytes = memory.availableRamBytes,
                    ramLowMemory = memory.isLowMemory,
                    storageUsedBytes = memory.usedStorageBytes,
                    storageTotalBytes = memory.totalStorageBytes,
                    storageAvailableBytes = memory.availableStorageBytes,
                    cpuCoreFreqsMhz = cpu.coreFreqsMhz,
                    cpuCoreMinFreqsMhz = cpu.minFreqsMhz,
                    cpuCoreMaxFreqsMhz = cpu.maxFreqsMhz,
                    cpuAvgFreqMhz = cpu.avgFreqMhz,
                    cpuPeakFreqMhz = cpu.peakFreqMhz,
                    cpuFrequencyLoadPct = cpu.frequencyLoadPct,
                    thermalStatus = cpu.thermalStatus,
                    thermalHeadroom = cpu.thermalHeadroom,
                    netType = net.netType,
                    ipAddress = net.ipAddress,
                    rxBytesPerSec = net.rxBytesPerSec,
                    txBytesPerSec = net.txBytesPerSec,
                    totalRxBytes = net.totalRxBytes,
                    totalTxBytes = net.totalTxBytes,
                    linkDownstreamKbps = net.linkDownstreamKbps,
                    linkUpstreamKbps = net.linkUpstreamKbps,
                    uptimeMillis = SystemClock.elapsedRealtime(),
                    awakeUptimeMillis = SystemClock.uptimeMillis()
                )

                var score = 100
                if (battery.tempC >= 42f) score -= 25 else if (battery.tempC >= 38f) score -= 10
                if (cpu.thermalStatus >= 3) score -= 25 else if (cpu.thermalStatus >= 2) score -= 10
                val ramPct = if (memory.totalRamBytes > 0L) memory.usedRamBytes.toFloat() / memory.totalRamBytes else 0f
                if (ramPct >= 0.90f || memory.isLowMemory) score -= 20 else if (ramPct >= 0.80f) score -= 10
                val storagePct = if (memory.totalStorageBytes > 0L) memory.usedStorageBytes.toFloat() / memory.totalStorageBytes else 0f
                if (storagePct >= 0.95f) score -= 15

                Result.success(
                    TelemetrySnapshot(
                        deviceId = device.id,
                        timestamp = System.currentTimeMillis(),
                        healthScore = score.coerceIn(0, 100),
                        payload = payload
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeConnectionStatus(device: UnifiedDevice): Flow<ConnectionStatus> = flow {
        emit(ConnectionStatus.ONLINE)
    }
}
