package com.devicexpert.app.data.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun getAllSensors(): List<HardwareSensorInfo> {
        return sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            HardwareSensorInfo(
                name = sensor.name,
                vendor = sensor.vendor,
                type = sensor.type,
                stringType = sensor.stringType ?: "unknown",
                powerMa = sensor.power,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution
            )
        }
    }

    fun getSensorCount(): Int = sensorManager.getSensorList(Sensor.TYPE_ALL).size

    fun observeLiveSensors(): Flow<LiveSensorSnapshot> = callbackFlow {
        val accel = FloatArray(3)
        val magnet = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var hasAccel = false
        var hasMagnet = false
        var currentState = LiveSensorSnapshot()
        var lastEmitMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
                        currentState = currentState.copy(compassAzimuthDeg = deg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accel, 0, 3)
                        hasAccel = true
                        var azimuth = currentState.compassAzimuthDeg
                        if (hasMagnet && SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
                        }
                        currentState = currentState.copy(
                            accelerometer = Triple(event.values[0], event.values[1], event.values[2]),
                            compassAzimuthDeg = azimuth
                        )
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnet, 0, 3)
                        hasMagnet = true
                        currentState = currentState.copy(
                            magnetometer = Triple(event.values[0], event.values[1], event.values[2])
                        )
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        currentState = currentState.copy(
                            gyroscope = Triple(event.values[0], event.values[1], event.values[2])
                        )
                    }
                    Sensor.TYPE_LIGHT -> {
                        currentState = currentState.copy(lightLux = event.values[0])
                    }
                    Sensor.TYPE_PROXIMITY -> {
                        currentState = currentState.copy(proximityCm = event.values[0])
                    }
                    Sensor.TYPE_PRESSURE -> {
                        currentState = currentState.copy(pressureHpa = event.values[0])
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastEmitMs >= 100L) {
                    lastEmitMs = now
                    trySend(currentState)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val targetTypes = listOf(
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_PRESSURE
        )

        targetTypes.forEach { type ->
            sensorManager.getDefaultSensor(type)?.let { sensor ->
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        trySend(currentState)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    data class HardwareSensorInfo(
        val name: String,
        val vendor: String,
        val type: Int,
        val stringType: String,
        val powerMa: Float,
        val maxRange: Float,
        val resolution: Float
    )

    data class LiveSensorSnapshot(
        val compassAzimuthDeg: Float? = null,
        val accelerometer: Triple<Float, Float, Float>? = null,
        val gyroscope: Triple<Float, Float, Float>? = null,
        val magnetometer: Triple<Float, Float, Float>? = null,
        val lightLux: Float? = null,
        val proximityCm: Float? = null,
        val pressureHpa: Float? = null
    )
}
