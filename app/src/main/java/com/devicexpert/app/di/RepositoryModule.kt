package com.devicexpert.app.di

import com.devicexpert.app.data.repository.AlertRepositoryImpl
import com.devicexpert.app.data.repository.AuthRepositoryImpl
import com.devicexpert.app.data.repository.BatterySessionRepositoryImpl
import com.devicexpert.app.data.repository.DeviceRepositoryImpl
import com.devicexpert.app.data.repository.SensorRepositoryImpl
import com.devicexpert.app.data.repository.SettingsRepositoryImpl
import com.devicexpert.app.data.repository.TelemetryRepositoryImpl
import com.devicexpert.app.domain.repository.AlertRepository
import com.devicexpert.app.domain.repository.AuthRepository
import com.devicexpert.app.domain.repository.BatterySessionRepository
import com.devicexpert.app.domain.repository.DeviceRepository
import com.devicexpert.app.domain.repository.SensorRepository
import com.devicexpert.app.domain.repository.SettingsRepository
import com.devicexpert.app.domain.repository.TelemetryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository
    @Binds @Singleton abstract fun bindTelemetryRepository(impl: TelemetryRepositoryImpl): TelemetryRepository
    @Binds @Singleton abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository
    @Binds @Singleton abstract fun bindSensorRepository(impl: SensorRepositoryImpl): SensorRepository
    @Binds @Singleton abstract fun bindBatterySessionRepository(impl: BatterySessionRepositoryImpl): BatterySessionRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
