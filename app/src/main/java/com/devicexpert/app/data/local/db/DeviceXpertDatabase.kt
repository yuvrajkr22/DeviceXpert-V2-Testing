package com.devicexpert.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devicexpert.app.data.local.dao.AlertDao
import com.devicexpert.app.data.local.dao.BatterySessionDao
import com.devicexpert.app.data.local.dao.DeviceDao
import com.devicexpert.app.data.local.dao.TelemetryDao
import com.devicexpert.app.data.local.entity.AlertEventEntity
import com.devicexpert.app.data.local.entity.AlertRuleEntity
import com.devicexpert.app.data.local.entity.BatterySessionEntity
import com.devicexpert.app.data.local.entity.DeviceEntity
import com.devicexpert.app.data.local.entity.TelemetryEntity

@Database(
    entities = [
        DeviceEntity::class,
        TelemetryEntity::class,
        AlertRuleEntity::class,
        AlertEventEntity::class,
        BatterySessionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DeviceXpertDatabase : RoomDatabase() {
    abstract val deviceDao: DeviceDao
    abstract val telemetryDao: TelemetryDao
    abstract val alertDao: AlertDao
    abstract val batterySessionDao: BatterySessionDao
}
