package com.devicexpert.app.data.repository

import com.devicexpert.app.data.local.dao.TelemetryDao
import com.devicexpert.app.data.mapper.toDomain
import com.devicexpert.app.data.mapper.toEntity
import com.devicexpert.app.domain.model.telemetry.TelemetrySnapshot
import com.devicexpert.app.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRepositoryImpl @Inject constructor(
    private val telemetryDao: TelemetryDao
) : TelemetryRepository {

    override fun observeTelemetryHistory(deviceId: String, sinceMillis: Long): Flow<List<TelemetrySnapshot>> =
        telemetryDao.observeTelemetryHistory(deviceId, sinceMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun saveSnapshot(snapshot: TelemetrySnapshot): Result<Unit> = runCatching {
        telemetryDao.insertSnapshot(snapshot.toEntity())
    }

    override suspend fun pruneOldSnapshots(beforeMillis: Long): Result<Int> = runCatching {
        telemetryDao.pruneOldSnapshots(beforeMillis)
    }
}
