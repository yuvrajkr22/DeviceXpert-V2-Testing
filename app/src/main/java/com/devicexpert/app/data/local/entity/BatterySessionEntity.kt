package com.devicexpert.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_sessions",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["device_id", "start_time"])
    ]
)
data class BatterySessionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "session_id") val sessionId: Long = 0L,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "is_charging") val isCharging: Boolean,
    @ColumnInfo(name = "start_time") val startTimeMillis: Long,
    @ColumnInfo(name = "end_time") val endTimeMillis: Long,
    @ColumnInfo(name = "start_pct") val startPct: Int,
    @ColumnInfo(name = "end_pct") val endPct: Int,
    @ColumnInfo(name = "max_temp_c") val maxTempC: Float,
    @ColumnInfo(name = "avg_current_ma") val avgCurrentMa: Float?
)
