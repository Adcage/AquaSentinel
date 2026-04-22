package com.vision.swimsafe.data.alert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealtimeAlertNotifierTest {

    @Test
    fun parseAlertPayload_shouldReturnNullForNonAlertMessage() {
        val raw = """{"messageType":"MONITOR_REALTIME_HEARTBEAT","data":{}}"""

        val payload = RealtimeAlertNotifier.parseAlertPayload(raw)

        assertNull(payload)
    }

    @Test
    fun parseAlertPayload_shouldParseAlertCreatedPayload() {
        val raw = """
            {
              "messageType": "ALERT_CREATED",
              "eventUid": "evt_123",
              "data": {
                "alertId": 987,
                "riskLevel": "HIGH"
              }
            }
        """.trimIndent()

        val payload = RealtimeAlertNotifier.parseAlertPayload(raw)

        assertEquals("ALERT_CREATED", payload?.messageType)
        assertEquals("evt_123", payload?.eventUid)
        assertEquals(987L, payload?.data?.alertId)
        assertEquals("HIGH", payload?.data?.riskLevel)
    }
}
