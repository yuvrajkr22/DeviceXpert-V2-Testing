package com.devicexpert.app.presentation.deviceinfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.common.TelemetryFormatters
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.DeviceWireframeVisual
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.device.AndroidMetadata
import com.devicexpert.app.domain.model.device.ComputerMetadata
import com.devicexpert.app.domain.model.device.IotMetadata
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeviceInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Information", fontWeight = FontWeight.SemiBold) },
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
        val device = uiState.device
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonViolet)
            }
            uiState.error != null || device == null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Device unavailable", color = StatusCriticalCrimson)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    when (val meta = device.metadata) {
                        is AndroidMetadata -> {
                            val diag = if (meta.displayDiagonalInches > 0f) String.format(Locale.US, "%.1f\"", meta.displayDiagonalInches) else "Android"
                            val res = if (meta.displayWidthPx > 0 && meta.displayHeightPx > 0) "${maxOf(meta.displayHeightPx, meta.displayWidthPx)}x${minOf(meta.displayHeightPx, meta.displayWidthPx)}" else "API ${meta.sdkInt}"
                            item {
                                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        DeviceWireframeVisual(device.category, diag, res, Modifier.width(120.dp).height(190.dp))
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(device.name, style = MaterialTheme.typography.titleMedium, color = NeonViolet)
                                            CompactSpec("Display", "$res px")
                                            CompactSpec("Density", "${meta.displayDensityDpi} dpi")
                                            CompactSpec("Refresh Rate", String.format(Locale.US, "%.0f Hz", meta.displayRefreshRateHz))
                                            CompactSpec("OS", "Android ${meta.androidVersion} (API ${meta.sdkInt})")
                                        }
                                    }
                                }
                            }
                            item {
                                SpecSectionCard("Hardware & Chipset") {
                                    SpecRow("Manufacturer", meta.manufacturer)
                                    SpecRow("Model", meta.model)
                                    SpecRow("Brand", meta.brand.ifBlank { "Unknown" })
                                    SpecRow("SoC / Platform", meta.socModel)
                                    SpecRow("Board / Hardware", "${meta.board} / ${meta.hardware}")
                                    SpecRow("CPU Cores", meta.cpuCoreCount.toString())
                                    SpecRow("ABIs", meta.supportedAbis.joinToString(", ").ifBlank { "Unknown" })
                                    SpecRow("Total RAM", TelemetryFormatters.formatBytes(meta.totalRamBytes))
                                    SpecRow("Usable Storage", TelemetryFormatters.formatBytes(meta.totalStorageBytes))
                                    SpecRow("Sensors Installed", meta.sensorCount.toString())
                                }
                            }
                            item {
                                SpecSectionCard("System & Runtime") {
                                    SpecRow("Android Version", meta.androidVersion)
                                    SpecRow("SDK Level", "API ${meta.sdkInt}")
                                    SpecRow("Security Patch", meta.securityPatch.ifBlank { "Unknown" })
                                    SpecRow("Kernel Version", meta.kernelVersion)
                                    SpecRow("System Uptime", TelemetryFormatters.formatDurationMillis(uiState.uptimeMillis))
                                    SpecRow("Awake Time", TelemetryFormatters.formatDurationMillis(uiState.awakeUptimeMillis))
                                    SpecRow("Deep Sleep", "${TelemetryFormatters.formatDurationMillis(uiState.deepSleepMillis)} (${(uiState.deepSleepPct * 100).toInt()}%)")
                                }
                            }
                        }
                        is ComputerMetadata -> {
                            item {
                                SpecSectionCard("Computer Specifications") {
                                    SpecRow("Hostname", meta.hostname)
                                    SpecRow("Operating System", "${meta.osName} ${meta.osVersion}")
                                    SpecRow("CPU Model", meta.cpuModel)
                                    SpecRow("Core Count", meta.coreCount.toString())
                                    SpecRow("Total RAM", TelemetryFormatters.formatBytes(meta.totalRamBytes))
                                    SpecRow("Total Disk", TelemetryFormatters.formatBytes(meta.totalDiskBytes))
                                    SpecRow("Agent Version", meta.agentVersion)
                                    SpecRow("Uptime", TelemetryFormatters.formatDurationMillis(uiState.uptimeMillis))
                                }
                            }
                        }
                        is IotMetadata -> {
                            item {
                                SpecSectionCard("IoT Node Specifications") {
                                    SpecRow("Board Model", meta.boardModel)
                                    SpecRow("Firmware", meta.firmwareVersion)
                                    SpecRow("MAC Address", meta.macAddress)
                                    SpecRow("IP Address", meta.ipAddress)
                                    SpecRow("MQTT Prefix", meta.mqttTopicPrefix)
                                    SpecRow("Capabilities", meta.capabilities.joinToString(", ").ifBlank { "Standard" })
                                    SpecRow("Uptime", TelemetryFormatters.formatDurationMillis(uiState.uptimeMillis))
                                }
                            }
                        }
                    }
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Hardware Identifiers Policy", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                            Spacer(Modifier.height(4.dp))
                            Text("DeviceXpert never accesses IMEI, hardware serial numbers, or personal identifiers. Only non-sensitive hardware and OS specifications are displayed.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable private fun CompactSpec(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable private fun SpecSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    TelemetryCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable private fun SpecRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.55f))
    }
}
