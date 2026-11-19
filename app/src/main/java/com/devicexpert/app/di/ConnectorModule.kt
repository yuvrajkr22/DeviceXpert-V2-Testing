package com.devicexpert.app.di

import com.devicexpert.app.data.connector.LocalAndroidDeviceConnector
import com.devicexpert.app.data.connector.MqttComputerDeviceConnector
import com.devicexpert.app.data.connector.MqttIotDeviceConnector
import com.devicexpert.app.domain.connector.ConnectorRegistry
import com.devicexpert.app.domain.connector.DeviceConnector
import com.devicexpert.app.domain.model.device.DeviceCategory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConnectorModule {

    @Provides
    @Singleton
    fun provideConnectorRegistry(
        androidConnector: LocalAndroidDeviceConnector,
        computerConnector: MqttComputerDeviceConnector,
        iotConnector: MqttIotDeviceConnector
    ): ConnectorRegistry = object : ConnectorRegistry {
        override fun getConnector(category: DeviceCategory): DeviceConnector = when (category) {
            DeviceCategory.ANDROID -> androidConnector
            DeviceCategory.COMPUTER -> computerConnector
            DeviceCategory.IOT -> iotConnector
        }
    }
}
