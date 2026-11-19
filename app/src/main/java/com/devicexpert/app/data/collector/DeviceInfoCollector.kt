package com.devicexpert.app.data.collector

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import com.devicexpert.app.domain.model.device.AndroidMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class DeviceInfoCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryStorageCollector: MemoryStorageCollector,
    private val sensorCollector: SensorCollector
) {
    fun collectAndroidMetadata(): AndroidMetadata {
        val memStorage = memoryStorageCollector.getMemoryStorageMetrics()
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val defaultDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val displayMode = defaultDisplay?.mode
        val metrics = context.resources.displayMetrics

        val widthPx = displayMode?.physicalWidth ?: metrics.widthPixels
        val heightPx = displayMode?.physicalHeight ?: metrics.heightPixels
        val refreshHz = displayMode?.refreshRate ?: 60f
        val densityDpi = metrics.densityDpi

        val xInches = if (metrics.xdpi > 0f) widthPx / metrics.xdpi else 0f
        val yInches = if (metrics.ydpi > 0f) heightPx / metrics.ydpi else 0f
        val diagonalInches = if (xInches > 0f && yInches > 0f) {
            sqrt(xInches.pow(2) + yInches.pow(2))
        } else 0f

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        val kernelVersion = System.getProperty("os.version") ?: "Unknown"

        return AndroidMetadata(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            socModel = socModel,
            totalRamBytes = memStorage.totalRamBytes,
            totalStorageBytes = memStorage.totalStorageBytes,
            sensorCount = sensorCollector.getSensorCount(),
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            displayWidthPx = widthPx,
            displayHeightPx = heightPx,
            displayDensityDpi = densityDpi,
            displayDiagonalInches = diagonalInches,
            displayRefreshRateHz = refreshHz,
            kernelVersion = kernelVersion
        )
    }
}
