package com.devicexpert.app.domain.repository

import com.devicexpert.app.domain.model.settings.AppSettings
import com.devicexpert.app.domain.model.settings.MqttBrokerConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeAppSettings(): Flow<AppSettings>
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setUseFahrenheit(enabled: Boolean)
    suspend fun setLivePollingIntervalMs(intervalMs: Long)
    suspend fun saveMqttBrokerConfig(config: MqttBrokerConfig)
}
