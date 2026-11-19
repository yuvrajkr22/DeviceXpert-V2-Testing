package com.devicexpert.app.data.mapper

import com.devicexpert.app.data.local.entity.DeviceEntity
import com.devicexpert.app.data.local.entity.DeviceMetadataDto
import com.devicexpert.app.domain.model.device.AndroidMetadata
import com.devicexpert.app.domain.model.device.ComputerMetadata
import com.devicexpert.app.domain.model.device.IotMetadata
import com.devicexpert.app.domain.model.device.UnifiedDevice
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val deviceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

fun DeviceEntity.toDomain(): UnifiedDevice {
    val metadataDto = deviceJson.decodeFromString<DeviceMetadataDto>(this.metadataJson)
    val metadataDomain = when (metadataDto) {
        is DeviceMetadataDto.Android -> AndroidMetadata(
            manufacturer = metadataDto.manufacturer,
            model = metadataDto.model,
            androidVersion = metadataDto.androidVersion,
            sdkInt = metadataDto.sdkInt,
            socModel = metadataDto.socModel,
            totalRamBytes = metadataDto.totalRamBytes,
            totalStorageBytes = metadataDto.totalStorageBytes,
            sensorCount = metadataDto.sensorCount,
            brand = metadataDto.brand,
            board = metadataDto.board,
            hardware = metadataDto.hardware,
            securityPatch = metadataDto.securityPatch,
            supportedAbis = metadataDto.supportedAbis,
            cpuCoreCount = metadataDto.cpuCoreCount,
            displayWidthPx = metadataDto.displayWidthPx,
            displayHeightPx = metadataDto.displayHeightPx,
            displayDensityDpi = metadataDto.displayDensityDpi,
            displayDiagonalInches = metadataDto.displayDiagonalInches,
            displayRefreshRateHz = metadataDto.displayRefreshRateHz,
            kernelVersion = metadataDto.kernelVersion
        )
        is DeviceMetadataDto.Computer -> ComputerMetadata(
            hostname = metadataDto.hostname,
            osName = metadataDto.osName,
            osVersion = metadataDto.osVersion,
            cpuModel = metadataDto.cpuModel,
            coreCount = metadataDto.coreCount,
            totalRamBytes = metadataDto.totalRamBytes,
            totalDiskBytes = metadataDto.totalDiskBytes,
            agentVersion = metadataDto.agentVersion
        )
        is DeviceMetadataDto.Iot -> IotMetadata(
            boardModel = metadataDto.boardModel,
            firmwareVersion = metadataDto.firmwareVersion,
            macAddress = metadataDto.macAddress,
            ipAddress = metadataDto.ipAddress,
            mqttTopicPrefix = metadataDto.mqttTopicPrefix,
            capabilities = metadataDto.capabilities
        )
    }

    return UnifiedDevice(
        id = this.id,
        name = this.name,
        category = this.category,
        status = this.connectionStatus,
        isLocalHost = this.isLocalHost,
        lastSeenMillis = this.lastSeenMillis,
        metadata = metadataDomain,
        latestTelemetry = null
    )
}

fun UnifiedDevice.toEntity(): DeviceEntity {
    val metadataDto: DeviceMetadataDto = when (val meta = this.metadata) {
        is AndroidMetadata -> DeviceMetadataDto.Android(
            manufacturer = meta.manufacturer,
            model = meta.model,
            androidVersion = meta.androidVersion,
            sdkInt = meta.sdkInt,
            socModel = meta.socModel,
            totalRamBytes = meta.totalRamBytes,
            totalStorageBytes = meta.totalStorageBytes,
            sensorCount = meta.sensorCount,
            brand = meta.brand,
            board = meta.board,
            hardware = meta.hardware,
            securityPatch = meta.securityPatch,
            supportedAbis = meta.supportedAbis,
            cpuCoreCount = meta.cpuCoreCount,
            displayWidthPx = meta.displayWidthPx,
            displayHeightPx = meta.displayHeightPx,
            displayDensityDpi = meta.displayDensityDpi,
            displayDiagonalInches = meta.displayDiagonalInches,
            displayRefreshRateHz = meta.displayRefreshRateHz,
            kernelVersion = meta.kernelVersion
        )
        is ComputerMetadata -> DeviceMetadataDto.Computer(
            hostname = meta.hostname,
            osName = meta.osName,
            osVersion = meta.osVersion,
            cpuModel = meta.cpuModel,
            coreCount = meta.coreCount,
            totalRamBytes = meta.totalRamBytes,
            totalDiskBytes = meta.totalDiskBytes,
            agentVersion = meta.agentVersion
        )
        is IotMetadata -> DeviceMetadataDto.Iot(
            boardModel = meta.boardModel,
            firmwareVersion = meta.firmwareVersion,
            macAddress = meta.macAddress,
            ipAddress = meta.ipAddress,
            mqttTopicPrefix = meta.mqttTopicPrefix,
            capabilities = meta.capabilities
        )
    }

    return DeviceEntity(
        id = this.id,
        name = this.name,
        category = this.category,
        connectionStatus = this.status,
        isLocalHost = this.isLocalHost,
        lastSeenMillis = this.lastSeenMillis,
        metadataJson = deviceJson.encodeToString(metadataDto)
    )
}
