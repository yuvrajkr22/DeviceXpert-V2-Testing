package com.devicexpert.app.domain.repository

import com.devicexpert.app.domain.model.battery.BatterySession
import kotlinx.coroutines.flow.Flow

interface BatterySessionRepository {
    fun observeRecentSessions(deviceId: String, limit: Int = 20): Flow<List<BatterySession>>
    suspend fun recordSample(
        deviceId: String,
        isCharging: Boolean,
        batteryPctInt: Int,
        tempC: Float,
        currentMa: Float?,
        timestampMillis: Long
    )
    suspend fun pruneOldSessions(beforeMillis: Long): Int
}
