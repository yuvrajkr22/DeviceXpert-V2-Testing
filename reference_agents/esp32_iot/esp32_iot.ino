#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASS = "YOUR_WIFI_PASSWORD";

const char* MQTT_HOST = "broker.hivemq.com";
const int   MQTT_PORT = 8883;
const char* MQTT_USER = "";
const char* MQTT_PASS = "";

const char* DEVICE_ID = "esp32_node_01";
const String TOPIC_PREFIX    = String("devicexpert/v1/local/") + DEVICE_ID;
const String TELEMETRY_TOPIC = TOPIC_PREFIX + "/telemetry";
const String STATUS_TOPIC    = TOPIC_PREFIX + "/status";

WiFiClientSecure secureClient;
PubSubClient mqttClient(secureClient);
unsigned long lastPublishMs = 0;

void connectWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void ensureMqttConnected() {
  while (!mqttClient.connected()) {
    bool connected;
    if (strlen(MQTT_USER) > 0) {
      connected = mqttClient.connect(DEVICE_ID, MQTT_USER, MQTT_PASS, STATUS_TOPIC.c_str(), 1, true, "OFFLINE");
    } else {
      connected = mqttClient.connect(DEVICE_ID, nullptr, nullptr, STATUS_TOPIC.c_str(), 1, true, "OFFLINE");
    }
    if (connected) {
      mqttClient.publish(STATUS_TOPIC.c_str(), "ONLINE", true);
    } else {
      delay(3000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  connectWifi();
  // Replace setInsecure() with secureClient.setCACert(ROOT_CA_PEM) for production TLS verification
  secureClient.setInsecure();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setBufferSize(512);
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) connectWifi();
  if (!mqttClient.connected()) ensureMqttConnected();
  mqttClient.loop();

  unsigned long now = millis();
  if (now - lastPublishMs >= 2000UL) {
    lastPublishMs = now;
    float voltageV = analogReadMilliVolts(34) / 1000.0f;

    JsonDocument doc;
    doc["type"] = "IOT";
    doc["temperatureC"] = temperatureRead();
    doc["humidityPct"] = nullptr;
    doc["voltageV"] = voltageV;
    doc["currentMa"] = nullptr;
    doc["powerMw"] = nullptr;
    doc["rssiDbm"] = WiFi.RSSI();
    doc["uptimeSeconds"] = now / 1000UL;

    JsonObject custom = doc["customMetrics"].to<JsonObject>();
    custom["free_heap_kb"] = (double)ESP.getFreeHeap() / 1024.0;

    char payload[512];
    size_t len = serializeJson(doc, payload, sizeof(payload));
    mqttClient.publish(TELEMETRY_TOPIC.c_str(), (const uint8_t*)payload, len, false);
  }
}
