package com.devicexpert.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.devicexpert.app.data.local.entity.BatterySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySessionDao {
    @Query("SELECT * FROM battery_sessions WHERE device_id = :deviceId ORDER BY start_time DESC LIMIT :limit")
    fun observeRecentSessions(deviceId: String, limit: Int): Flow<List<BatterySessionEntity>>

    @Query("SELECT * FROM battery_sessions WHERE device_id = :deviceId ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestSession(deviceId: String): BatterySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BatterySessionEntity): Long

    @Update
    suspend fun updateSession(session: BatterySessionEntity)

    @Query("DELETE FROM battery_sessions WHERE end_time < :beforeMillis")
    suspend fun pruneOldSessions(beforeMillis: Long): Int
}
