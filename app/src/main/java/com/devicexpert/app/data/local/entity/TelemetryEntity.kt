package com.devicexpert.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telemetry_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["device_id", "timestamp"])
    ]
)
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "snapshot_id") val snapshotId: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: String,
    val timestamp: Long,
    @ColumnInfo(name = "health_score") val healthScore: Int,
    @ColumnInfo(name = "payload_json") val payloadJson: String
)
