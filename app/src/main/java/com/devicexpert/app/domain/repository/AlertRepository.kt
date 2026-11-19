package com.devicexpert.app.domain.repository

import com.devicexpert.app.domain.model.alert.AlertEvent
import com.devicexpert.app.domain.model.alert.AlertRule
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    suspend fun getActiveRules(): List<AlertRule>
    fun observeAllRules(): Flow<List<AlertRule>>
    suspend fun saveRule(rule: AlertRule): Result<Unit>
    suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Result<Unit>
    suspend fun deleteRule(ruleId: String): Result<Unit>
    fun observeAllEvents(): Flow<List<AlertEvent>>
    fun observeUnacknowledgedEvents(): Flow<List<AlertEvent>>
    suspend fun recordEvent(event: AlertEvent): Result<Unit>
    suspend fun acknowledgeEvent(alertId: String): Result<Unit>
}
