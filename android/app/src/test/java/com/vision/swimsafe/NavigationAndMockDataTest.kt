package com.vision.swimsafe

import com.vision.swimsafe.data.remote.RemoteMapper
import com.vision.swimsafe.ui.navigation.mainTabs
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationAndMockDataTest {

    @Test
    fun mainTabs_shouldExposeFiveRequiredEntries() {
        val titles = mainTabs().map { it.title }

        assertEquals(listOf("首页", "报警", "定位", "记录", "我的"), titles)
    }

    @Test
    fun statusTextMapper_shouldAlignWithBackendEnums() {
        assertEquals("在岗中", RemoteMapper.dutyStatusToText("ON_DUTY"))
        assertEquals("围栏外", RemoteMapper.dutyStatusToText("OUT_OF_FENCE"))
    }

    @Test
    fun alertTextMapper_shouldAlignWithBackendEnums() {
        assertEquals("溺水预警", RemoteMapper.alertTypeToText("DROWNING"))
        assertEquals("处理中", RemoteMapper.alertStatusToText("ASSIGNED"))
    }
}
