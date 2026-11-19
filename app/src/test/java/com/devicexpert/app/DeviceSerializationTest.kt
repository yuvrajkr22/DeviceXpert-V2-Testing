package com.devicexpert.app

import com.devicexpert.app.data.local.entity.TelemetryPayloadDto
import com.devicexpert.app.data.mapper.deviceJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSerializationTest {

    @Test
    fun computerAgentJson_deserializesBothDirectlyAndPolymorphically() {
        val pythonAgentJson = """
            {
              "type": "COMPUTER",
              "cpuUsagePct": 0.425,
              "ramUsedBytes": 8589934592,
              "diskUsedBytes": 120000000000,
              "netRxBps": 15420,
              "netTxBps": 4096,
              "cpuTempC": 58.4,
              "uptimeSeconds": 7200,
              "topProcessCount": 184
            }
        """.trimIndent()

        val direct = deviceJson.decodeFromString<TelemetryPayloadDto.Computer>(pythonAgentJson)
        assertEquals(0.425f, direct.cpuUsagePct, 0.0001f)
        assertEquals(15420L, direct.netRxBps)
        assertEquals(58.4f, direct.cpuTempC ?: 0f, 0.01f)

        val polymorphic = deviceJson.decodeFromString<TelemetryPayloadDto>(pythonAgentJson)
        assertTrue(polymorphic is TelemetryPayloadDto.Computer)
    }

    @Test
    fun esp32AgentJson_deserializesWithNullableSensorsAndCustomMetrics() {
        val esp32Json = """
            {
              "type": "IOT",
              "temperatureC": 41.2,
              "humidityPct": null,
              "voltageV": 3.28,
              "currentMa": null,
              "powerMw": null,
              "rssiDbm": -64,
              "uptimeSeconds": 360,
              "customMetrics": {
                "free_heap_kb": 214.5
              }
            }
        """.trimIndent()

        val direct = deviceJson.decodeFromString<TelemetryPayloadDto.Iot>(esp32Json)
        assertEquals(41.2f, direct.temperatureC ?: 0f, 0.01f)
        assertNull(direct.humidityPct)
        assertEquals(-64, direct.rssiDbm)
        assertEquals(214.5, direct.customMetrics["free_heap_kb"] ?: 0.0, 0.01)

        val polymorphic = deviceJson.decodeFromString<TelemetryPayloadDto>(esp32Json)
        assertTrue(polymorphic is TelemetryPayloadDto.Iot)
    }
}
