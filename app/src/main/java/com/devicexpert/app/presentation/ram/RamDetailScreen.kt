package com.devicexpert.app.presentation.ram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Memory
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
import com.devicexpert.app.core.designsystem.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RamDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RAM Monitor", fontWeight = FontWeight.SemiBold) },
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
                CircularProgressIndicator(color = CyanPulse)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error", color = StatusCriticalCrimson)
            }
            else -> {
                val (pressureLabel, pressureColor) = when (uiState.pressureLevel) {
                    MemoryPressureLevel.NORMAL -> "Normal Pressure" to EmeraldVolt
                    MemoryPressureLevel.ELEVATED -> "Elevated Pressure" to StatusWarningAmber
                    MemoryPressureLevel.CRITICAL -> "High / Low Memory" to StatusCriticalCrimson
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(pressureColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(pressureLabel, style = MaterialTheme.typography.labelSmall, color = pressureColor)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(TelemetryFormatters.formatBytes(uiState.usedRamBytes), style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp), color = CyanPulse)
                                    Text("Used of ${TelemetryFormatters.formatBytes(uiState.totalRamBytes)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                GradientRingGauge(progress = uiState.usageRatio, modifier = Modifier.size(92.dp), gradientColors = listOf(CyanPulse, NeonViolet, CyanPulse), strokeWidth = 8.dp, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            DotMatrixProgressBar(progress = uiState.usageRatio, dotCount = 20, activeColor = CyanPulse, dotSize = 7.dp)
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RamStatTile("Total RAM", TelemetryFormatters.formatBytes(uiState.totalRamBytes), Modifier.weight(1f))
                            RamStatTile("Available", TelemetryFormatters.formatBytes(uiState.availableRamBytes), Modifier.weight(1f))
                        }
                    }
                    if (uiState.liveUsageHistoryPct.size >= 2) {
                        item {
                            TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = CyanPulse, modifier = Modifier.size(18.dp))
                                        Text("Live RAM Utilization (60s)", style = MaterialTheme.typography.titleSmall)
                                    }
                                    Text("${(uiState.usageRatio * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = CyanPulse)
                                }
                                Spacer(Modifier.height(8.dp))
                                LiveAreaSparkline(
                                    series = listOf(SparklineSeries(uiState.liveUsageHistoryPct, listOf(CyanPulse, NeonViolet), CyanPulse.copy(alpha = 0.28f))),
                                    height = 72.dp,
                                    fixedMin = 0f,
                                    fixedMax = 100f
                                )
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("24-Hour Memory Trend (%)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            if (!uiState.hasHistoryChart) {
                                Text("Historical RAM trends appear after WorkManager captures at least 2 background snapshots.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            } else {
                                HistoricalLineChart(modelProducer = viewModel.ramHistoryProducer, lineColor = CyanPulse, height = 170.dp)
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Android Memory Sandboxing Note", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                            Spacer(Modifier.height(4.dp))
                            Text("System-wide RAM totals and low-memory pressure states are read directly from ActivityManager. Per-process RAM inspection of third-party apps is restricted by Android 10+ privacy controls.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RamStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    TelemetryCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}
