package com.vision.swimsafe

import com.vision.swimsafe.data.remote.RemoteMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteMapperTest {

    @Test
    fun alertType_shouldMapBackendEnumToChineseLabel() {
        assertEquals("溺水预警", RemoteMapper.alertTypeToText("DROWNING"))
        assertEquals("人员越界", RemoteMapper.alertTypeToText("CROSS_BORDER"))
        assertEquals("超员告警", RemoteMapper.alertTypeToText("OVER_CAPACITY"))
    }

    @Test
    fun alertStatus_shouldMapBackendEnumToChineseLabel() {
        assertEquals("未处理", RemoteMapper.alertStatusToText("PENDING"))
        assertEquals("处理中", RemoteMapper.alertStatusToText("ASSIGNED"))
        assertEquals("已处理", RemoteMapper.alertStatusToText("DONE"))
        assertEquals("误报", RemoteMapper.alertStatusToText("FALSE_ALARM"))
    }

    @Test
    fun dutyStatus_shouldMapBackendEnumToChineseLabel() {
        assertEquals("在岗中", RemoteMapper.dutyStatusToText("ON_DUTY"))
        assertEquals("离岗", RemoteMapper.dutyStatusToText("OFF_DUTY"))
        assertEquals("离岗", RemoteMapper.dutyStatusToText("LEAVE"))
        assertEquals("围栏外", RemoteMapper.dutyStatusToText("OUT_OF_FENCE"))
    }
}
