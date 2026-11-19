package com.devicexpert.app.presentation.thermal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.common.TelemetryFormatters
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.GradientRingGauge
import com.devicexpert.app.core.designsystem.components.HistoricalLineChart
import com.devicexpert.app.core.designsystem.components.LiveAreaSparkline
import com.devicexpert.app.core.designsystem.components.SparklineSeries
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.presentation.cpu.thermalStatusInfo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThermalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useFahrenheit = LocalUseFahrenheit.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thermal Monitor", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val telemetry = uiState.telemetry
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StatusWarningAmber)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error", color = StatusCriticalCrimson)
            }
            telemetry == null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Thermal metrics available for Android devices.", color = TextSecondary)
                }
            }
            else -> {
                val (statusLabel, statusColor) = thermalStatusInfo(telemetry.thermalStatus)
                val headroomPct = telemetry.thermalHeadroom?.coerceIn(0f, 1.5f)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text("Throttle State: $statusLabel", style = MaterialTheme.typography.labelSmall, color = statusColor)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(TelemetryFormatters.formatTemperature(telemetry.batteryTempC, useFahrenheit), style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp), color = statusColor)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Live Battery Thermal Sensor", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = statusColor, modifier = Modifier.size(56.dp))
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Thermal Headroom Forecast", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (headroomPct != null) "10-second Android PowerManager forecast toward severe throttling (100% = severe throttle)."
                                        else "Requires Android 11+ (API 30) and OEM thermal HAL support.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                GradientRingGauge(
                                    progress = headroomPct?.coerceIn(0f, 1f),
                                    modifier = Modifier.size(72.dp),
                                    gradientColors = listOf(EmeraldVolt, StatusWarningAmber, StatusCriticalCrimson),
                                    strokeWidth = 6.dp,
                                    labelText = headroomPct?.let { String.format(Locale.US, "%.0f%%", it * 100f) } ?: "N/A",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    if (uiState.liveTempHistory.size >= 2) {
                        item {
                            TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                                Text("Live Temperature Stream (60s)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp))
                                Spacer(Modifier.height(8.dp))
                                LiveAreaSparkline(
                                    series = listOf(SparklineSeries(uiState.liveTempHistory, listOf(StatusWarningAmber, StatusCriticalCrimson), StatusWarningAmber.copy(alpha = 0.25f))),
                                    height = 72.dp,
                                    fixedMin = 15f,
                                    fixedMax = 55f
                                )
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("24-Hour Thermal Profile (°C)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            if (!uiState.hasHistoryChart) {
                                Text("Historical thermal trend will render once WorkManager stores 2+ snapshots.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            } else {
                                HistoricalLineChart(modelProducer = viewModel.thermalHistoryProducer, lineColor = StatusWarningAmber, height = 170.dp)
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("SELinux Thermal Zone Note", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                            Spacer(Modifier.height(4.dp))
                            Text("Modern Android isolates raw /sys/class/thermal/ die sensors from non-root apps. DeviceXpert relies on official BatteryManager temperature and PowerManager thermal APIs for verified accuracy.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}
