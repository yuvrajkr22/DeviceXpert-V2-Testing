package com.devicexpert.app.domain.model.common

sealed interface MetricValue<out T> {
    data class Direct<T>(val value: T) : MetricValue<T>
    data class Calculated<T>(val value: T, val basis: String) : MetricValue<T>
    data class Unavailable(val reason: String) : MetricValue<Nothing>

    fun getOrNull(): T? = when (this) {
        is Direct -> value
        is Calculated -> value
        is Unavailable -> null
    }
}
