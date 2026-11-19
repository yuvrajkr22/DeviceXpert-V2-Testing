package com.devicexpert.app.data.repository

import com.devicexpert.app.data.local.dao.AlertDao
import com.devicexpert.app.data.mapper.toDomain
import com.devicexpert.app.data.mapper.toEntity
import com.devicexpert.app.domain.model.alert.AlertEvent
import com.devicexpert.app.domain.model.alert.AlertRule
import com.devicexpert.app.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao
) : AlertRepository {

    override suspend fun getActiveRules(): List<AlertRule> = alertDao.getActiveRules().map { it.toDomain() }
    override fun observeAllRules(): Flow<List<AlertRule>> = alertDao.observeAllRules().map { l -> l.map { it.toDomain() } }
    override suspend fun saveRule(rule: AlertRule): Result<Unit> = runCatching { alertDao.insertRule(rule.toEntity()) }
    override suspend fun setRuleEnabled(ruleId: String, enabled: Boolean): Result<Unit> = runCatching { alertDao.setRuleEnabled(ruleId, enabled) }
    override suspend fun deleteRule(ruleId: String): Result<Unit> = runCatching { alertDao.deleteRule(ruleId) }
    override fun observeAllEvents(): Flow<List<AlertEvent>> = alertDao.observeAllEvents().map { l -> l.map { it.toDomain() } }
    override fun observeUnacknowledgedEvents(): Flow<List<AlertEvent>> = alertDao.observeUnacknowledgedEvents().map { l -> l.map { it.toDomain() } }
    override suspend fun recordEvent(event: AlertEvent): Result<Unit> = runCatching { alertDao.insertEvent(event.toEntity()) }
    override suspend fun acknowledgeEvent(alertId: String): Result<Unit> = runCatching { alertDao.acknowledgeEvent(alertId) }
}
