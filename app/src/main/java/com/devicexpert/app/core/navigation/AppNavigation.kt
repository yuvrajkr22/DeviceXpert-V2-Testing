package com.devicexpert.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.devicexpert.app.presentation.alerts.AlertsScreen
import com.devicexpert.app.presentation.analytics.AnalyticsScreen
import com.devicexpert.app.presentation.battery.BatteryDetailScreen
import com.devicexpert.app.presentation.cpu.CpuDetailScreen
import com.devicexpert.app.presentation.dashboard.DashboardScreen
import com.devicexpert.app.presentation.deviceinfo.DeviceInfoScreen
import com.devicexpert.app.presentation.devices.DevicesScreen
import com.devicexpert.app.presentation.livemonitor.LiveMonitorScreen
import com.devicexpert.app.presentation.network.NetworkDetailScreen
import com.devicexpert.app.presentation.ram.RamDetailScreen
import com.devicexpert.app.presentation.sensors.SensorsDetailScreen
import com.devicexpert.app.presentation.settings.SettingsScreen
import com.devicexpert.app.presentation.storage.StorageDetailScreen
import com.devicexpert.app.presentation.thermal.ThermalDetailScreen

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard,
        modifier = modifier
    ) {
        composable<Route.Dashboard> {
            DashboardScreen(
                onNavigateToRam = { navController.navigate(Route.RamDetail(it)) },
                onNavigateToBattery = { navController.navigate(Route.BatteryDetail(it)) },
                onNavigateToCpu = { navController.navigate(Route.CpuDetail(it)) },
                onNavigateToStorage = { navController.navigate(Route.StorageDetail(it)) },
                onNavigateToNetwork = { navController.navigate(Route.NetworkDetail(it)) },
                onNavigateToSensors = { navController.navigate(Route.SensorsDetail(it)) },
                onNavigateToDeviceInfo = { navController.navigate(Route.DeviceInfoDetail(it)) },
                onNavigateToDevices = { navController.navigate(Route.Devices) },
                onNavigateToAlerts = { navController.navigate(Route.Alerts) },
                onNavigateToSettings = { navController.navigate(Route.Settings) }
            )
        }

        composable<Route.DeviceDetails> {
            DeviceInfoScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.BatteryDetail> {
            BatteryDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.CpuDetail> {
            CpuDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToThermal = { deviceId -> navController.navigate(Route.ThermalDetail(deviceId)) }
            )
        }

        composable<Route.ThermalDetail> {
            ThermalDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.RamDetail> {
            RamDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.StorageDetail> {
            StorageDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.NetworkDetail> {
            NetworkDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.SensorsDetail> {
            SensorsDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.DeviceInfoDetail> {
            DeviceInfoScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.Devices> {
            DevicesScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeviceSelected = { navController.popBackStack(Route.Dashboard, inclusive = false) }
            )
        }

        composable<Route.Settings> {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.LiveMonitor> {
            LiveMonitorScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.Analytics> {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Route.Alerts> {
            AlertsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
