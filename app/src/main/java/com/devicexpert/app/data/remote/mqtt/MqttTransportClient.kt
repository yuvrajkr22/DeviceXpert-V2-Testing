package com.devicexpert.app.data.remote.mqtt

import com.devicexpert.app.domain.model.settings.MqttBrokerConfig
import com.devicexpert.app.domain.repository.SettingsRepository
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MqttTransportClient @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private val mutex = Mutex()
    private var activeClient: Mqtt5AsyncClient? = null
    private var activeConfig: MqttBrokerConfig? = null

    private suspend fun getConnectedClient(): Mqtt5AsyncClient = mutex.withLock {
        val desiredConfig = settingsRepository.observeAppSettings().first().mqttConfig
        val existing = activeClient

        if (existing != null && activeConfig == desiredConfig && existing.state.isConnected) {
            return existing
        }

        if (existing != null) {
            runCatching { existing.disconnect().awaitResult() }
            activeClient = null
        }

        var builder = MqttClient.builder()
            .useMqttVersion5()
            .identifier("devicexpert-${UUID.randomUUID()}")
            .serverHost(desiredConfig.host)
            .serverPort(desiredConfig.port)

        if (desiredConfig.useTls) {
            builder = builder.sslWithDefaultConfig()
        }

        if (desiredConfig.username.isNotBlank()) {
            builder = builder.simpleAuth()
                .username(desiredConfig.username)
                .password(desiredConfig.password.toByteArray(StandardCharsets.UTF_8))
                .applySimpleAuth()
        }

        val newClient = builder.buildAsync()
        newClient.connect().awaitResult()
        activeClient = newClient
        activeConfig = desiredConfig
        return newClient
    }

    fun subscribeTopic(topic: String): Flow<String> = callbackFlow {
        val client = getConnectedClient()

        client.subscribeWith()
            .topicFilter(topic)
            .callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                trySend(payload)
            }
            .send()
            .awaitResult()

        awaitClose {
            runCatching {
                client.unsubscribeWith().topicFilter(topic).send()
            }
        }
    }

    private suspend fun <T> CompletableFuture<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        whenComplete { result, error ->
            if (error != null) cont.resumeWithException(error) else cont.resume(result)
        }
        cont.invokeOnCancellation { cancel(false) }
    }
}
