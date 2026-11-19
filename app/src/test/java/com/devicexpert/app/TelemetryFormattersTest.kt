package com.devicexpert.app

import com.devicexpert.app.core.common.TelemetryFormatters
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryFormattersTest {

    @Test
    fun formatBytes_handlesZeroKilobytesMegabytesAndGigabytes() {
        assertEquals("0 B", TelemetryFormatters.formatBytes(0L))
        assertEquals("512 B", TelemetryFormatters.formatBytes(512L))
        assertEquals("2 KB", TelemetryFormatters.formatBytes(2048L))
        assertEquals("705 MB", TelemetryFormatters.formatBytes(705L * 1024L * 1024L))
        assertEquals("127.6 GB", TelemetryFormatters.formatBytes((127.6 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatFrequencyMhz_formatsWithSpaceThousandsSeparator() {
        assertEquals("-- Mhz", TelemetryFormatters.formatFrequencyMhz(0))
        assertEquals("800 Mhz", TelemetryFormatters.formatFrequencyMhz(800))
        assertEquals("1 880 Mhz", TelemetryFormatters.formatFrequencyMhz(1880))
        assertEquals("3 050 Mhz", TelemetryFormatters.formatFrequencyMhz(3050))
    }

    @Test
    fun formatTemperature_supportsCelsiusAndFahrenheit() {
        assertEquals("31.0°C", TelemetryFormatters.formatTemperature(31.0f, useFahrenheit = false))
        assertEquals("87.8°F", TelemetryFormatters.formatTemperature(31.0f, useFahrenheit = true))
    }

    @Test
    fun formatSpeedBytesPerSec_scalesUnitsCorrectly() {
        assertEquals("0.0 B/s", TelemetryFormatters.formatSpeedBytesPerSec(0L))
        assertEquals("1.5 KB/s", TelemetryFormatters.formatSpeedBytesPerSec(1536L))
        assertEquals("2.5 MB/s", TelemetryFormatters.formatSpeedBytesPerSec((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatDurationMillis_formatsDaysHoursMinutes() {
        assertEquals("0m", TelemetryFormatters.formatDurationMillis(0L))
        assertEquals("45m", TelemetryFormatters.formatDurationMillis(45L * 60_000L))
        assertEquals("2h 15m", TelemetryFormatters.formatDurationMillis(135L * 60_000L))
        assertEquals("1d 2h 5m", TelemetryFormatters.formatDurationMillis(((24L + 2L) * 60L + 5L) * 60_000L))
    }
}
