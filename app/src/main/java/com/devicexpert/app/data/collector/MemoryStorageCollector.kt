package com.devicexpert.app.data.collector

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryStorageCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getMemoryStorageMetrics(): MemoryStorageResult {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem.coerceAtLeast(0L)
        val availRam = memoryInfo.availMem.coerceAtLeast(0L)
        val usedRam = (totalRam - availRam).coerceAtLeast(0L)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorage = statFs.totalBytes.coerceAtLeast(0L)
        val availableStorage = statFs.availableBytes.coerceAtLeast(0L)
        val usedStorage = (totalStorage - availableStorage).coerceAtLeast(0L)

        return MemoryStorageResult(
            usedRamBytes = usedRam,
            availableRamBytes = availRam,
            totalRamBytes = totalRam,
            isLowMemory = memoryInfo.lowMemory,
            lowMemoryThresholdBytes = memoryInfo.threshold,
            usedStorageBytes = usedStorage,
            availableStorageBytes = availableStorage,
            totalStorageBytes = totalStorage
        )
    }

    data class MemoryStorageResult(
        val usedRamBytes: Long,
        val availableRamBytes: Long,
        val totalRamBytes: Long,
        val isLowMemory: Boolean,
        val lowMemoryThresholdBytes: Long,
        val usedStorageBytes: Long,
        val availableStorageBytes: Long,
        val totalStorageBytes: Long
    )
}
