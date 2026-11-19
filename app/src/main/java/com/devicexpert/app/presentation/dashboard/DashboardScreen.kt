package com.devicexpert.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicexpert.app.core.common.TelemetryFormatters
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.core.designsystem.components.*
import com.devicexpert.app.domain.model.device.*
import com.devicexpert.app.domain.model.telemetry.AndroidTelemetry
import com.devicexpert.app.domain.model.telemetry.ComputerTelemetry
import com.devicexpert.app.domain.model.telemetry.IotTelemetry
import com.devicexpert.app.presentation.dashboard.components.DeviceSwitcherSheet
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToRam: (String) -> Unit,
    onNavigateToBattery: (String) -> Unit,
    onNavigateToCpu: (String) -> Unit,
    onNavigateToStorage: (String) -> Unit,
    onNavigateToNetwork: (String) -> Unit,
    onNavigateToSensors: (String) -> Unit,
    onNavigateToDeviceInfo: (String) -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useFahrenheit = LocalUseFahrenheit.current
    var showDeviceSheet by remember { mutableStateOf(false) }

    val activeDevice = uiState.activeDevice
    val activeId = activeDevice?.id ?: "local_android_host"

    if (showDeviceSheet) {
        DeviceSwitcherSheet(
            devices = uiState.allDevices,
            activeDeviceId = activeDevice?.id,
            onSelectDevice = { viewModel.selectDevice(it) },
            onManageFleet = onNavigateToDevices,
            onDismiss = { showDeviceSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardHeader(
            deviceName = activeDevice?.name ?: "DeviceXpert",
            isLivePaused = uiState.isLivePaused,
            onOpenSwitcher = { showDeviceSheet = true },
            onTogglePause = { viewModel.toggleLivePause() }
        )

        if (uiState.streamError != null) {
            TelemetryCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = StatusCriticalCrimson.copy(alpha = 0.14f),
                borderColor = StatusCriticalCrimson.copy(alpha = 0.45f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = uiState.streamError ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusCriticalCrimson
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RamBentoCard(state = uiState, onClick = { onNavigateToRam(activeId) })
                CpuBentoCard(state = uiState, onClick = { onNavigateToCpu(activeId) })
                StorageBentoCard(state = uiState, onClick = { onNavigateToStorage(activeId) })
                SensorsCompassBentoCard(state = uiState, useFahrenheit = useFahrenheit, onClick = { onNavigateToSensors(activeId) })
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BatteryBentoCard(state = uiState, useFahrenheit = useFahrenheit, onClick = { onNavigateToBattery(activeId) })
                DeviceWireframeBentoCard(device = activeDevice, onClick = { onNavigateToDeviceInfo(activeId) })
                NetworkBentoCard(state = uiState, onClick = { onNavigateToNetwork(activeId) })
                SplitActionRow(
                    healthState = uiState.healthState,
                    alertCount = uiState.activeAlertCount,
                    onClickAlerts = onNavigateToAlerts,
                    onClickSettings = onNavigateToSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DashboardHeader(
    deviceName: String,
    isLivePaused: Boolean,
    onOpenSwitcher: () -> Unit,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenSwitcher)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hi, $deviceName",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold),
                color = NeonViolet,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Switch Device", tint = NeonViolet, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onTogglePause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLivePaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isLivePaused) "Resume Live Monitoring" else "Pause Live Monitoring",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RamBentoCard(state: DashboardUiState, onClick: () -> Unit) {
    val androidPayload = state.latestSnapshot?.payload as? AndroidTelemetry
    val pcPayload = state.latestSnapshot?.payload as? ComputerTelemetry
    val androidMeta = state.activeDevice?.metadata as? AndroidMetadata
    val pcMeta = state.activeDevice?.metadata as? ComputerMetadata

    val totalRam = androidPayload?.ramTotalBytes?.takeIf { it > 0L } ?: androidMeta?.totalRamBytes ?: pcMeta?.totalRamBytes ?: 0L
    val usedRam = androidPayload?.ramUsedBytes ?: pcPayload?.ramUsedBytes ?: 0L
    val availRam = androidPayload?.ramAvailableBytes?.takeIf { it > 0L } ?: (totalRam - usedRam).coerceAtLeast(0L)
    val usedRatio = if (totalRam > 0L) (usedRam.toFloat() / totalRam.toFloat()).coerceIn(0f, 1f) else 0f

    TelemetryCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Memory, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Text(
                text = "RAM ${TelemetryFormatters.formatTotalCapacityGb(totalRam)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("Available: ${TelemetryFormatters.formatBytes(availRam)}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
        Spacer(modifier = Modifier.height(12.dp))
        DotMatrixProgressBar(progress = usedRatio, dotCount = 13, activeColor = CyanPulse)
    }
}

@Composable
private fun CpuBentoCard(state: DashboardUiState, onClick: () -> Unit) {
    val androidPayload = state.latestSnapshot?.payload as? AndroidTelemetry
    val pcPayload = state.latestSnapshot?.payload as? ComputerTelemetry
    val loadProgress = androidPayload?.cpuFrequencyLoadPct ?: pcPayload?.cpuUsagePct?.coerceIn(0f, 1f)
    val peakMhz = androidPayload?.cpuCoreFreqsMhz?.maxOrNull() ?: 0
    val clockLabel = when {
        peakMhz > 0 -> TelemetryFormatters.formatFrequencyMhz(peakMhz)
        pcPayload != null -> "${(pcPayload.cpuUsagePct * 100).toInt()}% Load"
        else -> "-- Mhz"
    }

    TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), onClick = onClick) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.DeveloperBoard, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
                Text("CPU", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GradientRingGauge(progress = loadProgress, modifier = Modifier.size(54.dp), strokeWidth = 4.5.dp, fontSize = 12.sp)
                Text(clockLabel, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        CpuAreaSparkline(values = state.cpuLoadHistory, height = 54.dp)
    }
}

@Composable
private fun StorageBentoCard(state: DashboardUiState, onClick: () -> Unit) {
    val androidPayload = state.latestSnapshot?.payload as? AndroidTelemetry
    val pcPayload = state.latestSnapshot?.payload as? ComputerTelemetry
    val androidMeta = state.activeDevice?.metadata as? AndroidMetadata
    val pcMeta = state.activeDevice?.metadata as? ComputerMetadata

    val totalBytes = androidPayload?.storageTotalBytes?.takeIf { it > 0L } ?: androidMeta?.totalStorageBytes ?: pcMeta?.totalDiskBytes ?: 0L
    val usedBytes = androidPayload?.storageUsedBytes ?: pcPayload?.diskUsedBytes ?: 0L
    val availBytes = androidPayload?.storageAvailableBytes?.takeIf { it > 0L } ?: (totalBytes - usedBytes).coerceAtLeast(0L)
    val usedRatio = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val freeRatio = if (totalBytes > 0L) (availBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    TelemetryCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Storage, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
            Text(
                text = "Storage ${TelemetryFormatters.formatTotalCapacityGb(totalBytes)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConcentricStorageRings(outerProgress = usedRatio, middleProgress = 0f, innerProgress = freeRatio, modifier = Modifier.size(66.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(TelemetryFormatters.formatBytes(usedBytes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Used out of ${TelemetryFormatters.formatTotalCapacityGb(totalBytes)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun SensorsCompassBentoCard(state: DashboardUiState, useFahrenheit: Boolean, onClick: () -> Unit) {
    val isLocalAndroid = state.activeDevice?.isLocalHost == true
    val iotPayload = state.latestSnapshot?.payload as? IotTelemetry

    TelemetryCard(
        modifier = Modifier.fillMaxWidth().height(216.dp),
        containerColor = OledBlack,
        borderColor = HairlineBorder,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        if (isLocalAndroid) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LiveCompassDial(azimuthDegrees = state.compassAzimuthDeg, modifier = Modifier.fillMaxSize(0.95f))
            }
        } else if (iotPayload != null) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("IoT Sensors", style = MaterialTheme.typography.titleSmall, color = CyanPulse)
                Text(
                    text = iotPayload.temperatureC?.let { TelemetryFormatters.formatTemperature(it, useFahrenheit) } ?: "--",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text("Humidity: ${iotPayload.humidityPct?.let { "${"%.1f".format(it)}%" } ?: "--"}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Text("RSSI: ${iotPayload.rssiDbm?.let { "$it dBm" } ?: "--"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Explore, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Sensors", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun BatteryBentoCard(state: DashboardUiState, useFahrenheit: Boolean, onClick: () -> Unit) {
    val payload = state.latestSnapshot?.payload as? AndroidTelemetry
    val pct = payload?.batteryPct ?: 0f
    val pctInt = (pct * 100).toInt()
    val tempLabel = payload?.let { TelemetryFormatters.formatTemperature(it.batteryTempC, useFahrenheit) } ?: "--"
    val secondaryElectricLabel = when {
        payload?.batteryChargeCounterMah != null -> "${payload.batteryChargeCounterMah.toInt()} mAh"
        payload != null && payload.batteryVoltageMv > 0 -> "${payload.batteryVoltageMv} mV"
        else -> "--"
    }
    val statusBadge = if (payload?.isCharging == true) "Charging" else "Battery"

    TelemetryCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = if (payload?.isCharging == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(17.dp)
            )
            Text("Battery", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(EmeraldBadgeBg).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(statusBadge, style = MaterialTheme.typography.labelSmall, color = EmeraldVolt)
            }
            Text("$pctInt%", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold), color = EmeraldVolt)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(InactiveTrack)) {
            Box(modifier = Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(EmeraldVolt))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Text(secondaryElectricLabel, style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Text(tempLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DeviceWireframeBentoCard(device: UnifiedDevice?, onClick: () -> Unit) {
    val category = device?.category ?: DeviceCategory.ANDROID
    val (primary, secondary) = when (val meta = device?.metadata) {
        is AndroidMetadata -> {
            val diag = if (meta.displayDiagonalInches > 0f) String.format(Locale.US, "%.1f"", meta.displayDiagonalInches) else "Android"
            val res = if (meta.displayWidthPx > 0 && meta.displayHeightPx > 0) "${maxOf(meta.displayHeightPx, meta.displayWidthPx)}x${minOf(meta.displayHeightPx, meta.displayWidthPx)}" else "API ${meta.sdkInt}"
            diag to res
        }
        is ComputerMetadata -> meta.osName to "${meta.coreCount} Cores"
        is IotMetadata -> meta.boardModel to "FW ${meta.firmwareVersion}"
        null -> "Device" to "Loading..."
    }

    TelemetryCard(modifier = Modifier.fillMaxWidth().height(256.dp), contentPadding = PaddingValues(12.dp), onClick = onClick) {
        DeviceWireframeVisual(category = category, primaryLabel = primary, secondaryLabel = secondary, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun NetworkBentoCard(state: DashboardUiState, onClick: () -> Unit) {
    val androidPayload = state.latestSnapshot?.payload as? AndroidTelemetry
    val pcPayload = state.latestSnapshot?.payload as? ComputerTelemetry
    val headerLabel = when {
        androidPayload?.ipAddress != null -> "Network ${androidPayload.ipAddress}"
        androidPayload != null -> "Network ${androidPayload.netType}"
        pcPayload != null -> "Network PC"
        else -> "Network"
    }
    val rxSpeed = androidPayload?.rxBytesPerSec ?: pcPayload?.netRxBps ?: 0L
    val txSpeed = androidPayload?.txBytesPerSec ?: pcPayload?.netTxBps ?: 0L
    val isOffline = androidPayload?.netType == "OFFLINE"

    TelemetryCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), onClick = onClick) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (isOffline) Icons.Rounded.WifiOff else Icons.Rounded.Wifi, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Text(headerLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(10.dp))
            SpeedIndicatorRow(isDownload = true, text = TelemetryFormatters.formatSpeedBytesPerSec(rxSpeed))
            Spacer(modifier = Modifier.height(6.dp))
            SpeedIndicatorRow(isDownload = false, text = TelemetryFormatters.formatSpeedBytesPerSec(txSpeed))
        }
        Spacer(modifier = Modifier.height(10.dp))
        LiveAreaSparkline(
            series = listOf(
                SparklineSeries(values = state.rxSpeedHistory, lineColors = listOf(CyanPulse)),
                SparklineSeries(values = state.txSpeedHistory, lineColors = listOf(NeonViolet))
            ),
            height = 46.dp,
            fixedMin = 0f,
            fixedMax = null
        )
    }
}

@Composable
private fun SpeedIndicatorRow(isDownload: Boolean, text: String) {
    val badgeColor = if (isDownload) CyanPulse else NeonViolet
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(badgeColor), contentAlignment = Alignment.Center) {
            Icon(if (isDownload) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, contentDescription = null, tint = OledBlack, modifier = Modifier.size(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
    }
}

@Composable
private fun SplitActionRow(
    healthState: HealthBannerState,
    alertCount: Int,
    onClickAlerts: () -> Unit,
    onClickSettings: () -> Unit
) {
    val shieldColor = when (healthState) {
        HealthBannerState.HEALTHY -> EmeraldVolt
        HealthBannerState.WARNING -> StatusWarningAmber
        HealthBannerState.CRITICAL -> StatusCriticalCrimson
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TelemetryCard(modifier = Modifier.weight(1f).aspectRatio(1f), contentPadding = PaddingValues(10.dp), onClick = onClickAlerts) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).border(2.5.dp, shieldColor, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (healthState == HealthBannerState.HEALTHY) Icons.Rounded.GppGood else Icons.Rounded.GppMaybe, contentDescription = "System Health & Alerts", tint = shieldColor, modifier = Modifier.size(24.dp))
                }
                if (alertCount > 0) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).clip(CircleShape).background(shieldColor).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text(alertCount.toString(), style = MaterialTheme.typography.labelSmall, color = OledBlack)
                    }
                }
            }
        }
        TelemetryCard(modifier = Modifier.weight(1f).aspectRatio(1f), contentPadding = PaddingValues(10.dp), onClick = onClickSettings) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp))
            }
        }
    }
}
