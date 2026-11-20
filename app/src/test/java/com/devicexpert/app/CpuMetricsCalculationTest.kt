package com.devicexpert.app

import com.devicexpert.app.data.collector.CpuThermalCollector
import com.devicexpert.app.data.local.entity.TelemetryEntity
import com.devicexpert.app.data.mapper.toDomain
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuMetricsCalculationTest {

    @Test
    fun unisocT612_sixAt768AndTwoAt1820_computesExactAverageAndPeak() {
        val freqs = mapOf(
            0 to 768, 1 to 768, 2 to 768, 3 to 768, 4 to 768, 5 to 768,
            6 to 1820, 7 to 1820
        )

        val snapshot = CpuThermalCollector.collectCpuFrequencies(
            presentCpuListRaw = "0-7",
            onlineCpuListRaw = "0-7",
            fallbackCoreCount = 8,
            readCoreOnlineFlag = { true },
            readCurrentFreqMhz = { freqs[it] },
            readMinFreqMhz = { 768 },
            readMaxFreqMhz = { 1820 }
        )

        // (6 * 768 + 2 * 1820) / 8 = 8248 / 8 = 1031 MHz
        assertEquals(8, snapshot.coreFreqsMhz.size)
        assertEquals(1031, snapshot.avgFreqMhz)
        assertEquals(1820, snapshot.peakFreqMhz)
        assertEquals(8248f / (8f * 1820f), snapshot.frequencyLoadPct ?: 0f, 0.0001f)
    }

    @Test
    fun heterogeneousCores_differentFrequenciesAndCeilings_computesRoundedAverageAndPeak() {
        // 4 little cores, 3 performance cores, 1 prime core
        val cur = listOf(800, 950, 1100, 1200, 1650, 1800, 2100, 2850)
        val max = listOf(1800, 1800, 1800, 1800, 2400, 2400, 2400, 3000)

        val snapshot = CpuThermalCollector.collectCpuFrequencies(
            presentCpuListRaw = "0-7",
            onlineCpuListRaw = "0-7",
            fallbackCoreCount = 8,
            readCoreOnlineFlag = { true },
            readCurrentFreqMhz = { cur[it] },
            readMinFreqMhz = { 600 },
            readMaxFreqMhz = { max[it] }
        )

        // Sum = 12450 / 8 = 1556.25 -> 1556 MHz
        assertEquals(1556, snapshot.avgFreqMhz)
        assertEquals(2850, snapshot.peakFreqMhz)
        assertEquals(cur, snapshot.coreFreqsMhz)
    }

    @Test
    fun offlineCores_excludedFromAverageAndPeakAndNotRead() {
        val readCalls = mutableListOf<Int>()
        val staleOrLiveFreqs = mapOf(
            0 to 768, 1 to 768, 2 to 768, 3 to 768, 4 to 768, 5 to 768,
            6 to 1820, 7 to 1820 // Offline cores must not be read or included
        )

        val snapshot = CpuThermalCollector.collectCpuFrequencies(
            presentCpuListRaw = "0-7",
            onlineCpuListRaw = "0-5",
            fallbackCoreCount = 6, // Simulates availableProcessors() shrinking during hotplug
            readCoreOnlineFlag = { it <= 5 },
            readCurrentFreqMhz = { coreId ->
                readCalls.add(coreId)
                staleOrLiveFreqs[coreId]
            },
            readMinFreqMhz = { 768 },
            readMaxFreqMhz = { 1820 }
        )

        assertEquals(listOf(0, 1, 2, 3, 4, 5), readCalls)
        assertEquals(listOf(768, 768, 768, 768, 768, 768, 0, 0), snapshot.coreFreqsMhz)
        assertEquals(768, snapshot.avgFreqMhz)
        assertEquals(768, snapshot.peakFreqMhz)
    }

    @Test
    fun unreadableFrequencyFiles_excludedWithoutSkewingAverageWithZeros() {
        // Cores 0..3 readable, cores 4..7 unreadable (SELinux / I/O error returns null)
        val snapshot = CpuThermalCollector.collectCpuFrequencies(
            presentCpuListRaw = "0-7",
            onlineCpuListRaw = "0-7",
            fallbackCoreCount = 8,
            readCoreOnlineFlag = { true },
            readCurrentFreqMhz = { coreId -> if (coreId < 4) 1200 else null },
            readMinFreqMhz = { 768 },
            readMaxFreqMhz = { 1820 }
        )

        assertEquals(listOf(1200, 1200, 1200, 1200, 0, 0, 0, 0), snapshot.coreFreqsMhz)
        assertEquals(1200, snapshot.avgFreqMhz)
        assertEquals(1200, snapshot.peakFreqMhz)
    }

    @Test
    fun allReadingsUnavailable_returnsNullWithoutFabricatingValues() {
        val snapshot = CpuThermalCollector.collectCpuFrequencies(
            presentCpuListRaw = "0-7",
            onlineCpuListRaw = "0-7",
            fallbackCoreCount = 8,
            readCoreOnlineFlag = { true },
            readCurrentFreqMhz = { null },
            readMinFreqMhz = { 0 },
            readMaxFreqMhz = { 0 }
        )

        assertEquals(List(8) { 0 }, snapshot.coreFreqsMhz)
        assertNull(snapshot.avgFreqMhz)
        assertNull(snapshot.peakFreqMhz)
        assertNull(snapshot.frequencyLoadPct)
    }

    @Test
    fun parseCpuList_handlesRangesDisjointSetsAndInvalidInput() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), CpuThermalCollector.parseCpuList("0-7\n"))
        assertEquals(listOf(0, 1, 2, 3, 6, 7), CpuThermalCollector.parseCpuList("0-3,6-7"))
        assertEquals(listOf(0, 2, 5), CpuThermalCollector.parseCpuList("0, 2, 5"))
        assertEquals(emptyList<Int>(), CpuThermalCollector.parseCpuList(null))
        assertEquals(emptyList<Int>(), CpuThermalCollector.parseCpuList(""))
        assertEquals(emptyList<Int>(), CpuThermalCollector.parseCpuList("7-0,abc"))
    }

    @Test
    fun legacyRoomJsonWithoutAvgOrPeakFields_deserializesBackwardCompatibly() {
        val legacyJson = """
            {
              "type": "ANDROID",
              "batteryPct": 0.85,
              "batteryTempC": 32.0,
              "isCharging": false,
              "chargingSource": "BATTERY",
              "ramUsedBytes": 2147483648,
              "storageUsedBytes": 32000000000,
              "cpuCoreFreqsMhz": [768, 768, 768, 768, 768, 768, 1820, 1820],
              "thermalStatus": 0,
              "netType": "WIFI",
              "rxBytesPerSec": 0,
              "txBytesPerSec": 0,
              "uptimeMillis": 600000
            }
        """.trimIndent()

        val entity = TelemetryEntity(
            snapshotId = 1L,
            deviceId = "local_android_host",
            timestamp = 1700000000000L,
            healthScore = 95,
            payloadJson = legacyJson
        )

        val domain = entity.toDomain()
        assertTrue(domain.payload is AndroidTelemetry)
        val payload = domain.payload as AndroidTelemetry
        assertEquals(1031, payload.cpuAvgFreqMhz)
        assertEquals(1820, payload.cpuPeakFreqMhz)
    }
}
