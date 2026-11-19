package com.devicexpert.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devicexpert.app.data.local.entity.AlertEventEntity
import com.devicexpert.app.data.local.entity.AlertRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alert_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<AlertRuleEntity>

    @Query("SELECT * FROM alert_rules ORDER BY metricKey ASC")
    fun observeAllRules(): Flow<List<AlertRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlertRuleEntity)

    @Query("UPDATE alert_rules SET isEnabled = :enabled WHERE ruleId = :ruleId")
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean)

    @Query("DELETE FROM alert_rules WHERE ruleId = :ruleId")
    suspend fun deleteRule(ruleId: String)

    @Query("SELECT * FROM alert_events ORDER BY triggeredAtMillis DESC")
    fun observeAllEvents(): Flow<List<AlertEventEntity>>

    @Query("SELECT * FROM alert_events WHERE isAcknowledged = 0 ORDER BY triggeredAtMillis DESC")
    fun observeUnacknowledgedEvents(): Flow<List<AlertEventEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: AlertEventEntity)

    @Query("UPDATE alert_events SET isAcknowledged = 1 WHERE alertId = :alertId")
    suspend fun acknowledgeEvent(alertId: String)
}
