package com.devicexpert.app.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devicexpert.app.core.designsystem.*
import com.devicexpert.app.domain.model.device.ConnectionStatus
import com.devicexpert.app.domain.model.device.DeviceCategory
import com.devicexpert.app.domain.model.device.UnifiedDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSwitcherSheet(
    devices: List<UnifiedDevice>,
    activeDeviceId: String?,
    onSelectDevice: (String) -> Unit,
    onManageFleet: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Monitored Devices", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Switch live telemetry between your Android phone, PCs, and IoT nodes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            devices.forEach { device ->
                val isSelected = device.id == activeDeviceId
                val icon = when (device.category) {
                    DeviceCategory.ANDROID -> Icons.Rounded.Smartphone
                    DeviceCategory.COMPUTER -> Icons.Rounded.Computer
                    DeviceCategory.IOT -> Icons.Rounded.Router
                }
                val statusColor = when (device.status) {
                    ConnectionStatus.ONLINE -> StatusHealthyEmerald
                    ConnectionStatus.DEGRADED -> StatusWarningAmber
                    ConnectionStatus.OFFLINE -> StatusOfflineSlate
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                        )
                        .clickable {
                            onSelectDevice(device.id)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonViolet.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (device.isLocalHost) "${device.name} (This Device)" else device.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                            Text(
                                text = "${device.category.name} • ${device.status.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSelected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = EmeraldVolt, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onManageFleet()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Fleet & Add PC / IoT Device")
            }
        }
    }
}
