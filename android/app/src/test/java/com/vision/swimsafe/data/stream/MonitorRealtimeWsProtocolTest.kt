package com.vision.swimsafe.data.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorRealtimeWsProtocolTest {

    @Test
    fun buildWsUrl_shouldConvertHttpApiBaseToWsUrl() {
        val url = MonitorRealtimeWsProtocol.buildWsUrl(
            apiBaseUrl = "http://192.168.0.181:8300/api/",
            token = "abc123",
        )

        assertEquals("ws://192.168.0.181:8300/api/ws/alerts?token=abc123", url)
    }

    @Test
    fun parseFrameHeader_shouldParseMonitorVideoFrameMessage() {
        val header = MonitorRealtimeWsProtocol.parseFrameHeader(
            """
            {
              "messageType": "MONITOR_VIDEO_FRAME",
              "data": {
                "cameraId": 5002,
                "frameTs": 1711111111111,
                "seq": 99
              }
            }
            """.trimIndent(),
        )

        assertNotNull(header)
        assertEquals(5002L, header?.cameraId)
        assertEquals(1711111111111L, header?.frameTs)
    }

    @Test
    fun parseFrameHeader_shouldIgnoreNonFrameMessage() {
        val header = MonitorRealtimeWsProtocol.parseFrameHeader(
            """
            {
              "messageType": "MONITOR_REALTIME_BATCH",
              "data": {
                "5002": {
                  "engine": {
                    "available": true
                  }
                }
              }
            }
            """.trimIndent(),
        )

        assertNull(header)
    }

    @Test
    fun buildSubscribeMessage_shouldContainActionAndCameraIds() {
        val payload = MonitorRealtimeWsProtocol.buildSubscribeMessage(setOf(5002L, 5005L))

        assertEquals("{\"action\":\"SUBSCRIBE_MONITOR_REALTIME\",\"cameraIds\":[5002,5005]}", payload)
    }

    @Test
    fun buildUnsubscribeMessage_shouldContainAction() {
        val payload = MonitorRealtimeWsProtocol.buildUnsubscribeMessage()

        assertEquals("{\"action\":\"UNSUBSCRIBE_MONITOR_REALTIME\"}", payload)
    }
}
