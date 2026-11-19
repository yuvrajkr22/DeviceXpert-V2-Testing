package com.devicexpert.app.data.repository

import com.devicexpert.app.data.collector.SensorCollector
import com.devicexpert.app.domain.model.sensor.HardwareSensor
import com.devicexpert.app.domain.model.sensor.LiveSensorReadings
import com.devicexpert.app.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val sensorCollector: SensorCollector
) : SensorRepository {

    override fun getAllHardwareSensors(): List<HardwareSensor> =
        sensorCollector.getAllSensors().map { info ->
            HardwareSensor(info.name, info.vendor, info.type, info.stringType, info.powerMa, info.maxRange, info.resolution)
        }

    override fun observeLiveSensors(): Flow<LiveSensorReadings> =
        sensorCollector.observeLiveSensors().map { snap ->
            LiveSensorReadings(
                compassAzimuthDeg = snap.compassAzimuthDeg,
                accelerometer = snap.accelerometer,
                gyroscope = snap.gyroscope,
                magnetometer = snap.magnetometer,
                lightLux = snap.lightLux,
                proximityCm = snap.proximityCm,
                pressureHpa = snap.pressureHpa
            )
        }
}
