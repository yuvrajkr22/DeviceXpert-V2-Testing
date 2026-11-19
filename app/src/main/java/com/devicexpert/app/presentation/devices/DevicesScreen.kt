package com.devicexpert.app.presentation.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.IotMetadata
import com.devicexpert.app.domain.model.device.UnifiedDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onNavigateBack: () -> Unit,
    onDeviceSelected: () -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddRemoteDeviceDialog(
            onDismiss = { showAddDialog = false },
            onAddComputer = { id, name, os, cpu, cores, ramGb ->
                viewModel.registerComputerDevice(id, name, os, cpu, cores, ramGb)
                showAddDialog = false
            },
            onAddIot = { id, name, board, prefix ->
                viewModel.registerIotDevice(id, name, board, prefix)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Fleet", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonViolet,
                contentColor = TextPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add PC / IoT") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.selectedFilter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") })
                FilterChip(selected = uiState.selectedFilter == DeviceCategory.ANDROID, onClick = { viewModel.setFilter(DeviceCategory.ANDROID) }, label = { Text("Android") })
                FilterChip(selected = uiState.selectedFilter == DeviceCategory.COMPUTER, onClick = { viewModel.setFilter(DeviceCategory.COMPUTER) }, label = { Text("PC") })
                FilterChip(selected = uiState.selectedFilter == DeviceCategory.IOT, onClick = { viewModel.setFilter(DeviceCategory.IOT) }, label = { Text("IoT") })
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(uiState.filteredDevices, key = { it.id }) { device ->
                    FleetDeviceCard(
                        device = device,
                        isActive = device.id == uiState.activeDeviceId,
                        onSelect = { viewModel.selectActiveDevice(device.id); onDeviceSelected() },
                        onDelete = { viewModel.deleteDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FleetDeviceCard(device: UnifiedDevice, isActive: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val icon = when (device.category) {
        DeviceCategory.ANDROID -> Icons.Rounded.Smartphone
        DeviceCategory.COMPUTER -> Icons.Rounded.Computer
        DeviceCategory.IOT -> Icons.Rounded.Router
    }
    val statusColor = when (device.status) {
        ConnectionStatus.ONLINE -> EmeraldVolt
        ConnectionStatus.DEGRADED -> StatusWarningAmber
        ConnectionStatus.OFFLINE -> StatusOfflineSlate
    }
    val topicPrefix = (device.metadata as? IotMetadata)?.mqttTopicPrefix ?: "devicexpert/v1/local/${device.id}"

    TelemetryCard(modifier = Modifier.fillMaxWidth(), borderColor = if (isActive) NeonViolet else MaterialTheme.colorScheme.outline, onClick = onSelect) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(NeonViolet.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(if (device.isLocalHost) "${device.name} (Host)" else device.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Text("${device.category.name} • ${device.status.name} • ID: ${device.id}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            if (isActive) Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = EmeraldVolt)
            else if (!device.isLocalHost) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = StatusOfflineSlate) }
        }
        if (!device.isLocalHost) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(OledBlack.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Topic: $topicPrefix/telemetry", style = MaterialTheme.typography.labelSmall, color = CyanPulse, modifier = Modifier.weight(1f))
                IconButton(onClick = { clipboard.setText(AnnotatedString("$topicPrefix/telemetry")) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Topic", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AddRemoteDeviceDialog(
    onDismiss: () -> Unit,
    onAddComputer: (id: String, name: String, os: String, cpu: String, cores: Int, ramGb: Int) -> Unit,
    onAddIot: (id: String, name: String, board: String, prefix: String) -> Unit
) {
    var category by remember { mutableStateOf(DeviceCategory.IOT) }
    var deviceId by remember { mutableStateOf("esp32_node_01") }
    var name by remember { mutableStateOf("Lab ESP32 Sensor") }
    var boardOrOs by remember { mutableStateOf("ESP32-WROOM") }
    var topicPrefix by remember { mutableStateOf("devicexpert/v1/local/esp32_node_01") }
    var ramGbText by remember { mutableStateOf("16") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Pair Remote Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == DeviceCategory.IOT, onClick = {
                        category = DeviceCategory.IOT; deviceId = "esp32_node_01"; name = "Lab ESP32 Sensor"
                        boardOrOs = "ESP32-WROOM"; topicPrefix = "devicexpert/v1/local/esp32_node_01"
                    }, label = { Text("ESP32 / IoT") })
                    FilterChip(selected = category == DeviceCategory.COMPUTER, onClick = {
                        category = DeviceCategory.COMPUTER; deviceId = "desktop_pc_01"; name = "Workstation PC"; boardOrOs = "Linux / Windows"
                    }, label = { Text("Windows / Linux") })
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deviceId, onValueChange = {
                    deviceId = it
                    if (category == DeviceCategory.IOT) topicPrefix = "devicexpert/v1/local/${it.trim()}"
                }, label = { Text("Device ID (Matches Agent)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = boardOrOs, onValueChange = { boardOrOs = it }, label = { Text(if (category == DeviceCategory.IOT) "Board Model" else "Operating System") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (category == DeviceCategory.IOT) {
                    OutlinedTextField(value = topicPrefix, onValueChange = { topicPrefix = it }, label = { Text("MQTT Topic Prefix") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(value = ramGbText, onValueChange = { ramGbText = it.filter { ch -> ch.isDigit() } }, label = { Text("Total RAM (GB)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (category == DeviceCategory.IOT) onAddIot(deviceId, name, boardOrOs, topicPrefix)
                else onAddComputer(deviceId, name, boardOrOs, "Desktop CPU", 8, ramGbText.toIntOrNull() ?: 16)
            }) { Text("Register") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
