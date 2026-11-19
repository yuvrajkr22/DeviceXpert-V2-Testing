package com.devicexpert.app.presentation.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.data.initializer.HostDeviceInitializer
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.repository.TelemetryRepository
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val telemetryRepository: TelemetryRepository
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: HostDeviceInitializer.LOCAL_HOST_DEVICE_ID
    val batteryChartProducer = CartesianChartModelProducer()
    val tempChartProducer = CartesianChartModelProducer()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasData = MutableStateFlow(false)
    val hasData: StateFlow<Boolean> = _hasData.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val since = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
            telemetryRepository.observeTelemetryHistory(deviceId, since)
                .catch { _isLoading.update { false } }
                .collect { snapshots ->
                    val batteryPoints = mutableListOf<Float>()
                    val tempPoints = mutableListOf<Float>()
                    snapshots.forEach { snap ->
                        val pl = snap.payload as? AndroidTelemetry ?: return@forEach
                        batteryPoints.add(pl.batteryPct * 100f)
                        tempPoints.add(pl.batteryTempC)
                    }
                    if (batteryPoints.size >= 2) {
                        batteryChartProducer.runTransaction { lineSeries { series(batteryPoints) } }
                        tempChartProducer.runTransaction { lineSeries { series(tempPoints) } }
                        _hasData.update { true }
                    }
                    _isLoading.update { false }
                }
        }
    }
}
