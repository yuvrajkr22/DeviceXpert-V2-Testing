package com.devicexpert.app.presentation.livemonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.common.TelemetryFormatters
import com.devicexpert.app.core.designsystem.AppTypography
import com.devicexpert.app.core.designsystem.LocalUseFahrenheit
import com.devicexpert.app.core.designsystem.components.MetricGauge
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.device.AndroidMetadata
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMonitorScreen(
    onNavigateBack: () -> Unit,
    viewModel: LiveMonitorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useFahrenheit = LocalUseFahrenheit.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Monitor") },
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
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                else -> {
                    val payload = uiState.latestSnapshot?.payload as? AndroidTelemetry
                    val androidMeta = uiState.device?.metadata as? AndroidMetadata
                    val totalRam = payload?.ramTotalBytes?.takeIf { it > 0L } ?: androidMeta?.totalRamBytes ?: 0L
                    val ramPct = if (payload != null && totalRam > 0L) (payload.ramUsedBytes.toFloat() / totalRam.toFloat()).coerceIn(0f, 1f) else 0f

                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(uiState.device?.name ?: "Unknown", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("RAM Load", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(16.dp))
                                    MetricGauge(ramPct, Modifier.fillMaxWidth().aspectRatio(1f))
                                }
                            }
                            item {
                                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                    Text("Battery", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(16.dp))
                                    MetricGauge(payload?.batteryPct ?: 0f, Modifier.fillMaxWidth().aspectRatio(1f))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = payload?.let { TelemetryFormatters.formatTemperature(it.batteryTempC, useFahrenheit) } ?: "--",
                                        style = AppTypography.labelLarge,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Live Hardware Telemetry", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            MetricLine("Network", "${payload?.netType ?: "N/A"} (${payload?.ipAddress ?: "-"})")
                            MetricLine("Power", payload?.batteryPowerWatts?.let { "${"%.2f".format(it)} W" } ?: "Unsupported")
                            MetricLine("Voltage", "${payload?.batteryVoltageMv ?: 0} mV")
                            MetricLine("Uptime", TelemetryFormatters.formatDurationMillis(payload?.uptimeMillis ?: 0L))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = AppTypography.labelLarge)
    }
}
