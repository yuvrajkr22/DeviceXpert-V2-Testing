package com.devicexpert.app.domain.model.device

sealed interface DeviceMetadata

data class AndroidMetadata(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val socModel: String,
    val totalRamBytes: Long,
    val totalStorageBytes: Long,
    val sensorCount: Int,
    val brand: String = "",
    val board: String = "",
    val hardware: String = "",
    val securityPatch: String = "",
    val supportedAbis: List<String> = emptyList(),
    val cpuCoreCount: Int = 0,
    val displayWidthPx: Int = 0,
    val displayHeightPx: Int = 0,
    val displayDensityDpi: Int = 0,
    val displayDiagonalInches: Float = 0f,
    val displayRefreshRateHz: Float = 60f,
    val kernelVersion: String = ""
) : DeviceMetadata

data class ComputerMetadata(
    val hostname: String,
    val osName: String,
    val osVersion: String,
    val cpuModel: String,
    val coreCount: Int,
    val totalRamBytes: Long,
    val totalDiskBytes: Long,
    val agentVersion: String
) : DeviceMetadata

data class IotMetadata(
    val boardModel: String,
    val firmwareVersion: String,
    val macAddress: String,
    val ipAddress: String,
    val mqttTopicPrefix: String,
    val capabilities: List<String>
) : DeviceMetadata
