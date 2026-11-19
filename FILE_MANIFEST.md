# DeviceXpert V2 — Automated File Change Manifest (V1 → V2)

Generated programmatically by comparing the V1 archive (`DeviceXpert.zip`) against the actual staged V2 repository.

## Summary Counts
- **V1 Baseline Files:** 69
- **V2 Total Staged Files:** 129
- **New Files Added:** 60
- **Existing V1 Files Modified:** 61
- **Existing V1 Files Unchanged:** 8
- **Existing V1 Files Removed:** 0
- **Reconciliation Check:** `8 unchanged + 61 modified + 60 new = 129 total` (matches `129` staged files; `69/69` V1 files accounted for).

## New Files (60)

### Documentation & Cloud Schema (4)
- `CHANGELOG.md`
- `FILE_MANIFEST.md`
- `README.md`
- `supabase/schema.sql`

### Core Utilities & Native Canvas Visualizations (8)
- `app/src/main/java/com/devicexpert/app/core/common/TelemetryFormatters.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/ConcentricStorageRings.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/DeviceWireframeCard.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/DotMatrixProgressBar.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/GradientRingGauge.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/HistoricalLineChart.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/LiveAreaSparkline.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/LiveCompassDial.kt`

### Domain Layer (Models, Managers & Repository Contracts) (10)
- `app/src/main/java/com/devicexpert/app/domain/manager/ActiveDeviceManager.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/battery/BatterySession.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/common/MetricValue.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/sensor/SensorModels.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/settings/AppSettings.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/AlertRepository.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/BatterySessionRepository.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/SensorRepository.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/SettingsRepository.kt`

### Data Layer (Collectors, Room v3, MQTT Transport, Connectors & Repositories) (15)
- `app/src/main/java/com/devicexpert/app/data/collector/DeviceInfoCollector.kt`
- `app/src/main/java/com/devicexpert/app/data/collector/NetworkCollector.kt`
- `app/src/main/java/com/devicexpert/app/data/collector/SensorCollector.kt`
- `app/src/main/java/com/devicexpert/app/data/connector/MqttComputerDeviceConnector.kt`
- `app/src/main/java/com/devicexpert/app/data/initializer/HostDeviceInitializer.kt`
- `app/src/main/java/com/devicexpert/app/data/local/dao/BatterySessionDao.kt`
- `app/src/main/java/com/devicexpert/app/data/local/entity/BatterySessionEntity.kt`
- `app/src/main/java/com/devicexpert/app/data/mapper/AlertMapper.kt`
- `app/src/main/java/com/devicexpert/app/data/remote/mqtt/MqttTransportClient.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/AlertRepositoryImpl.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/BatterySessionRepositoryImpl.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/SensorRepositoryImpl.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/SettingsRepositoryImpl.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/TelemetryRepositoryImpl.kt`

### Presentation Layer (Screens, ViewModels & Components) (21)
- `app/src/main/java/com/devicexpert/app/presentation/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/battery/BatteryDetailViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/cpu/CpuDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/cpu/CpuDetailViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/dashboard/components/DeviceSwitcherSheet.kt`
- `app/src/main/java/com/devicexpert/app/presentation/deviceinfo/DeviceInfoScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/deviceinfo/DeviceInfoViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/devices/DevicesScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/devices/DevicesViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/network/NetworkDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/network/NetworkDetailViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/ram/RamDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/ram/RamDetailViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/sensors/SensorsDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/sensors/SensorsDetailViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/settings/SettingsScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/settings/SettingsViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/storage/StorageDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/storage/StorageDetailViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/thermal/ThermalDetailScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/thermal/ThermalDetailViewModel.kt`

### Unit Tests (2)
- `app/src/test/java/com/devicexpert/app/DeviceSerializationTest.kt`
- `app/src/test/java/com/devicexpert/app/TelemetryFormattersTest.kt`

## Modified V1 Files (61)
- `.github/workflows/android-build.yml`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/java/com/devicexpert/app/DeviceXpertApp.kt`
- `app/src/main/java/com/devicexpert/app/MainActivity.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/Color.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/Theme.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/Typography.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/MetricGauge.kt`
- `app/src/main/java/com/devicexpert/app/core/designsystem/components/TelemetryCard.kt`
- `app/src/main/java/com/devicexpert/app/core/navigation/AppNavigation.kt`
- `app/src/main/java/com/devicexpert/app/core/navigation/NavRoutes.kt`
- `app/src/main/java/com/devicexpert/app/core/security/SecureStorageManager.kt`
- `app/src/main/java/com/devicexpert/app/data/collector/BatteryCollector.kt`
- `app/src/main/java/com/devicexpert/app/data/collector/CpuThermalCollector.kt`
- `app/src/main/java/com/devicexpert/app/data/collector/MemoryStorageCollector.kt`
- `app/src/main/java/com/devicexpert/app/data/connector/LocalAndroidDeviceConnector.kt`
- `app/src/main/java/com/devicexpert/app/data/connector/MqttIotDeviceConnector.kt`
- `app/src/main/java/com/devicexpert/app/data/local/dao/AlertDao.kt`
- `app/src/main/java/com/devicexpert/app/data/local/dao/DeviceDao.kt`
- `app/src/main/java/com/devicexpert/app/data/local/dao/TelemetryDao.kt`
- `app/src/main/java/com/devicexpert/app/data/local/db/Converters.kt`
- `app/src/main/java/com/devicexpert/app/data/local/db/DeviceXpertDatabase.kt`
- `app/src/main/java/com/devicexpert/app/data/local/entity/AlertEntities.kt`
- `app/src/main/java/com/devicexpert/app/data/local/entity/DeviceEntity.kt`
- `app/src/main/java/com/devicexpert/app/data/local/entity/JsonModels.kt`
- `app/src/main/java/com/devicexpert/app/data/local/entity/TelemetryEntity.kt`
- `app/src/main/java/com/devicexpert/app/data/mapper/DeviceMapper.kt`
- `app/src/main/java/com/devicexpert/app/data/mapper/TelemetryMapper.kt`
- `app/src/main/java/com/devicexpert/app/data/remote/supabase/SupabaseTelemetryDto.kt`
- `app/src/main/java/com/devicexpert/app/data/repository/DeviceRepositoryImpl.kt`
- `app/src/main/java/com/devicexpert/app/data/worker/AlertEvaluatorWorker.kt`
- `app/src/main/java/com/devicexpert/app/data/worker/TelemetrySyncWorker.kt`
- `app/src/main/java/com/devicexpert/app/di/ConnectorModule.kt`
- `app/src/main/java/com/devicexpert/app/di/DatabaseModule.kt`
- `app/src/main/java/com/devicexpert/app/di/RepositoryModule.kt`
- `app/src/main/java/com/devicexpert/app/di/SecurityModule.kt`
- `app/src/main/java/com/devicexpert/app/di/SupabaseModule.kt`
- `app/src/main/java/com/devicexpert/app/domain/connector/ConnectorRegistry.kt`
- `app/src/main/java/com/devicexpert/app/domain/connector/DeviceConnector.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/alert/AlertModels.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/device/DeviceEnums.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/device/DeviceMetadata.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/device/UnifiedDevice.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/telemetry/TelemetryPayload.kt`
- `app/src/main/java/com/devicexpert/app/domain/model/telemetry/TelemetrySnapshot.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/DeviceRepository.kt`
- `app/src/main/java/com/devicexpert/app/domain/repository/TelemetryRepository.kt`
- `app/src/main/java/com/devicexpert/app/presentation/alerts/AlertsScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/analytics/AnalyticsScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/analytics/AnalyticsViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/devicexpert/app/presentation/livemonitor/LiveMonitorScreen.kt`
- `app/src/main/java/com/devicexpert/app/presentation/livemonitor/LiveMonitorViewModel.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/values/themes.xml`
- `gradle/libs.versions.toml`
- `reference_agents/esp32_iot/esp32_iot.ino`
- `reference_agents/python_pc/pc_agent.py`

## Unchanged V1 Files (8)
- `.gitignore`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/values/strings.xml`
- `build.gradle.kts`
- `gradle.properties`
- `settings.gradle.kts`

## Removed V1 Files (0)
- None (0 V1 files removed)
