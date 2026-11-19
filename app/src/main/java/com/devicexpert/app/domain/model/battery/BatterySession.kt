package com.devicexpert.app.domain.model.battery

data class BatterySession(
    val sessionId: Long = 0L,
    val deviceId: String,
    val isCharging: Boolean,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val startPct: Int,
    val endPct: Int,
    val maxTempC: Float,
    val avgCurrentMa: Float?
) {
    val durationMillis: Long get() = (endTimeMillis - startTimeMillis).coerceAtLeast(0L)
    val deltaPct: Int get() = endPct - startPct
}
