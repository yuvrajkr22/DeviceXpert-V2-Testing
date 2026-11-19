package com.devicexpert.app.core.common

import java.util.Locale
import kotlin.math.roundToInt

object TelemetryFormatters {

    fun formatBytes(bytes: Long, decimals: Int = 1): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        val tb = gb * 1024.0
        return when {
            bytes >= tb -> String.format(Locale.US, "%.${decimals}f TB", bytes / tb)
            bytes >= gb -> String.format(Locale.US, "%.${decimals}f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.0f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatTotalCapacityGb(bytes: Long): String {
        if (bytes <= 0L) return "N/A"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return when {
            gb >= 10.0 -> "${gb.roundToInt()} GB"
            else -> String.format(Locale.US, "%.1f GB", gb)
        }
    }

    fun formatSpeedBytesPerSec(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0.0 B/s"
        val kb = 1024.0
        val mb = kb * 1024.0
        return when {
            bytesPerSec >= mb -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / mb)
            bytesPerSec >= kb -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / kb)
            else -> String.format(Locale.US, "%.1f B/s", bytesPerSec.toDouble())
        }
    }

    fun formatFrequencyMhz(mhz: Int): String {
        if (mhz <= 0) return "-- Mhz"
        val thousands = mhz / 1000
        val remainder = mhz % 1000
        return if (thousands > 0) {
            String.format(Locale.US, "%d %03d Mhz", thousands, remainder)
        } else {
            "$mhz Mhz"
        }
    }

    fun formatTemperature(tempC: Float, useFahrenheit: Boolean = false): String {
        return if (useFahrenheit) {
            val f = (tempC * 9f / 5f) + 32f
            String.format(Locale.US, "%.1f°F", f)
        } else {
            String.format(Locale.US, "%.1f°C", tempC)
        }
    }

    fun formatDurationMillis(millis: Long): String {
        if (millis <= 0L) return "0m"
        val totalMinutes = millis / 60_000L
        val days = totalMinutes / (60L * 24L)
        val hours = (totalMinutes % (60L * 24L)) / 60L
        val minutes = totalMinutes % 60L
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m")
        }.trim()
    }
}
