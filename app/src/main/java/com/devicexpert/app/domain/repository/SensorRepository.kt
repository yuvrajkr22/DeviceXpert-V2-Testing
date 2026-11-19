package com.devicexpert.app.domain.repository

import com.devicexpert.app.domain.model.sensor.HardwareSensor
import com.devicexpert.app.domain.model.sensor.LiveSensorReadings
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun getAllHardwareSensors(): List<HardwareSensor>
    fun observeLiveSensors(): Flow<LiveSensorReadings>
}
