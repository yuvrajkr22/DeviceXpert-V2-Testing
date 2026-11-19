# Changelog — DeviceXpert V1 to V2

## [2.0.0] — DeviceXpert V2 (Milestones 1–7 Merged)

### Added
- **OLED Bento Design System & Native Canvas Visualizations:**
  - Upgraded palette to OLED Black (`#000000`) & Charcoal (`#16171A`) with Light theme support and `LocalUseFahrenheit` (`°C / °F`) propagation.
  - Added 7 reusable native Compose components: `DotMatrixProgressBar`, `GradientRingGauge`, `LiveAreaSparkline` / `CpuAreaSparkline`, `ConcentricStorageRings`, `DeviceWireframeVisual`, `LiveCompassDial`, and shared `HistoricalLineChart` (aligned to Vico `2.0.0-beta.2`).
  - Added `TelemetryFormatters` utility (`formatBytes`, `formatTotalCapacityGb`, `formatSpeedBytesPerSec`, `formatFrequencyMhz`, `formatTemperature`, `formatDurationMillis`).
- **Complete Android Hardware Collector Suite:**
  - Added `NetworkCollector` (transport type, IPv4/IPv6, link bandwidth, live RX/TX deltas).
  - Added `SensorCollector` (full `TYPE_ALL` inventory + lifecycle-aware compass, accelerometer, gyroscope, magnetometer, light, proximity, barometer stream).
  - Added `DeviceInfoCollector` (SoC, ABIs, security patch, kernel, physical display geometry & refresh rate).
  - Added `HostDeviceInitializer` to auto-register the local Android device and seed default alert rules on first launch.
- **8 Dedicated Hardware Deep-Dive Screens + Fleet & Settings:**
  - `BatteryDetailScreen` / `BatteryDetailViewModel` (electrical grid, live power sparkline, provenance badges, 24h Vico charts, local Room charge/discharge sessions).
  - `CpuDetailScreen` / `CpuDetailViewModel` (SoC/ABI header, live 60s frequency scaling sparkline, 2-column per-core clock grid with SELinux fallback).
  - `ThermalDetailScreen` / `ThermalDetailViewModel` (throttle severity, API 30+ thermal headroom gauge, live & 24h thermal curves).
  - `RamDetailScreen` / `RamDetailViewModel` (pressure tier, 20-dot bar, live & 24h RAM charts).
  - `StorageDetailScreen` / `StorageDetailViewModel` (concentric storage rings & internal partition breakdown).
  - `NetworkDetailScreen` / `NetworkDetailViewModel` (live dual-line throughput sparkline, peak rates, session vs. boot counters).
  - `SensorsDetailScreen` / `SensorsDetailViewModel` (live compass dial, 3-axis readouts, accelerometer magnitude waveform, searchable hardware inventory).
  - `DeviceInfoScreen` / `DeviceInfoViewModel` (display wireframe, full hardware/OS specs, awake vs. deep sleep ratio).
  - `DevicesScreen` / `DevicesViewModel` & `DeviceSwitcherSheet` (multi-device fleet switcher + PC/ESP32 pairing wizard with copyable MQTT topics).
  - `SettingsScreen` / `SettingsViewModel` (OLED Dark/Light toggle, °C/°F toggle, 1s/2s/5s polling cadence, Tink AES-256-GCM encrypted MQTT credentials, optional Supabase Auth).
- **Room v3 & Clean Architecture Repositories:**
  - Added `BatterySessionEntity`, `BatterySessionDao`, `MIGRATION_2_3`, and `BatterySessionRepositoryImpl`.
  - Added `TelemetryRepositoryImpl`, `AlertRepositoryImpl`, `SensorRepositoryImpl`, `SettingsRepositoryImpl`, `AuthRepositoryImpl`, `AlertMapper`, `ActiveDeviceManager`, and `MetricValue`.
  - Added shared TLS `MqttTransportClient` and `MqttComputerDeviceConnector` for Windows/Linux PC agents.
  - Added `supabase/schema.sql` with strict Row Level Security (`auth.uid() = user_id`).
- **Unit Tests & Documentation:**
  - Added `TelemetryFormattersTest` and `DeviceSerializationTest` under `app/src/test/`.
  - Added `README.md`, `CHANGELOG.md`, and automated `FILE_MANIFEST.md`.

### Changed & Fixed (from V1)
- **Fixed Hardcoded 8 GB RAM Cap:** `LiveMonitorScreen` and `AlertEvaluatorWorker` now divide by real device `totalRamBytes`.
- **Fixed HiveMQ `CompletableFuture.await()` Compile Issue:** Replaced missing `kotlinx-coroutines-jdk8` call with native `suspendCancellableCoroutine` bridge in `MqttTransportClient`.
- **Fixed Vico `2.0.0-beta.2` API Consistency:** Migrated `AnalyticsScreen` and all V2 chart screens to shared `HistoricalLineChart` using verified `VerticalAxis.rememberStart()`, `HorizontalAxis.rememberBottom()`, and `LineCartesianLayer.LineProvider.series(LineCartesianLayer.rememberLine(...))`.
- **Fixed Supabase-kt `3.0.2` Builder Assignment:** Changed `httpEngine { OkHttp.create() }` to `httpEngine = OkHttp.create()` in `SupabaseModule`.
- **Fixed Clean Architecture Layer Leaks:** Refactored `AnalyticsViewModel`, `AlertsViewModel`, `TelemetrySyncWorker`, and `AlertEvaluatorWorker` to inject domain repositories instead of Room DAOs directly.
- **Fixed `ConnectorRegistry` Bypass:** `LiveMonitorViewModel` now resolves telemetry through `ConnectorRegistry`.
- **Scheduled Background Workers:** `DeviceXpertApp` now registers the host device and enqueues unique periodic `TelemetrySyncWorker` and `AlertEvaluatorWorker` jobs (15-minute cadence).
- **Upgraded Alerts Screen:** Added Android 13+ (`POST_NOTIFICATIONS`) runtime permission prompt and interactive Alert Rule Builder tab.
- **Upgraded External Agents:** Updated `pc_agent.py` (TLS 8883, `paho-mqtt` v1/v2 compatibility, true RX/TX B/s deltas) and `esp32_iot.ino` (`WiFiClientSecure` TLS 8883, `ArduinoJson` v7+, aligned topic prefix).

### Preserved
- Zero V1 files were removed. `MetricGauge`, `LiveMonitorScreen`, `AnalyticsScreen`, `Route.DeviceDetails`, `SecureStorageManager`, and all V1 Room entities/DAOs remain intact and wired.
