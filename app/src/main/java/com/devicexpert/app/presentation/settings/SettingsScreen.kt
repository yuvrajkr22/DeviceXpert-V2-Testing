package com.devicexpert.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.settings.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    var mqttHost by remember(settings.mqttConfig.host) { mutableStateOf(settings.mqttConfig.host) }
    var mqttPort by remember(settings.mqttConfig.port) { mutableStateOf(settings.mqttConfig.port.toString()) }
    var mqttTls by remember(settings.mqttConfig.useTls) { mutableStateOf(settings.mqttConfig.useTls) }
    var mqttUser by remember(settings.mqttConfig.username) { mutableStateOf(settings.mqttConfig.username) }
    var mqttPass by remember(settings.mqttConfig.password) { mutableStateOf(settings.mqttConfig.password) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Security", fontWeight = FontWeight.SemiBold) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (uiState.statusMessage != null) {
                item {
                    TelemetryCard(modifier = Modifier.fillMaxWidth(), borderColor = CyanPulse) {
                        Text(uiState.statusMessage ?: "", style = MaterialTheme.typography.labelLarge, color = CyanPulse)
                    }
                }
            }
            item {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Display & Sampling", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("OLED Dark Theme", style = MaterialTheme.typography.bodyLarge)
                            Text("Pure black canvas optimized for AMOLED", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Switch(checked = settings.darkTheme, onCheckedChange = viewModel::setDarkTheme)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Use Fahrenheit (°F)", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = settings.useFahrenheit, onCheckedChange = viewModel::setUseFahrenheit)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Live Refresh Cadence", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1000L to "1s", 2000L to "2s", 5000L to "5s").forEach { (ms, label) ->
                            FilterChip(selected = settings.livePollingIntervalMs == ms, onClick = { viewModel.setPollingInterval(ms) }, label = { Text(label) })
                        }
                    }
                }
            }
            item {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = EmeraldVolt, modifier = Modifier.size(18.dp))
                        Text("MQTT Broker (AES-256-GCM Encrypted)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = mqttHost, onValueChange = { mqttHost = it }, label = { Text("Broker Host") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = mqttPort, onValueChange = { mqttPort = it.filter { ch -> ch.isDigit() } }, label = { Text("Port") }, singleLine = true, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TLS", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(6.dp))
                            Switch(checked = mqttTls, onCheckedChange = { mqttTls = it; mqttPort = if (it) "8883" else "1883" })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = mqttUser, onValueChange = { mqttUser = it }, label = { Text("Username (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = mqttPass, onValueChange = { mqttPass = it }, label = { Text("Password (Encrypted with Tink)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.saveMqttConfig(mqttHost, mqttPort, mqttTls, mqttUser, mqttPass) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save Encrypted MQTT Settings")
                    }
                }
            }
            item {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                        Text("Supabase Cloud Sync (Optional)", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    when (val auth = uiState.authState) {
                        is AuthState.SignedIn -> {
                            Text("Signed in as ${auth.email}", style = MaterialTheme.typography.bodyLarge, color = EmeraldVolt)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) { Text("Sign Out") }
                        }
                        else -> {
                            if (!uiState.isCloudConfigured) {
                                Text("Running in 100% local offline mode. Add SUPABASE_URL and SUPABASE_ANON_KEY to GitHub Actions Secrets to enable cloud sync.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.signIn(email, password) }, modifier = Modifier.weight(1f)) { Text("Sign In") }
                                OutlinedButton(onClick = { viewModel.signUp(email, password) }, modifier = Modifier.weight(1f)) { Text("Register") }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
