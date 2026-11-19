package com.devicexpert.app.presentation.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
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
import com.devicexpert.app.core.designsystem.components.LiveAreaSparkline
import com.devicexpert.app.core.designsystem.components.SparklineSeries
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: NetworkDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Monitor", fontWeight = FontWeight.SemiBold) },
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
                val isOffline = uiState.netType == "OFFLINE"
                val statusColor = if (isOffline) StatusOfflineSlate else EmeraldVolt
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(uiState.netType, style = MaterialTheme.typography.labelSmall, color = statusColor)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(uiState.ipAddress ?: "No Active IP", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Local Interface Address", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Icon(if (isOffline) Icons.Rounded.WifiOff else Icons.Rounded.Wifi, contentDescription = null, tint = statusColor, modifier = Modifier.size(42.dp))
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                                Text("Live Throughput (60s)", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ThroughputColumn(true, TelemetryFormatters.formatSpeedBytesPerSec(uiState.rxBytesPerSec), TelemetryFormatters.formatSpeedBytesPerSec(uiState.peakRxBytesPerSec), Modifier.weight(1f))
                                    ThroughputColumn(false, TelemetryFormatters.formatSpeedBytesPerSec(uiState.txBytesPerSec), TelemetryFormatters.formatSpeedBytesPerSec(uiState.peakTxBytesPerSec), Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            LiveAreaSparkline(
                                series = listOf(
                                    SparklineSeries(uiState.rxHistory, listOf(CyanPulse), CyanPulse.copy(alpha = 0.22f)),
                                    SparklineSeries(uiState.txHistory, listOf(NeonViolet), NeonViolet.copy(alpha = 0.16f))
                                ),
                                height = 84.dp, fixedMin = 0f, fixedMax = null
                            )
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Data Counters & Link Capacity", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            NetworkSpecRow("Session Download", TelemetryFormatters.formatBytes(uiState.sessionRxBytes))
                            NetworkSpecRow("Session Upload", TelemetryFormatters.formatBytes(uiState.sessionTxBytes))
                            NetworkSpecRow("Since Boot Download", TelemetryFormatters.formatBytes(uiState.totalBootRxBytes))
                            NetworkSpecRow("Since Boot Upload", TelemetryFormatters.formatBytes(uiState.totalBootTxBytes))
                            NetworkSpecRow("Estimated Link Down", uiState.linkDownstreamKbps?.let { formatBandwidthKbps(it) } ?: "Unavailable")
                            NetworkSpecRow("Estimated Link Up", uiState.linkUpstreamKbps?.let { formatBandwidthKbps(it) } ?: "Unavailable")
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Privacy-First Network Monitoring", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                            Spacer(Modifier.height(4.dp))
                            Text("DeviceXpert intentionally omits Wi-Fi SSID names so it never requires GPS/Location permissions. Throughput is calculated locally from Android TrafficStats deltas.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ThroughputColumn(isDownload: Boolean, liveSpeed: String, peakSpeed: String, modifier: Modifier = Modifier) {
    val accent = if (isDownload) CyanPulse else NeonViolet
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(if (isDownload) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, contentDescription = null, tint = OledBlack, modifier = Modifier.size(13.dp))
            }
            Text(if (isDownload) "Download" else "Upload", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Text(liveSpeed, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = accent)
        Text("Peak: $peakSpeed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun NetworkSpecRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

private fun formatBandwidthKbps(kbps: Int): String = if (kbps >= 1000) String.format(Locale.US, "%.1f Mbps", kbps / 1000f) else "$kbps Kbps"
