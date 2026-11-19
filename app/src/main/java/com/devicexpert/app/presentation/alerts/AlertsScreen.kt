package com.devicexpert.app.presentation.alerts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.alert.AlertEvent
import com.devicexpert.app.domain.model.alert.AlertOperator
import com.devicexpert.app.domain.model.alert.AlertRule
import com.devicexpert.app.domain.model.alert.AlertSeverity
import com.devicexpert.app.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository
) : ViewModel() {

    val unacknowledgedEvents: StateFlow<List<AlertEvent>> =
        alertRepository.observeUnacknowledgedEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRules: StateFlow<List<AlertRule>> =
        alertRepository.observeAllRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun acknowledge(alertId: String) { viewModelScope.launch { alertRepository.acknowledgeEvent(alertId) } }
    fun toggleRule(ruleId: String, enabled: Boolean) { viewModelScope.launch { alertRepository.setRuleEnabled(ruleId, enabled) } }
    fun deleteRule(ruleId: String) { viewModelScope.launch { alertRepository.deleteRule(ruleId) } }
    fun createRule(metricKey: String, operator: AlertOperator, threshold: Double, severity: AlertSeverity) {
        viewModelScope.launch {
            alertRepository.saveRule(AlertRule(UUID.randomUUID().toString(), null, metricKey, operator, threshold, severity, true))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val events by viewModel.unacknowledgedEvents.collectAsStateWithLifecycle()
    val rules by viewModel.allRules.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    var hasNotifPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPermission = granted
    }

    if (showAddRuleDialog) {
        AddAlertRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onCreate = { m, o, t, s -> viewModel.createRule(m, o, t, s); showAddRuleDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts & Health Rules", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showAddRuleDialog = true }, containerColor = CyanPulse, contentColor = OledBlack) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Alert Rule")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                TelemetryCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), borderColor = StatusWarningAmber) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = StatusWarningAmber)
                            Column {
                                Text("Enable Alert Notifications", style = MaterialTheme.typography.titleSmall)
                                Text("Allow Android notifications for thermal & RAM threshold breaches.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarningAmber, contentColor = OledBlack)
                        ) { Text("Allow") }
                    }
                }
            }
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active (${events.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Rules (${rules.size})") })
            }
            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    if (events.isEmpty()) {
                        item {
                            TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                Text("All Systems Healthy", style = MaterialTheme.typography.titleMedium, color = EmeraldVolt)
                                Spacer(Modifier.height(4.dp))
                                Text("No unacknowledged alerts. Background rules evaluate automatically via WorkManager.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    } else {
                        items(events, key = { it.alertId }) { event ->
                            val color = if (event.severity == AlertSeverity.CRITICAL) StatusCriticalCrimson else StatusWarningAmber
                            TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                                Text(event.title, style = MaterialTheme.typography.titleMedium, color = color)
                                Spacer(Modifier.height(4.dp))
                                Text(event.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { viewModel.acknowledge(event.alertId) }, colors = ButtonDefaults.buttonColors(containerColor = color)) { Text("Acknowledge") }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp)) {
                    items(rules, key = { it.ruleId }) { rule ->
                        TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${rule.metricKey} ${rule.operator.name} ${rule.thresholdValue}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Severity: ${rule.severity.name}", style = MaterialTheme.typography.labelSmall, color = if (rule.severity == AlertSeverity.CRITICAL) StatusCriticalCrimson else StatusWarningAmber)
                                }
                                Switch(checked = rule.isEnabled, onCheckedChange = { viewModel.toggleRule(rule.ruleId, it) })
                                IconButton(onClick = { viewModel.deleteRule(rule.ruleId) }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete Rule", tint = StatusOfflineSlate) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAlertRuleDialog(
    onDismiss: () -> Unit,
    onCreate: (metric: String, op: AlertOperator, threshold: Double, severity: AlertSeverity) -> Unit
) {
    var metric by remember { mutableStateOf("TEMP_C") }
    var operator by remember { mutableStateOf(AlertOperator.GT) }
    var thresholdText by remember { mutableStateOf("42.0") }
    var severity by remember { mutableStateOf(AlertSeverity.WARNING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Create Alert Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Metric", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TEMP_C" to "Temp", "BATTERY_PCT" to "Battery", "RAM_USED_PCT" to "RAM").forEach { (k, l) ->
                        FilterChip(selected = metric == k, onClick = { metric = k }, label = { Text(l) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = operator == AlertOperator.GT, onClick = { operator = AlertOperator.GT }, label = { Text("Above (>)") })
                    FilterChip(selected = operator == AlertOperator.LT, onClick = { operator = AlertOperator.LT }, label = { Text("Below (<)") })
                }
                OutlinedTextField(value = thresholdText, onValueChange = { thresholdText = it }, label = { Text("Threshold Value") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = severity == AlertSeverity.WARNING, onClick = { severity = AlertSeverity.WARNING }, label = { Text("Warning") })
                    FilterChip(selected = severity == AlertSeverity.CRITICAL, onClick = { severity = AlertSeverity.CRITICAL }, label = { Text("Critical") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { val threshold = thresholdText.toDoubleOrNull() ?: return@Button; onCreate(metric, operator, threshold, severity) }) { Text("Save Rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
