# DeviceXpert V2 — Universal Native Android Hardware Monitor

DeviceXpert V2 is a native Android hardware-monitoring application built with **Kotlin**, **Jetpack Compose**, **Clean Architecture + MVVM**, **Hilt**, **Room (v3)**, **WorkManager**, **Google Tink + DataStore**, **HiveMQ MQTT 5 (TLS)**, **Vico Charts**, and optional **Supabase** cloud synchronization.

It monitors the host Android smartphone locally out of the box (100% offline-capable) and scales cleanly via polymorphic `DeviceConnector` implementations to **Windows/Linux computers** (`reference_agents/python_pc/pc_agent.py`) and **ESP32/Arduino IoT nodes** (`reference_agents/esp32_iot/esp32_iot.ino`).

---

## Architecture Overview

- **Presentation Layer (`presentation/`):** OLED Black & Charcoal asymmetric 2-column Bento Dashboard, Active Device Switcher sheet, 8 dedicated hardware deep-dive screens (RAM, Battery, CPU, Storage, Network, Thermal, Sensors Hub, Device Info), Device Fleet & Pairing Manager, Alerts & Rule Builder, Settings & Cloud Auth, plus preserved V1 Live Monitor and Performance Analytics screens.
- **Domain Layer (`domain/`):** Pure Kotlin models (`UnifiedDevice`, `DeviceMetadata`, `TelemetryPayload`, `MetricValue`, `BatterySession`, `HardwareSensor`, `AppSettings`, `AlertRule`, `AlertEvent`), `ActiveDeviceManager`, `DeviceConnector` / `ConnectorRegistry`, and repository interfaces.
- **Data Layer (`data/`):**
  - **Android Collectors:** `BatteryCollector`, `MemoryStorageCollector`, `CpuThermalCollector`, `NetworkCollector`, `SensorCollector`, `DeviceInfoCollector`.
  - **Local Persistence:** Room Database v3 (`devices`, `telemetry_snapshots`, `alert_rules`, `alert_events`, `battery_sessions`) with non-destructive `MIGRATION_2_3`.
  - **Background Workers:** `TelemetrySyncWorker` & `AlertEvaluatorWorker` scheduled via `WorkManager` (15-minute minimum Android periodic cadence, 7-day telemetry pruning, 14-day battery session pruning).
  - **Remote & Real-Time:** Shared `MqttTransportClient` (HiveMQ MQTT 5 over TLS port `8883` using Tink-encrypted credentials) serving both `MqttIotDeviceConnector` and `MqttComputerDeviceConnector`, plus optional Supabase Auth & Postgrest sync (`supabase/schema.sql`).

---

## Android Telemetry Transparency

DeviceXpert never fabricates hardware readings:
- **Directly Accessible:** Battery %, voltage, temperature, health, technology, charging state/source, system RAM totals & `lowMemory` pressure, internal flash partition totals (`StatFs`), active network transport & IPv4/IPv6, `TrafficStats` RX/TX counters, `PowerManager` thermal status (API 29+) & thermal headroom (API 30+), full hardware sensor inventory & live compass/motion/light/proximity streams, display geometry & refresh rate, and system uptime/deep-sleep.
- **Calculated / App-Recorded:** Live power (W = V * I), live network speed (byte deltas), live 60s sparklines (in-memory ring buffer), and local Room charge/discharge session history.
- **OEM / OS-Dependent:** Per-core `cpufreq` sysfs clocks (may be SELinux-restricted on some OEMs), battery current/fuel-gauge `CHARGE_COUNTER`, and battery cycle count (`EXTRA_CYCLE_COUNT`, API 34+ only).
- **Unavailable Without Root (Explicitly Labeled):** Aggregate `/proc/stat` CPU %, per-app third-party RAM usage (Android 10+), raw flash SMART counters, and Wi-Fi SSID (omitted intentionally so Location permissions are never required).

---

## Building DeviceXpert V2

### Option 1: GitHub Actions CI (No Local PC or Gradle Required)
`gradle/wrapper/gradle-wrapper.jar` is intentionally omitted so the repository stays 100% text-only and friendly to mobile/Git workflows. The included workflow `.github/workflows/android-build.yml` provisions **JDK 17** and **Gradle 8.9** automatically via `gradle/actions/setup-gradle@v4` and runs:

```bash
gradle assembleDebug testDebugUnitTest --stacktrace
```

1. Push the `DeviceXpert` folder to a GitHub branch (e.g., `v2-review`).
2. *(Optional for Supabase cloud sync)* Add `SUPABASE_URL` and `SUPABASE_ANON_KEY` under **Repository Settings -> Secrets and variables -> Actions**. If omitted, safe placeholder values are injected so local offline builds succeed.
3. Download the compiled `DeviceXpert-V2-debug-apk` artifact from the **Actions** tab.

### Option 2: Android Studio (Desktop)
1. Extract `DeviceXpert-V2.zip`.
2. Open the extracted `DeviceXpert` folder in Android Studio (Ladybug / AGP 8.7.2+ with JDK 17).
3. Allow Gradle sync to resolve dependencies from `gradle/libs.versions.toml` and build/run `:app`.
4. Optional: run `gradle wrapper --gradle-version 8.9` locally if you want to generate `./gradlew` wrapper scripts for CLI use.

---

## External Reference Agents

- **Windows / Linux PC Agent:** `reference_agents/python_pc/pc_agent.py` (`pip install psutil paho-mqtt`). Configure via `DEVICEXPERT_DEVICE_ID`, `DEVICEXPERT_MQTT_HOST`, `DEVICEXPERT_MQTT_PORT`, `DEVICEXPERT_MQTT_USER`, `DEVICEXPERT_MQTT_PASS`.
- **ESP32 IoT Agent:** `reference_agents/esp32_iot/esp32_iot.ino` (uses `WiFiClientSecure`, `PubSubClient`, and `ArduinoJson` v7+ over TLS port `8883`).
- **Supabase Cloud Schema:** Run `supabase/schema.sql` in the Supabase SQL Editor to create RLS-protected tables.
