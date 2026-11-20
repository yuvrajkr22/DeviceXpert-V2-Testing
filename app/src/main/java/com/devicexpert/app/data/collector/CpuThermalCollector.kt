package com.devicexpert.app.data.collector

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class CpuThermalCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Cache only static hardware frequency bounds once positively read; never cache live scaling_cur_freq.
    private val cachedMinFreqMhz = ConcurrentHashMap<Int, Int>()
    private val cachedMaxFreqMhz = ConcurrentHashMap<Int, Int>()

    fun getCpuThermalMetrics(): CpuThermalResult {
        val freqSnapshot = collectCpuFrequencies(
            presentCpuListRaw = readTextOrNull(CPU_PRESENT_PATH),
            onlineCpuListRaw = readTextOrNull(CPU_ONLINE_PATH),
            fallbackCoreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            readCoreOnlineFlag = { coreId -> readCoreOnlineStatus(coreId) },
            readCurrentFreqMhz = { coreId ->
                readFreqMhz("/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq")
            },
            readMinFreqMhz = { coreId -> getStaticMinFreqMhz(coreId) },
            readMaxFreqMhz = { coreId -> getStaticMaxFreqMhz(coreId) }
        )

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            0
        }

        val thermalHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                powerManager.getThermalHeadroom(10).takeIf { !it.isNaN() && it >= 0f }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        return CpuThermalResult(
            coreFreqsMhz = freqSnapshot.coreFreqsMhz,
            minFreqsMhz = freqSnapshot.minFreqsMhz,
            maxFreqsMhz = freqSnapshot.maxFreqsMhz,
            avgFreqMhz = freqSnapshot.avgFreqMhz,
            peakFreqMhz = freqSnapshot.peakFreqMhz,
            frequencyLoadPct = freqSnapshot.frequencyLoadPct,
            thermalStatus = thermalStatus,
            thermalHeadroom = thermalHeadroom
        )
    }

    private fun getStaticMinFreqMhz(coreId: Int): Int {
        cachedMinFreqMhz[coreId]?.let { return it }
        val value = readFreqMhz("/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_min_freq") ?: 0
        if (value > 0) cachedMinFreqMhz[coreId] = value
        return value
    }

    private fun getStaticMaxFreqMhz(coreId: Int): Int {
        cachedMaxFreqMhz[coreId]?.let { return it }
        val value = readFreqMhz("/sys/devices/system/cpu/cpu$coreId/cpufreq/cpuinfo_max_freq") ?: 0
        if (value > 0) cachedMaxFreqMhz[coreId] = value
        return value
    }

    private fun readCoreOnlineStatus(coreId: Int): Boolean? {
        val raw = readTextOrNull("/sys/devices/system/cpu/cpu$coreId/online") ?: return null
        return when (raw) {
            "1" -> true
            "0" -> false
            else -> null
        }
    }

    private fun readFreqMhz(path: String): Int? {
        val raw = readTextOrNull(path) ?: return null
        val khz = raw.toIntOrNull() ?: return null
        val mhz = khz / 1000
        return mhz.takeIf { it > 0 }
    }

    private fun readTextOrNull(path: String): String? = try {
        val file = File(path)
        if (file.exists() && file.canRead()) {
            file.readText().trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    data class CpuFrequencySnapshot(
        val presentCoreIds: List<Int>,
        val coreFreqsMhz: List<Int>,
        val minFreqsMhz: List<Int>,
        val maxFreqsMhz: List<Int>,
        val avgFreqMhz: Int?,
        val peakFreqMhz: Int?,
        val frequencyLoadPct: Float?
    )

    data class CpuThermalResult(
        val coreFreqsMhz: List<Int>,
        val minFreqsMhz: List<Int>,
        val maxFreqsMhz: List<Int>,
        val avgFreqMhz: Int?,
        val peakFreqMhz: Int?,
        val frequencyLoadPct: Float?,
        val thermalStatus: Int,
        val thermalHeadroom: Float?
    )

    companion object {
        private const val CPU_PRESENT_PATH = "/sys/devices/system/cpu/present"
        private const val CPU_ONLINE_PATH = "/sys/devices/system/cpu/online"

        fun parseCpuList(raw: String?): List<Int> {
            if (raw.isNullOrBlank()) return emptyList()
            val result = mutableListOf<Int>()
            raw.trim().split(",").forEach { token ->
                val part = token.trim()
                if (part.isEmpty()) return@forEach
                val dashIdx = part.indexOf('-')
                if (dashIdx >= 0) {
                    val start = part.substring(0, dashIdx).trim().toIntOrNull()
                    val end = part.substring(dashIdx + 1).trim().toIntOrNull()
                    if (start != null && end != null && start >= 0 && end >= start && (end - start) <= 1024) {
                        for (id in start..end) {
                            result.add(id)
                        }
                    }
                } else {
                    val single = part.toIntOrNull()
                    if (single != null && single >= 0) {
                        result.add(single)
                    }
                }
            }
            return result.distinct().sorted()
        }

        fun computeAverageMhz(validOnlineFreqsMhz: List<Int>): Int? {
            val valid = validOnlineFreqsMhz.filter { it > 0 }
            if (valid.isEmpty()) return null
            return valid.average().roundToInt()
        }

        fun computePeakMhz(validOnlineFreqsMhz: List<Int>): Int? {
            return validOnlineFreqsMhz.filter { it > 0 }.maxOrNull()
        }

        internal fun collectCpuFrequencies(
            presentCpuListRaw: String?,
            onlineCpuListRaw: String?,
            fallbackCoreCount: Int,
            readCoreOnlineFlag: (Int) -> Boolean?,
            readCurrentFreqMhz: (Int) -> Int?,
            readMinFreqMhz: (Int) -> Int,
            readMaxFreqMhz: (Int) -> Int
        ): CpuFrequencySnapshot {
            val parsedPresent = parseCpuList(presentCpuListRaw)
            val parsedOnlineList = parseCpuList(onlineCpuListRaw)
            val onlineSet: Set<Int>? = if (parsedOnlineList.isNotEmpty()) parsedOnlineList.toSet() else null

            val presentCores = when {
                parsedPresent.isNotEmpty() -> parsedPresent
                onlineSet != null -> (0..onlineSet.max()).toList()
                else -> (0 until fallbackCoreCount.coerceAtLeast(1)).toList()
            }

            val curFreqs = mutableListOf<Int>()
            val minFreqs = mutableListOf<Int>()
            val maxFreqs = mutableListOf<Int>()
            val validOnlineReadings = mutableListOf<Int>()

            var sumCur = 0L
            var sumMax = 0L

            for (coreId in presentCores) {
                val isOnline = when {
                    onlineSet != null -> coreId in onlineSet
                    else -> readCoreOnlineFlag(coreId) != false
                }

                val min = readMinFreqMhz(coreId).coerceAtLeast(0)
                val max = readMaxFreqMhz(coreId).coerceAtLeast(0)
                minFreqs.add(min)
                maxFreqs.add(max)

                if (!isOnline) {
                    curFreqs.add(0)
                    continue
                }

                val cur = readCurrentFreqMhz(coreId)?.takeIf { it > 0 } ?: 0
                curFreqs.add(cur)

                if (cur > 0) {
                    validOnlineReadings.add(cur)
                    if (max > 0) {
                        sumCur += cur
                        sumMax += max
                    }
                }
            }

            val avgMhz = computeAverageMhz(validOnlineReadings)
            val peakMhz = computePeakMhz(validOnlineReadings)
            val frequencyLoadPct = if (sumMax > 0L) {
                (sumCur.toFloat() / sumMax.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }

            return CpuFrequencySnapshot(
                presentCoreIds = presentCores,
                coreFreqsMhz = curFreqs,
                minFreqsMhz = minFreqs,
                maxFreqsMhz = maxFreqs,
                avgFreqMhz = avgMhz,
                peakFreqMhz = peakMhz,
                frequencyLoadPct = frequencyLoadPct
            )
        }
    }
}
