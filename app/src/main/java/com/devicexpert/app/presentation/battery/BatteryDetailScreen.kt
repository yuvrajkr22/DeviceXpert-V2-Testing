package com.devicexpert.app.presentation.battery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.devicexpert.app.domain.model.battery.BatterySession
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sessionDateFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: BatteryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useFahrenheit = LocalUseFahrenheit.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Monitor", fontWeight = FontWeight.SemiBold) },
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
                CircularProgressIndicator(color = EmeraldVolt)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error", color = StatusCriticalCrimson)
            }
            telemetry == null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Battery Telemetry Unavailable", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Battery details are available for Android host devices.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item { BatteryHeroCard(telemetry) }
                    item { ElectricalMetricsGrid(telemetry, useFahrenheit) }
                    if (uiState.livePowerHistoryWatts.size >= 2) {
                        item { LivePowerCard(uiState.livePowerHistoryWatts, telemetry.isCharging) }
                    }
                    item { HealthAndCapacityCard(telemetry) }
                    item {
                        BatteryHistoryChartsCard(
                            hasCharts = uiState.hasHistoryCharts,
                            levelProducer = viewModel.batteryLevelChartProducer,
                            tempProducer = viewModel.batteryTempChartProducer
                        )
                    }
                    item {
                        Text("Charge & Discharge History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                    }
                    if (uiState.recentSessions.isEmpty()) {
                        item {
                            TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Recording sessions locally via Room. History populates as your device charges and discharges.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    } else {
                        items(uiState.recentSessions, key = { it.sessionId }) { session ->
                            BatterySessionRow(session, useFahrenheit)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BatteryHeroCard(telemetry: AndroidTelemetry) {
    val pctInt = (telemetry.batteryPct * 100f).toInt()
    val statusText = if (telemetry.isCharging) "Charging via ${telemetry.chargingSource}" else "Discharging"
    val etaLabel = when {
        !telemetry.isCharging -> "Unplugged"
        telemetry.batteryChargeTimeRemainingMs != null -> "Full in ~${TelemetryFormatters.formatDurationMillis(telemetry.batteryChargeTimeRemainingMs)}"
        else -> "Estimating charge time..."
    }
    TelemetryCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(EmeraldBadgeBg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = EmeraldVolt)
                }
                Spacer(Modifier.height(10.dp))
                Text("$pctInt%", style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp), color = EmeraldVolt)
                Spacer(Modifier.height(4.dp))
                Text(etaLabel, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            GradientRingGauge(progress = telemetry.batteryPct, modifier = Modifier.size(92.dp), gradientColors = listOf(EmeraldVolt, CyanPulse, EmeraldVolt), strokeWidth = 8.dp, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ElectricalMetricsGrid(telemetry: AndroidTelemetry, useFahrenheit: Boolean) {
    val voltageText = if (telemetry.batteryVoltageMv > 0) String.format(Locale.US, "%.3f V (%d mV)", telemetry.batteryVoltageMv / 1000f, telemetry.batteryVoltageMv) else "Unavailable"
    val currentText = telemetry.batteryCurrentMa?.let { ma -> "${if (ma > 0f) "+" else ""}${String.format(Locale.US, "%.0f", ma)} mA" } ?: "Unsupported"
    val powerText = telemetry.batteryPowerWatts?.let { String.format(Locale.US, "%.2f W", it) } ?: "Unsupported"
    val tempText = TelemetryFormatters.formatTemperature(telemetry.batteryTempC, useFahrenheit)
    val tempAccent = when {
        telemetry.batteryTempC >= 43f -> StatusCriticalCrimson
        telemetry.batteryTempC >= 38f -> StatusWarningAmber
        else -> EmeraldVolt
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ElectricalTile("Voltage", voltageText, "Direct", CyanPulse, Modifier.weight(1f))
            ElectricalTile("Current", currentText, if (telemetry.batteryCurrentMa != null) "Direct" else "OEM", NeonViolet, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ElectricalTile("Power", powerText, if (telemetry.batteryPowerWatts != null) "Calculated" else "Unavailable", EmeraldVolt, Modifier.weight(1f))
            ElectricalTile("Temperature", tempText, "Direct", tempAccent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ElectricalTile(title: String, value: String, tag: String, accent: Color, modifier: Modifier = Modifier) {
    TelemetryCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            ProvenancePill(tag, accent)
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun LivePowerCard(samplesWatts: List<Float>, isCharging: Boolean) {
    val lineColor = if (isCharging) EmeraldVolt else StatusWarningAmber
    TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = lineColor, modifier = Modifier.size(18.dp))
                Text(if (isCharging) "Live Charging Power (W)" else "Live Discharge Power (W)", style = MaterialTheme.typography.titleSmall)
            }
            Text(String.format(Locale.US, "%.2f W", samplesWatts.lastOrNull() ?: 0f), style = MaterialTheme.typography.labelLarge, color = lineColor)
        }
        Spacer(Modifier.height(8.dp))
        LiveAreaSparkline(
            series = listOf(SparklineSeries(samplesWatts, listOf(lineColor), lineColor.copy(alpha = 0.28f))),
            height = 68.dp,
            fixedMin = 0f,
            fixedMax = null
        )
    }
}

@Composable
private fun HealthAndCapacityCard(telemetry: AndroidTelemetry) {
    TelemetryCard(modifier = Modifier.fillMaxWidth()) {
        Text("Health, Capacity & Provenance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        SpecProvenanceRow("Health", telemetry.batteryHealth, "Direct")
        SpecProvenanceRow("Technology", telemetry.batteryTechnology, "Direct")
        SpecProvenanceRow("Power Source", telemetry.chargingSource, "Direct")
        SpecProvenanceRow("Remaining Charge", telemetry.batteryChargeCounterMah?.let { "${it.toInt()} mAh" } ?: "Not exposed by fuel gauge", if (telemetry.batteryChargeCounterMah != null) "Direct" else "OEM")
        SpecProvenanceRow("Est. Full Capacity", telemetry.batteryEstimatedFullMah?.let { "~${it.toInt()} mAh" } ?: "Unavailable", if (telemetry.batteryEstimatedFullMah != null) "Estimated" else "Unavailable")
        SpecProvenanceRow("Cycle Count", telemetry.batteryCycleCount?.toString() ?: "Not exposed by OS/OEM", if (telemetry.batteryCycleCount != null) "Direct" else "API 34+ / OEM")
    }
}

@Composable
private fun BatteryHistoryChartsCard(
    hasCharts: Boolean,
    levelProducer: CartesianChartModelProducer,
    tempProducer: CartesianChartModelProducer
) {
    TelemetryCard(modifier = Modifier.fillMaxWidth()) {
        Text("24-Hour Telemetry Trends", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        if (!hasCharts) {
            Text("Collecting background snapshots every 15 minutes via WorkManager. Trends appear once at least 2 snapshots are stored.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else {
            Text("Battery Level (%)", style = MaterialTheme.typography.labelMedium, color = EmeraldVolt)
            Spacer(Modifier.height(8.dp))
            HistoricalLineChart(modelProducer = levelProducer, lineColor = EmeraldVolt, height = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Battery Temperature (°C)", style = MaterialTheme.typography.labelMedium, color = StatusWarningAmber)
            Spacer(Modifier.height(8.dp))
            HistoricalLineChart(modelProducer = tempProducer, lineColor = StatusWarningAmber, height = 160.dp)
        }
    }
}

@Composable
private fun BatterySessionRow(session: BatterySession, useFahrenheit: Boolean) {
    val accent = if (session.isCharging) EmeraldVolt else CyanPulse
    val startLabel = Instant.ofEpochMilli(session.startTimeMillis).atZone(ZoneId.systemDefault()).format(sessionDateFormatter)
    val deltaPrefix = if (session.deltaPct > 0) "+" else ""
    TelemetryCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (session.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                Column {
                    Text(if (session.isCharging) "Charging Session" else "Discharging Session", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("$startLabel • ${TelemetryFormatters.formatDurationMillis(session.durationMillis)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.startPct}% → ${session.endPct}% ($deltaPrefix${session.deltaPct}%)", style = MaterialTheme.typography.labelLarge, color = accent)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Text("Peak ${TelemetryFormatters.formatTemperature(session.maxTempC, useFahrenheit)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SpecProvenanceRow(label: String, value: String, tag: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
        ProvenancePill(tag, CyanPulse)
    }
}

@Composable
private fun ProvenancePill(text: String, accent: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.14f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = accent)
    }
}
