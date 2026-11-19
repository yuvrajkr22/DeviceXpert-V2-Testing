package com.devicexpert.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devicexpert.app.core.security.SecureStorageManager
import com.devicexpert.app.core.security.dataStore
import com.devicexpert.app.domain.model.settings.AppSettings
import com.devicexpert.app.domain.model.settings.MqttBrokerConfig
import com.devicexpert.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorageManager
) : SettingsRepository {

    private val dataStore = context.dataStore

    override fun observeAppSettings(): Flow<AppSettings> = combine(
        dataStore.data,
        secureStorage.getSecureString(KEY_MQTT_USERNAME),
        secureStorage.getSecureString(KEY_MQTT_PASSWORD)
    ) { prefs, username, password ->
        AppSettings(
            darkTheme = prefs[KEY_DARK_THEME] ?: true,
            useFahrenheit = prefs[KEY_USE_FAHRENHEIT] ?: false,
            livePollingIntervalMs = prefs[KEY_POLLING_MS] ?: 1000L,
            mqttConfig = MqttBrokerConfig(
                host = prefs[KEY_MQTT_HOST] ?: "broker.hivemq.com",
                port = prefs[KEY_MQTT_PORT] ?: 8883,
                useTls = prefs[KEY_MQTT_TLS] ?: true,
                username = username.orEmpty(),
                password = password.orEmpty()
            )
        )
    }

    override suspend fun setDarkTheme(enabled: Boolean) { dataStore.edit { it[KEY_DARK_THEME] = enabled } }
    override suspend fun setUseFahrenheit(enabled: Boolean) { dataStore.edit { it[KEY_USE_FAHRENHEIT] = enabled } }
    override suspend fun setLivePollingIntervalMs(intervalMs: Long) { dataStore.edit { it[KEY_POLLING_MS] = intervalMs.coerceIn(500L, 10_000L) } }

    override suspend fun saveMqttBrokerConfig(config: MqttBrokerConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_MQTT_HOST] = config.host.trim()
            prefs[KEY_MQTT_PORT] = config.port.coerceIn(1, 65535)
            prefs[KEY_MQTT_TLS] = config.useTls
        }
        if (config.username.isBlank()) secureStorage.clearSecureString(KEY_MQTT_USERNAME)
        else secureStorage.saveSecureString(KEY_MQTT_USERNAME, config.username.trim())
        if (config.password.isBlank()) secureStorage.clearSecureString(KEY_MQTT_PASSWORD)
        else secureStorage.saveSecureString(KEY_MQTT_PASSWORD, config.password)
    }

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("pref_dark_theme")
        private val KEY_USE_FAHRENHEIT = booleanPreferencesKey("pref_use_fahrenheit")
        private val KEY_POLLING_MS = longPreferencesKey("pref_polling_interval_ms")
        private val KEY_MQTT_HOST = stringPreferencesKey("pref_mqtt_host")
        private val KEY_MQTT_PORT = intPreferencesKey("pref_mqtt_port")
        private val KEY_MQTT_TLS = booleanPreferencesKey("pref_mqtt_tls")
        private const val KEY_MQTT_USERNAME = "secure_mqtt_username"
        private const val KEY_MQTT_PASSWORD = "secure_mqtt_password"
    }
}
