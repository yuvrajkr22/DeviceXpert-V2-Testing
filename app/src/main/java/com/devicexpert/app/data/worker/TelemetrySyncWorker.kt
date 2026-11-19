package com.devicexpert.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devicexpert.app.data.connector.LocalAndroidDeviceConnector
import com.devicexpert.app.data.mapper.deviceJson
import com.devicexpert.app.data.mapper.toEntity
import com.devicexpert.app.data.remote.supabase.SupabaseTelemetryDto
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.repository.BatterySessionRepository
import com.devicexpert.app.domain.repository.DeviceRepository
import com.devicexpert.app.domain.repository.TelemetryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class TelemetrySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val localConnector: LocalAndroidDeviceConnector,
    private val deviceRepository: DeviceRepository,
    private val telemetryRepository: TelemetryRepository,
    private val batterySessionRepository: BatterySessionRepository,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val localHostDevice = deviceRepository.observeAllDevices()
                .firstOrNull()
                ?.firstOrNull { it.isLocalHost }
                ?: return@withContext Result.success()

            val snapshot = localConnector.fetchSnapshotOnce(localHostDevice).getOrElse {
                return@withContext Result.retry()
            }

            telemetryRepository.saveSnapshot(snapshot)
            deviceRepository.updateConnectionStatus(
                deviceId = localHostDevice.id,
                status = ConnectionStatus.ONLINE,
                lastSeen = snapshot.timestamp
            )

            val androidPayload = snapshot.payload as? AndroidTelemetry
            if (androidPayload != null) {
                batterySessionRepository.recordSample(
                    deviceId = localHostDevice.id,
                    isCharging = androidPayload.isCharging,
                    batteryPctInt = (androidPayload.batteryPct * 100f).toInt(),
                    tempC = androidPayload.batteryTempC,
                    currentMa = androidPayload.batteryCurrentMa,
                    timestampMillis = snapshot.timestamp
                )
            }

            val sevenDaysAgo = snapshot.timestamp - (7L * 24L * 60L * 60L * 1000L)
            val fourteenDaysAgo = snapshot.timestamp - (14L * 24L * 60L * 60L * 1000L)
            telemetryRepository.pruneOldSnapshots(sevenDaysAgo)
            batterySessionRepository.pruneOldSessions(fourteenDaysAgo)

            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val isoTime = Instant.ofEpochMilli(snapshot.timestamp)
                    .atZone(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)
                val dto = SupabaseTelemetryDto(
                    deviceId = localHostDevice.id,
                    recordedAtIso = isoTime,
                    healthScore = snapshot.healthScore,
                    payload = deviceJson.decodeFromString<JsonElement>(snapshot.toEntity().payloadJson)
                )
                supabaseClient.postgrest["telemetry_logs"].insert(dto)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
