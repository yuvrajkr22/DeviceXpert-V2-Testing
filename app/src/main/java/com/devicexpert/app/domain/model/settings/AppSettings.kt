package com.devicexpert.app.domain.model.settings

data class MqttBrokerConfig(
    val host: String = "broker.hivemq.com",
    val port: Int = 8883,
    val useTls: Boolean = true,
    val username: String = "",
    val password: String = ""
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val useFahrenheit: Boolean = false,
    val livePollingIntervalMs: Long = 1000L,
    val mqttConfig: MqttBrokerConfig = MqttBrokerConfig()
)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String, val email: String) : AuthState
}
