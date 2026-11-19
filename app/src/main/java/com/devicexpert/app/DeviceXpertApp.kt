package com.devicexpert.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.data.worker.AlertEvaluatorWorker
import com.devicexpert.app.data.worker.TelemetrySyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class DeviceXpertApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var hostDeviceInitializer: HostDeviceInitializer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        appScope.launch {
            hostDeviceInitializer.ensureHostDeviceRegistered()
        }
        scheduleBackgroundMonitoring()
    }

    private fun scheduleBackgroundMonitoring() {
        val workManager = WorkManager.getInstance(this)
        val telemetryRequest = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_TELEMETRY_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            telemetryRequest
        )
        val alertRequest = PeriodicWorkRequestBuilder<AlertEvaluatorWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_ALERT_EVALUATOR,
            ExistingPeriodicWorkPolicy.KEEP,
            alertRequest
        )
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService<NotificationManager>() ?: return
        val criticalAlertsChannel = NotificationChannel(
            CHANNEL_CRITICAL_ALERTS,
            "Critical Device Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts for thermal, battery, and offline device events"
            enableVibration(true)
        }
        val backgroundSyncChannel = NotificationChannel(
            CHANNEL_BACKGROUND_MONITOR,
            "Background Telemetry Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Status notifications for continuous telemetry monitoring"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannels(listOf(criticalAlertsChannel, backgroundSyncChannel))
    }

    companion object {
        const val CHANNEL_CRITICAL_ALERTS = "devicexpert_critical_alerts"
        const val CHANNEL_BACKGROUND_MONITOR = "devicexpert_background_monitor"
        private const val WORK_TELEMETRY_SYNC = "devicexpert_periodic_telemetry_sync"
        private const val WORK_ALERT_EVALUATOR = "devicexpert_periodic_alert_evaluator"
    }
}
