package com.devicexpert.app.presentation.sensors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.LiveAreaSparkline
import com.devicexpert.app.core.designsystem.components.LiveCompassDial
import com.devicexpert.app.core.designsystem.components.SparklineSeries
import com.devicexpert.app.core.designsystem.components.TelemetryCard
import com.devicexpert.app.domain.model.sensor.HardwareSensor
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorsDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: SensorsDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensors Hub", fontWeight = FontWeight.SemiBold) },
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
            !uiState.isLocalHost -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                TelemetryCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Remote Device Sensors", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Hardware motion and compass sensors are streamed directly on local Android devices. Remote IoT sensor readings appear on the Bento Dashboard and Live Monitor.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            else -> {
                val readings = uiState.liveReadings
                val heading = readings.compassAzimuthDeg
                val headingText = heading?.let { "${it.roundToInt()}° ${azimuthToCardinal(it)}" } ?: "No Magnetometer"
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        TelemetryCard(modifier = Modifier.fillMaxWidth(), containerColor = OledBlack, borderColor = HairlineBorder) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Digital Compass", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Spacer(Modifier.height(6.dp))
                                    Text(headingText, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp), color = CyanPulse)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${uiState.allSensors.size} hardware sensors detected", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                }
                                LiveCompassDial(azimuthDegrees = heading, modifier = Modifier.size(136.dp))
                            }
                        }
                    }
                    item { ThreeAxisSensorCard("Accelerometer (m/s²)", readings.accelerometer, CyanPulse, uiState.accelMagnitudeHistory) }
                    item { ThreeAxisSensorCard("Gyroscope (rad/s)", readings.gyroscope, NeonViolet, emptyList()) }
                    item { ThreeAxisSensorCard("Magnetometer (µT)", readings.magnetometer, EmeraldVolt, emptyList()) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScalarSensorTile("Ambient Light", readings.lightLux?.let { String.format(Locale.US, "%.0f lx", it) } ?: "Not present", StorageAmber, Modifier.weight(1f))
                            ScalarSensorTile("Proximity", readings.proximityCm?.let { String.format(Locale.US, "%.1f cm", it) } ?: "Not present", CpuMagenta, Modifier.weight(1f))
                        }
                    }
                    if (readings.pressureHpa != null) {
                        item { ScalarSensorTile("Barometer", String.format(Locale.US, "%.1f hPa", readings.pressureHpa), CpuBlue, Modifier.fillMaxWidth()) }
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("All Hardware Sensors (${uiState.filteredSensors.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Filter by sensor name or vendor") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    itemsIndexed(items = uiState.filteredSensors, key = { index, sensor -> "${sensor.type}_${sensor.name}_$index" }) { _, sensor ->
                        SensorInventoryRow(sensor)
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ThreeAxisSensorCard(title: String, values: Triple<Float, Float, Float>?, accent: Color, sparklineValues: List<Float>) {
    TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = accent)
            Spacer(Modifier.height(10.dp))
            if (values == null) {
                Text("Hardware not present or idle", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AxisValue("X", values.first); AxisValue("Y", values.second); AxisValue("Z", values.third)
                }
            }
        }
        if (sparklineValues.size >= 2) {
            LiveAreaSparkline(series = listOf(SparklineSeries(sparklineValues, listOf(accent), accent.copy(alpha = 0.24f))), height = 50.dp, fixedMin = 0f, fixedMax = null)
        }
    }
}

@Composable
private fun AxisValue(axis: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(axis, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(String.format(Locale.US, "%+.2f", value), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ScalarSensorTile(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    TelemetryCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = accent)
    }
}

@Composable
private fun SensorInventoryRow(sensor: HardwareSensor) {
    TelemetryCard(modifier = Modifier.fillMaxWidth()) {
        Text(sensor.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Text(sensor.vendor, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Power: ${String.format(Locale.US, "%.2f", sensor.powerMa)} mA", style = MaterialTheme.typography.labelSmall, color = CyanPulse)
            Text("Max: ${String.format(Locale.US, "%.2f", sensor.maxRange)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Res: ${String.format(Locale.US, "%.4f", sensor.resolution)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

private fun azimuthToCardinal(deg: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[(((deg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)]
}
