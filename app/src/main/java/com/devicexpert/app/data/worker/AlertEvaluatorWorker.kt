package com.devicexpert.app.data.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devicexpert.app.DeviceXpertApp
import com.devicexpert.app.MainActivity
import com.devicexpert.app.domain.model.alert.AlertEvent
import com.devicexpert.app.domain.model.alert.AlertOperator
import com.devicexpert.app.domain.model.alert.AlertSeverity
import com.devicexpert.app.domain.model.device.AndroidMetadata
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.repository.AlertRepository
import com.devicexpert.app.domain.repository.DeviceRepository
import com.devicexpert.app.domain.repository.TelemetryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

@HiltWorker
class AlertEvaluatorWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val alertRepository: AlertRepository,
    private val telemetryRepository: TelemetryRepository,
    private val deviceRepository: DeviceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val rules = alertRepository.getActiveRules()
        if (rules.isEmpty()) return@withContext Result.success()

        val devices = deviceRepository.observeAllDevices().firstOrNull().orEmpty()
        val windowStart = System.currentTimeMillis() - (20L * 60L * 1000L)

        for (device in devices) {
            val latest = telemetryRepository.observeTelemetryHistory(device.id, windowStart)
                .firstOrNull()
                ?.lastOrNull() ?: continue

            val payload = latest.payload as? AndroidTelemetry ?: continue
            val totalRam = payload.ramTotalBytes.takeIf { it > 0L }
                ?: (device.metadata as? AndroidMetadata)?.totalRamBytes
                ?: 0L

            for (rule in rules.filter { it.deviceId == null || it.deviceId == device.id }) {
                val metricValue = when (rule.metricKey) {
                    "BATTERY_PCT" -> (payload.batteryPct * 100.0)
                    "TEMP_C" -> payload.batteryTempC.toDouble()
                    "RAM_USED_PCT" -> if (totalRam > 0L) (payload.ramUsedBytes.toDouble() / totalRam.toDouble()) * 100.0 else null
                    else -> null
                } ?: continue

                val breached = when (rule.operator) {
                    AlertOperator.GT -> metricValue > rule.thresholdValue
                    AlertOperator.LT -> metricValue < rule.thresholdValue
                    AlertOperator.EQ -> metricValue == rule.thresholdValue
                }

                if (breached) {
                    triggerAlert(device.id, device.name, rule.ruleId, rule.metricKey, metricValue, rule.severity)
                }
            }
        }
        Result.success()
    }

    private suspend fun triggerAlert(
        deviceId: String,
        deviceName: String,
        ruleId: String,
        metric: String,
        value: Double,
        severity: AlertSeverity
    ) {
        val event = AlertEvent(
            alertId = UUID.randomUUID().toString(),
            deviceId = deviceId,
            ruleId = ruleId,
            severity = severity,
            title = "Threshold Breached: $deviceName",
            message = "$metric reached ${"%.1f".format(value)}",
            metricValue = value,
            isAcknowledged = false,
            triggeredAtMillis = System.currentTimeMillis()
        )
        alertRepository.recordEvent(event)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(context, DeviceXpertApp.CHANNEL_CRITICAL_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(event.title)
                .setContentText(event.message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(event.alertId.hashCode(), notification)
        }
    }
}
