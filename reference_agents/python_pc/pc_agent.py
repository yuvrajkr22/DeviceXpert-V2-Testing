import json
import os
import ssl
import time
import psutil
import paho.mqtt.client as mqtt

DEVICE_ID = os.getenv("DEVICEXPERT_DEVICE_ID", "desktop_pc_01")
BROKER = os.getenv("DEVICEXPERT_MQTT_HOST", "broker.hivemq.com")
PORT = int(os.getenv("DEVICEXPERT_MQTT_PORT", "8883"))
USE_TLS = os.getenv("DEVICEXPERT_MQTT_TLS", "true").lower() == "true"
USERNAME = os.getenv("DEVICEXPERT_MQTT_USER", "")
PASSWORD = os.getenv("DEVICEXPERT_MQTT_PASS", "")
INTERVAL_SECONDS = float(os.getenv("DEVICEXPERT_INTERVAL", "2.0"))

TOPIC_PREFIX = f"devicexpert/v1/local/{DEVICE_ID}"
TELEMETRY_TOPIC = f"{TOPIC_PREFIX}/telemetry"
STATUS_TOPIC = f"{TOPIC_PREFIX}/status"


def on_connect(client, userdata, flags, rc):
    print(f"[DeviceXpert PC Agent] Connected to {BROKER}:{PORT} (rc={rc})")
    client.publish(STATUS_TOPIC, "ONLINE", qos=1, retain=True)


def build_mqtt_client():
    if hasattr(mqtt, "CallbackAPIVersion"):
        client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=f"dx-pc-{DEVICE_ID}")
    else:
        client = mqtt.Client(client_id=f"dx-pc-{DEVICE_ID}")

    if USE_TLS:
        client.tls_set_context(ssl.create_default_context())
    if USERNAME:
        client.username_pw_set(USERNAME, PASSWORD)

    client.will_set(STATUS_TOPIC, "OFFLINE", qos=1, retain=True)
    client.on_connect = on_connect
    return client


def read_cpu_temp():
    if not hasattr(psutil, "sensors_temperatures"):
        return None
    try:
        temps = psutil.sensors_temperatures()
        for key in ("coretemp", "k10temp", "cpu_thermal", "acpitz"):
            if key in temps and temps[key]:
                return round(float(temps[key][0].current), 1)
    except Exception:
        pass
    return None


def main():
    client = build_mqtt_client()
    client.connect(BROKER, PORT, keepalive=60)
    client.loop_start()

    boot_time = psutil.boot_time()
    prev_net = psutil.net_io_counters()
    prev_ts = time.monotonic()

    try:
        while True:
            cpu_pct = psutil.cpu_percent(interval=1.0)
            now_ts = time.monotonic()
            curr_net = psutil.net_io_counters()
            elapsed = max(now_ts - prev_ts, 0.001)

            rx_bps = int(max(0, curr_net.bytes_recv - prev_net.bytes_recv) / elapsed)
            tx_bps = int(max(0, curr_net.bytes_sent - prev_net.bytes_sent) / elapsed)
            prev_net = curr_net
            prev_ts = now_ts

            mem = psutil.virtual_memory()
            disk = psutil.disk_usage("/")

            payload = {
                "type": "COMPUTER",
                "cpuUsagePct": round(cpu_pct / 100.0, 4),
                "ramUsedBytes": int(mem.used),
                "diskUsedBytes": int(disk.used),
                "netRxBps": rx_bps,
                "netTxBps": tx_bps,
                "cpuTempC": read_cpu_temp(),
                "uptimeSeconds": int(time.time() - boot_time),
                "topProcessCount": len(psutil.pids()),
            }

            client.publish(TELEMETRY_TOPIC, json.dumps(payload), qos=0)
            time.sleep(max(0.5, INTERVAL_SECONDS - 1.0))

    except KeyboardInterrupt:
        print("\n[DeviceXpert PC Agent] Shutting down...")
    finally:
        client.publish(STATUS_TOPIC, "OFFLINE", qos=1, retain=True)
        client.disconnect()
        client.loop_stop()


if __name__ == "__main__":
    main()
