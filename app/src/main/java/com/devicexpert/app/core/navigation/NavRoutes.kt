package com.devicexpert.app.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Dashboard : Route
    @Serializable data class DeviceDetails(val deviceId: String) : Route
    @Serializable data class RamDetail(val deviceId: String) : Route
    @Serializable data class BatteryDetail(val deviceId: String) : Route
    @Serializable data class CpuDetail(val deviceId: String) : Route
    @Serializable data class StorageDetail(val deviceId: String) : Route
    @Serializable data class NetworkDetail(val deviceId: String) : Route
    @Serializable data class ThermalDetail(val deviceId: String) : Route
    @Serializable data class SensorsDetail(val deviceId: String) : Route
    @Serializable data class DeviceInfoDetail(val deviceId: String) : Route
    @Serializable data class LiveMonitor(val deviceId: String) : Route
    @Serializable data class Analytics(val deviceId: String) : Route
    @Serializable data object Devices : Route
    @Serializable data object Alerts : Route
    @Serializable data object Settings : Route
}
