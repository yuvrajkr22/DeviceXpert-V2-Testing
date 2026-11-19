package com.devicexpert.app.data.mapper

import com.devicexpert.app.data.local.entity.AlertEventEntity
import com.devicexpert.app.data.local.entity.AlertRuleEntity
import com.devicexpert.app.domain.model.alert.AlertEvent
import com.devicexpert.app.domain.model.alert.AlertOperator
import com.devicexpert.app.domain.model.alert.AlertRule
import com.devicexpert.app.domain.model.alert.AlertSeverity

fun AlertRuleEntity.toDomain(): AlertRule = AlertRule(
    ruleId = ruleId,
    deviceId = deviceId,
    metricKey = metricKey,
    operator = AlertOperator.valueOf(operator),
    thresholdValue = thresholdValue,
    severity = AlertSeverity.valueOf(severity),
    isEnabled = isEnabled
)

fun AlertRule.toEntity(): AlertRuleEntity = AlertRuleEntity(
    ruleId = ruleId,
    deviceId = deviceId,
    metricKey = metricKey,
    operator = operator.name,
    thresholdValue = thresholdValue,
    severity = severity.name,
    isEnabled = isEnabled
)

fun AlertEventEntity.toDomain(): AlertEvent = AlertEvent(
    alertId = alertId,
    deviceId = deviceId,
    ruleId = ruleId,
    severity = AlertSeverity.valueOf(severity),
    title = title,
    message = message,
    metricValue = metricValue,
    isAcknowledged = isAcknowledged,
    triggeredAtMillis = triggeredAtMillis
)

fun AlertEvent.toEntity(): AlertEventEntity = AlertEventEntity(
    alertId = alertId,
    deviceId = deviceId,
    ruleId = ruleId,
    severity = severity.name,
    title = title,
    message = message,
    metricValue = metricValue,
    isAcknowledged = isAcknowledged,
    triggeredAtMillis = triggeredAtMillis
)
