package com.devicexpert.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devicexpert.app.data.local.dao.AlertDao
import com.devicexpert.app.data.local.dao.BatterySessionDao
import com.devicexpert.app.data.local.dao.DeviceDao
import com.devicexpert.app.data.local.dao.TelemetryDao
import com.devicexpert.app.data.local.db.DeviceXpertDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `battery_sessions` (
                    `session_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `device_id` TEXT NOT NULL,
                    `is_charging` INTEGER NOT NULL,
                    `start_time` INTEGER NOT NULL,
                    `end_time` INTEGER NOT NULL,
                    `start_pct` INTEGER NOT NULL,
                    `end_pct` INTEGER NOT NULL,
                    `max_temp_c` REAL NOT NULL,
                    `avg_current_ma` REAL,
                    FOREIGN KEY(`device_id`) REFERENCES `devices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_battery_sessions_device_id_start_time`
                ON `battery_sessions` (`device_id`, `start_time`)
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDeviceXpertDatabase(@ApplicationContext context: Context): DeviceXpertDatabase {
        return Room.databaseBuilder(
            context,
            DeviceXpertDatabase::class.java,
            "devicexpert_local_db"
        )
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides @Singleton fun provideDeviceDao(db: DeviceXpertDatabase): DeviceDao = db.deviceDao
    @Provides @Singleton fun provideTelemetryDao(db: DeviceXpertDatabase): TelemetryDao = db.telemetryDao
    @Provides @Singleton fun provideAlertDao(db: DeviceXpertDatabase): AlertDao = db.alertDao
    @Provides @Singleton fun provideBatterySessionDao(db: DeviceXpertDatabase): BatterySessionDao = db.batterySessionDao
}
