package com.vision.swimsafe.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPolishTokensTest {

    @Test
    fun toolbarHeight_shouldBeCompactForContentDensity() {
        assertEquals(52.dp, AppDimens.toolbarHeight)
    }

    @Test
    fun alarmRed_shouldBeMutedToAvoidHarshDialogBackground() {
        assertTrue(AlarmRed.green > 0.2f)
        assertTrue(AlarmRed.blue > 0.2f)
    }
}
