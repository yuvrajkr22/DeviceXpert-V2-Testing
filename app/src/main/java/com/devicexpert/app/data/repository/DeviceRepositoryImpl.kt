package com.devicexpert.app.data.repository

import com.devicexpert.app.data.local.dao.DeviceDao
import com.devicexpert.app.data.mapper.toDomain
import com.devicexpert.app.data.mapper.toEntity
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.UnifiedDevice
import com.devicexpert.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override fun observeAllDevices(): Flow<List<UnifiedDevice>> =
        deviceDao.observeAllDevices().map { list -> list.map { it.toDomain() } }

    override fun observeDevice(deviceId: String): Flow<UnifiedDevice?> =
        deviceDao.observeDevice(deviceId).map { it?.toDomain() }

    override suspend fun getDeviceById(deviceId: String): UnifiedDevice? =
        deviceDao.getDeviceById(deviceId)?.toDomain()

    override suspend fun registerDevice(device: UnifiedDevice): Result<Unit> = runCatching {
        deviceDao.insertOrUpdateDevice(device.toEntity())
    }

    override suspend fun updateConnectionStatus(deviceId: String, status: ConnectionStatus, lastSeen: Long): Result<Unit> = runCatching {
        deviceDao.updateConnectionStatus(deviceId, status.name, lastSeen)
    }

    override suspend fun removeDevice(deviceId: String): Result<Unit> = runCatching {
        deviceDao.deleteDevice(deviceId)
    }
}
