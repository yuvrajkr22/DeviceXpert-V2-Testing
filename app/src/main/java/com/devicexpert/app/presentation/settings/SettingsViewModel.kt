package com.devicexpert.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.domain.model.settings.AppSettings
import com.devicexpert.app.domain.model.settings.AuthState
import com.devicexpert.app.domain.model.settings.MqttBrokerConfig
import com.devicexpert.app.domain.repository.AuthRepository
import com.devicexpert.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val authState: AuthState = AuthState.SignedOut,
    val isCloudConfigured: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeAppSettings(),
        authRepository.observeAuthState(),
        statusMessage
    ) { settings, auth, msg -> SettingsUiState(settings, auth, authRepository.isCloudConfigured, msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState(isCloudConfigured = authRepository.isCloudConfigured))

    fun setDarkTheme(enabled: Boolean) { viewModelScope.launch { settingsRepository.setDarkTheme(enabled) } }
    fun setUseFahrenheit(enabled: Boolean) { viewModelScope.launch { settingsRepository.setUseFahrenheit(enabled) } }
    fun setPollingInterval(intervalMs: Long) { viewModelScope.launch { settingsRepository.setLivePollingIntervalMs(intervalMs) } }

    fun saveMqttConfig(host: String, portText: String, useTls: Boolean, username: String, password: String) {
        val port = portText.toIntOrNull() ?: if (useTls) 8883 else 1883
        viewModelScope.launch {
            settingsRepository.saveMqttBrokerConfig(MqttBrokerConfig(host.ifBlank { "broker.hivemq.com" }, port, useTls, username, password))
            statusMessage.value = "MQTT broker credentials encrypted and saved."
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess { statusMessage.value = "Signed in to Supabase cloud sync." }
                .onFailure { statusMessage.value = it.localizedMessage ?: "Sign-in failed." }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signUp(email, password)
                .onSuccess { statusMessage.value = "Account created. Check email if confirmation is enabled." }
                .onFailure { statusMessage.value = it.localizedMessage ?: "Registration failed." }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
                .onSuccess { statusMessage.value = "Signed out." }
                .onFailure { statusMessage.value = it.localizedMessage }
        }
    }
}
