package com.vision.swimsafe

import com.vision.swimsafe.data.remote.RemoteAlarmAction
import com.vision.swimsafe.data.remote.RemoteMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteMapperActionAndLocationTest {

    @Test
    fun actionType_shouldMapUiActionToBackendEnum() {
        assertEquals("ASSIGN", RemoteMapper.actionToBackend(RemoteAlarmAction.DISPATCH))
        assertEquals("CONFIRM", RemoteMapper.actionToBackend(RemoteAlarmAction.ACKNOWLEDGED))
        assertEquals("DONE", RemoteMapper.actionToBackend(RemoteAlarmAction.RESOLVED))
        assertEquals("FALSE_ALARM", RemoteMapper.actionToBackend(RemoteAlarmAction.FALSE_ALARM))
    }

    @Test
    fun parseCoordinateText_shouldParseValidLatLon() {
        val parsed = RemoteMapper.parseCoordinateText("31.230416, 121.473701")

        assertEquals(31.230416, parsed?.first ?: 0.0, 0.000001)
        assertEquals(121.473701, parsed?.second ?: 0.0, 0.000001)
    }

    @Test
    fun parseCoordinateText_shouldReturnNullForInvalidInput() {
        assertNull(RemoteMapper.parseCoordinateText("--"))
        assertNull(RemoteMapper.parseCoordinateText("abc, def"))
        assertNull(RemoteMapper.parseCoordinateText("31.2"))
    }

    @Test
    fun buildAlertActionRequest_shouldFillAssignPayload() {
        val request = RemoteMapper.buildAlertActionRequest(
            alarmId = 12L,
            action = RemoteAlarmAction.DISPATCH,
            note = "请支援",
            currentLifeguardId = 99L,
        )

        assertEquals(12L, request.alertId)
        assertEquals("ASSIGN", request.actionType)
        assertEquals(99L, request.assigneeLifeguardId)
        assertEquals("请支援", request.actionNote)
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildAlertActionRequest_shouldRejectDispatchWithoutAssignee() {
        RemoteMapper.buildAlertActionRequest(
            alarmId = 12L,
            action = RemoteAlarmAction.DISPATCH,
            note = null,
            currentLifeguardId = null,
        )
    }
}
