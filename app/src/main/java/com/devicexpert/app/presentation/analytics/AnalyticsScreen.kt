package com.devicexpert.app.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.devicexpert.app.core.designsystem.ElectricCyan
import com.devicexpert.app.core.designsystem.StatusWarningAmber
import com.devicexpert.app.core.designsystem.TextSecondary
import com.devicexpert.app.core.designsystem.components.HistoricalLineChart
import com.devicexpert.app.core.designsystem.components.TelemetryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasData by viewModel.hasData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics") },
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
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !hasData -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Collecting Analytics History", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Charts render automatically once WorkManager stores at least 2 telemetry snapshots.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    TelemetryCard(Modifier.fillMaxWidth()) {
                        Text("Battery Curve (%)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        HistoricalLineChart(modelProducer = viewModel.batteryChartProducer, lineColor = ElectricCyan, height = 220.dp)
                    }
                }
                item {
                    TelemetryCard(Modifier.fillMaxWidth()) {
                        Text("Thermal Profile (°C)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        HistoricalLineChart(modelProducer = viewModel.tempChartProducer, lineColor = StatusWarningAmber, height = 220.dp)
                    }
                }
            }
        }
    }
}
