package com.devicexpert.app.presentation.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.devicexpert.app.core.designsystem.components.ConcentricStorageRings
import com.devicexpert.app.core.designsystem.components.TelemetryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: StorageDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Monitor", fontWeight = FontWeight.SemiBold) },
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
                CircularProgressIndicator(color = StorageAmber)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Error", color = StatusCriticalCrimson)
            }
            else -> {
                val usedPct = (uiState.usedRatio * 100f).toInt()
                val (statusLabel, statusColor) = when {
                    uiState.usedRatio >= 0.92f -> "Critically Low Space" to StatusCriticalCrimson
                    uiState.usedRatio >= 0.80f -> "Storage Getting Full" to StatusWarningAmber
                    else -> "Healthy Headroom" to EmeraldVolt
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
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(TelemetryFormatters.formatBytes(uiState.usedBytes), style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp), color = StorageAmber)
                                    Text("$usedPct% used of ${TelemetryFormatters.formatBytes(uiState.totalBytes)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                ConcentricStorageRings(outerProgress = uiState.usedRatio, middleProgress = 0f, innerProgress = uiState.freeRatio, modifier = Modifier.size(96.dp))
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Internal Partition Breakdown", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            StorageBarRow("Used Space", TelemetryFormatters.formatBytes(uiState.usedBytes), uiState.usedRatio, StorageAmber)
                            Spacer(Modifier.height(12.dp))
                            StorageBarRow("Available Space", TelemetryFormatters.formatBytes(uiState.availableBytes), uiState.freeRatio, StorageGreen)
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Usable Partition", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(TelemetryFormatters.formatBytes(uiState.totalBytes), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Storage Scope & Privacy Note", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                            Spacer(Modifier.height(4.dp))
                            Text("Usable partition totals are queried via Android StatFs without accessing personal files. Detailed per-app storage breakdown requires the special Usage Access system permission, and raw flash wear (SMART) counters are not exposed by standard Android kernels.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StorageBarRow(label: String, value: String, ratio: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(InactiveTrack)) {
            Box(modifier = Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}
