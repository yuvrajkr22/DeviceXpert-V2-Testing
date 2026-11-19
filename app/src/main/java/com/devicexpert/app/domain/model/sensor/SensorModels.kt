package com.devicexpert.app.domain.model.sensor

data class HardwareSensor(
    val name: String,
    val vendor: String,
    val type: Int,
    val stringType: String,
    val powerMa: Float,
    val maxRange: Float,
    val resolution: Float
)

data class LiveSensorReadings(
    val compassAzimuthDeg: Float? = null,
    val accelerometer: Triple<Float, Float, Float>? = null,
    val gyroscope: Triple<Float, Float, Float>? = null,
    val magnetometer: Triple<Float, Float, Float>? = null,
    val lightLux: Float? = null,
    val proximityCm: Float? = null,
    val pressureHpa: Float? = null
)
