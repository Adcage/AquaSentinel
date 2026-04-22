package com.vision.swimsafe.ui.components.video

import com.vision.swimsafe.data.remote.CameraDeviceVo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPlaybackAvailabilityTest {

    @Test
    fun resolveDevicePlaybackAvailability_shouldReturnDeletedWhenCameraMissing() {
        val result = resolveDevicePlaybackAvailability(camera = null)

        assertFalse(result.canPlay)
        assertEquals("设备不存在或已删除", result.message)
    }

    @Test
    fun resolveDevicePlaybackAvailability_shouldReturnDisabledWhenCameraNotEnabled() {
        val result = resolveDevicePlaybackAvailability(
            camera = CameraDeviceVo(
                id = 5002L,
                enabled = 0,
                deviceStatus = "ONLINE",
            ),
        )

        assertFalse(result.canPlay)
        assertEquals("设备未启用，暂无视频画面", result.message)
    }

    @Test
    fun resolveDevicePlaybackAvailability_shouldReturnInvalidWhenCameraIdMissing() {
        val result = resolveDevicePlaybackAvailability(
            camera = CameraDeviceVo(
                id = null,
                enabled = 1,
                deviceStatus = "ONLINE",
            ),
        )

        assertFalse(result.canPlay)
        assertEquals("设备编号缺失，暂无视频画面", result.message)
    }

    @Test
    fun resolveDevicePlaybackAvailability_shouldReturnOfflineWhenDeviceOffline() {
        val result = resolveDevicePlaybackAvailability(
            camera = CameraDeviceVo(
                id = 5002L,
                enabled = 1,
                deviceStatus = "OFFLINE",
            ),
        )

        assertFalse(result.canPlay)
        assertEquals("设备离线，暂无视频画面", result.message)
    }

    @Test
    fun resolveDevicePlaybackAvailability_shouldReturnPlayableWhenOnlineAndEnabled() {
        val result = resolveDevicePlaybackAvailability(
            camera = CameraDeviceVo(
                id = 5002L,
                enabled = 1,
                deviceStatus = "ONLINE",
            ),
        )

        assertTrue(result.canPlay)
        assertEquals(null, result.message)
    }

    @Test
    fun resolveAlarmPlaybackAvailability_shouldReturnNoCameraWhenIdInvalid() {
        val result = resolveAlarmPlaybackAvailability(
            cameraId = null,
            camera = null,
        )

        assertFalse(result.canPlay)
        assertEquals("未绑定摄像头，暂无视频画面", result.message)
    }
}
