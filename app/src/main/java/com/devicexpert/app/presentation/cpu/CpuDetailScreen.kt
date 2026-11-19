package com.devicexpert.app.presentation.cpu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeveloperBoard
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
import com.devicexpert.app.core.designsystem.components.CpuAreaSparkline
import com.devicexpert.app.core.designsystem.components.GradientRingGauge
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.device.AndroidMetadata
import com.devicexpert.app.domain.model.device.ComputerMetadata
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToThermal: (String) -> Unit,
    viewModel: CpuDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useFahrenheit = LocalUseFahrenheit.current
    val deviceId = uiState.device?.id ?: "local_android_host"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CPU & Frequency Monitor", fontWeight = FontWeight.SemiBold) },
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
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CpuBlue)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error", color = StatusCriticalCrimson)
            }
            else -> {
                val androidMeta = uiState.device?.metadata as? AndroidMetadata
                val pcMeta = uiState.device?.metadata as? ComputerMetadata
                val androidPayload = uiState.payload as? AndroidTelemetry
                val pcPayload = uiState.payload as? ComputerTelemetry

                val coreCount = androidMeta?.cpuCoreCount?.takeIf { it > 0 } ?: androidPayload?.cpuCoreFreqsMhz?.size ?: pcMeta?.coreCount ?: 0
                val socName = androidMeta?.socModel ?: pcMeta?.cpuModel ?: "Processor"
                val abiSummary = androidMeta?.supportedAbis?.joinToString(", ")?.ifBlank { "Unknown" } ?: pcMeta?.osName ?: "Unknown"
                val coreIndices = (0 until (androidPayload?.cpuCoreFreqsMhz?.size ?: 0)).toList()
                val anyFreqReadable = androidPayload?.cpuCoreFreqsMhz?.any { it > 0 } == true

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        CpuHeroCard(
                            socName = socName,
                            abiSummary = abiSummary,
                            coreCount = coreCount,
                            loadProgress = androidPayload?.cpuFrequencyLoadPct ?: pcPayload?.cpuUsagePct,
                            isPcTrueUsage = pcPayload != null,
                            peakMhz = androidPayload?.cpuCoreFreqsMhz?.maxOrNull() ?: 0,
                            history = uiState.liveLoadHistory
                        )
                    }
                    if (androidPayload != null) {
                        item {
                            ThermalShortcutCard(
                                thermalStatus = androidPayload.thermalStatus,
                                batteryTempC = androidPayload.batteryTempC,
                                useFahrenheit = useFahrenheit,
                                onClick = { onNavigateToThermal(deviceId) }
                            )
                        }
                        item {
                            Text("Per-Core Live Clocks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                        }
                        if (!anyFreqReadable) {
                            item {
                                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("OEM Restricted (SELinux)", style = MaterialTheme.typography.titleSmall, color = StatusWarningAmber)
                                    Spacer(Modifier.height(6.dp))
                                    Text("This device restricts third-party access to /sys/devices/system/cpu/cpufreq/. Core count ($coreCount) and thermal status remain actively monitored.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        } else {
                            items(coreIndices.chunked(2)) { pair ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { coreIdx ->
                                        CoreClockCard(
                                            coreIndex = coreIdx,
                                            curMhz = androidPayload.cpuCoreFreqsMhz.getOrNull(coreIdx) ?: 0,
                                            minMhz = androidPayload.cpuCoreMinFreqsMhz.getOrNull(coreIdx) ?: 0,
                                            maxMhz = androidPayload.cpuCoreMaxFreqsMhz.getOrNull(coreIdx) ?: 0,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        item {
                            TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Android OS Transparency", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                                Spacer(Modifier.height(4.dp))
                                Text("Android 8+ blocks unprivileged apps from reading /proc/stat. The load index above reflects real-time hardware frequency scaling across active cores rather than fabricated CPU percentages.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CpuHeroCard(socName: String, abiSummary: String, coreCount: Int, loadProgress: Float?, isPcTrueUsage: Boolean, peakMhz: Int, history: List<Float>) {
    TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.DeveloperBoard, contentDescription = null, tint = CpuBlue, modifier = Modifier.size(20.dp))
                Text(socName, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text("$coreCount Cores • $abiSummary", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GradientRingGauge(progress = loadProgress, modifier = Modifier.size(84.dp), strokeWidth = 7.dp, fontSize = 17.sp)
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isPcTrueUsage) "CPU Utilization" else "Frequency Scaling Index", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(if (peakMhz > 0) TelemetryFormatters.formatFrequencyMhz(peakMhz) else "Peak Clock N/A", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        CpuAreaSparkline(values = history, height = 76.dp)
    }
}

@Composable
private fun ThermalShortcutCard(thermalStatus: Int, batteryTempC: Float, useFahrenheit: Boolean, onClick: () -> Unit) {
    val (label, color) = thermalStatusInfo(thermalStatus)
    TelemetryCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Column {
                    Text("Thermal Status: $label", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = color)
                    Text("Battery Temp ${TelemetryFormatters.formatTemperature(batteryTempC, useFahrenheit)} • Tap for Thermal Monitor", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun CoreClockCard(coreIndex: Int, curMhz: Int, minMhz: Int, maxMhz: Int, modifier: Modifier = Modifier) {
    val ratio = if (maxMhz > 0 && curMhz > 0) (curMhz.toFloat() / maxMhz.toFloat()).coerceIn(0f, 1f) else 0f
    TelemetryCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Core $coreIndex", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(if (curMhz > 0) "$curMhz MHz" else "Offline", style = MaterialTheme.typography.labelLarge, color = if (curMhz > 0) MaterialTheme.colorScheme.onSurface else StatusOfflineSlate)
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(InactiveTrack)) {
            Box(modifier = Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (ratio > 0.85f) CpuMagenta else CpuBlue))
        }
        Spacer(Modifier.height(6.dp))
        Text(if (minMhz > 0 && maxMhz > 0) "$minMhz–$maxMhz MHz" else "Range N/A", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

internal fun thermalStatusInfo(status: Int): Pair<String, Color> = when (status) {
    0 -> "Nominal" to EmeraldVolt
    1 -> "Light" to CyanPulse
    2 -> "Moderate" to StatusWarningAmber
    3 -> "Severe" to StorageOrange
    4 -> "Critical" to StatusCriticalCrimson
    5 -> "Emergency" to StatusCriticalCrimson
    6 -> "Shutdown" to StatusCriticalCrimson
    else -> "Unknown" to StatusOfflineSlate
}
