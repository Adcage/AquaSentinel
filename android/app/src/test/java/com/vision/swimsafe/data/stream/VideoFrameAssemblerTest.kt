package com.vision.swimsafe.data.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoFrameAssemblerTest {

    @Test
    fun pollHeader_shouldKeepOrderWhenMultipleHeadersArriveBeforeBinary() {
        val assembler = VideoFrameAssembler(maxPendingHeaders = 10)
        assembler.pushHeader(MonitorVideoFrameHeader(cameraId = 5001L, frameTs = 1L, seq = 1L))
        assembler.pushHeader(MonitorVideoFrameHeader(cameraId = 5002L, frameTs = 2L, seq = 2L))

        val first = assembler.pollHeader()
        val second = assembler.pollHeader()

        assertEquals(5001L, first?.cameraId)
        assertEquals(5002L, second?.cameraId)
    }

    @Test
    fun pushHeader_shouldDropOldestWhenExceedLimit() {
        val assembler = VideoFrameAssembler(maxPendingHeaders = 2)
        assembler.pushHeader(MonitorVideoFrameHeader(cameraId = 5001L, frameTs = 1L, seq = 1L))
        assembler.pushHeader(MonitorVideoFrameHeader(cameraId = 5002L, frameTs = 2L, seq = 2L))
        assembler.pushHeader(MonitorVideoFrameHeader(cameraId = 5003L, frameTs = 3L, seq = 3L))

        val first = assembler.pollHeader()
        val second = assembler.pollHeader()
        val third = assembler.pollHeader()

        assertEquals(5002L, first?.cameraId)
        assertEquals(5003L, second?.cameraId)
        assertNull(third)
    }
}
