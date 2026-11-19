package com.devicexpert.app.data.collector

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuThermalCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getCpuThermalMetrics(): CpuThermalResult {
        val numCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val curFreqs = mutableListOf<Int>()
        val minFreqs = mutableListOf<Int>()
        val maxFreqs = mutableListOf<Int>()

        var sumCur = 0L
        var sumMax = 0L

        for (i in 0 until numCores) {
            val cur = readFreqMhz("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val min = readFreqMhz("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
            val max = readFreqMhz("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")

            curFreqs.add(cur)
            minFreqs.add(min)
            maxFreqs.add(max)

            if (cur > 0 && max > 0) {
                sumCur += cur
                sumMax += max
            }
        }

        val frequencyLoadPct = if (sumMax > 0L) (sumCur.toFloat() / sumMax.toFloat()).coerceIn(0f, 1f) else null

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) powerManager.currentThermalStatus else 0

        val thermalHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                powerManager.getThermalHeadroom(10).takeIf { !it.isNaN() && it >= 0f }
            } catch (_: Exception) {
                null
            }
        } else null

        return CpuThermalResult(
            coreFreqsMhz = curFreqs,
            minFreqsMhz = minFreqs,
            maxFreqsMhz = maxFreqs,
            frequencyLoadPct = frequencyLoadPct,
            thermalStatus = thermalStatus,
            thermalHeadroom = thermalHeadroom
        )
    }

    private fun readFreqMhz(path: String): Int = try {
        val file = File(path)
        if (file.exists() && file.canRead()) (file.readText().trim().toIntOrNull() ?: 0) / 1000 else 0
    } catch (_: Exception) {
        0
    }

    data class CpuThermalResult(
        val coreFreqsMhz: List<Int>,
        val minFreqsMhz: List<Int>,
        val maxFreqsMhz: List<Int>,
        val frequencyLoadPct: Float?,
        val thermalStatus: Int,
        val thermalHeadroom: Float?
    )
}
