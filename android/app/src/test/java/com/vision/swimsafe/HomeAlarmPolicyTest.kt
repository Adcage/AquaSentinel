package com.vision.swimsafe

import com.vision.swimsafe.data.remote.RemoteHomeRepository
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeAlarmPolicyTest {

    @Test
    fun shouldShowDialogOnHomeLoad_shouldAlwaysBeFalse() {
        assertFalse(RemoteHomeRepository.shouldShowDialogOnHomeLoad("123"))
        assertFalse(RemoteHomeRepository.shouldShowDialogOnHomeLoad(null))
    }
}
