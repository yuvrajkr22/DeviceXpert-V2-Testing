package com.devicexpert.app.data.repository

import com.devicexpert.app.data.local.dao.BatterySessionDao
import com.devicexpert.app.data.local.entity.BatterySessionEntity
import com.devicexpert.app.domain.model.battery.BatterySession
import com.devicexpert.app.domain.repository.BatterySessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class BatterySessionRepositoryImpl @Inject constructor(
    private val batterySessionDao: BatterySessionDao
) : BatterySessionRepository {

    override fun observeRecentSessions(deviceId: String, limit: Int): Flow<List<BatterySession>> =
        batterySessionDao.observeRecentSessions(deviceId, limit).map { list ->
            list.map { e ->
                BatterySession(
                    sessionId = e.sessionId,
                    deviceId = e.deviceId,
                    isCharging = e.isCharging,
                    startTimeMillis = e.startTimeMillis,
                    endTimeMillis = e.endTimeMillis,
                    startPct = e.startPct,
                    endPct = e.endPct,
                    maxTempC = e.maxTempC,
                    avgCurrentMa = e.avgCurrentMa
                )
            }
        }

    override suspend fun recordSample(
        deviceId: String,
        isCharging: Boolean,
        batteryPctInt: Int,
        tempC: Float,
        currentMa: Float?,
        timestampMillis: Long
    ) {
        val latest = batterySessionDao.getLatestSession(deviceId)
        val isStaleGap = latest != null && (timestampMillis - latest.endTimeMillis) > MAX_SESSION_GAP_MS

        if (latest == null || latest.isCharging != isCharging || isStaleGap) {
            batterySessionDao.insertSession(
                BatterySessionEntity(
                    deviceId = deviceId,
                    isCharging = isCharging,
                    startTimeMillis = timestampMillis,
                    endTimeMillis = timestampMillis,
                    startPct = batteryPctInt,
                    endPct = batteryPctInt,
                    maxTempC = tempC,
                    avgCurrentMa = currentMa
                )
            )
        } else {
            val blendedCurrent = when {
                latest.avgCurrentMa != null && currentMa != null -> (latest.avgCurrentMa * 0.7f) + (currentMa * 0.3f)
                currentMa != null -> currentMa
                else -> latest.avgCurrentMa
            }
            batterySessionDao.updateSession(
                latest.copy(
                    endTimeMillis = max(latest.endTimeMillis, timestampMillis),
                    endPct = batteryPctInt,
                    maxTempC = max(latest.maxTempC, tempC),
                    avgCurrentMa = blendedCurrent
                )
            )
        }
    }

    override suspend fun pruneOldSessions(beforeMillis: Long): Int =
        batterySessionDao.pruneOldSessions(beforeMillis)

    companion object {
        private const val MAX_SESSION_GAP_MS = 2L * 60L * 60L * 1000L
    }
}
