package com.vision.swimsafe.ui.components.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MjpegStreamUrlResolverTest {

    @Test
    fun resolveMjpegStreamUrl_shouldPreferBackendProxyWhenCameraIdValid() {
        val result = resolveMjpegStreamUrl(
            cameraId = 5002L,
            fallbackStreamUrl = "http://127.0.0.1:8300/api/streams/cameras/5002/preview",
            baseUrl = "http://192.168.0.181:8300/api/",
            token = "abc",
        )

        assertEquals(
            "http://192.168.0.181:8300/api/streams/cameras/5002/preview?provider=auto&token=abc",
            result,
        )
    }

    @Test
    fun resolveMjpegStreamUrl_shouldUseHttpFallbackWhenCameraIdMissing() {
        val result = resolveMjpegStreamUrl(
            cameraId = null,
            fallbackStreamUrl = "https://video.example.com/live.mjpeg",
            baseUrl = "http://192.168.0.181:8300/api/",
            token = "abc",
        )

        assertEquals("https://video.example.com/live.mjpeg", result)
    }

    @Test
    fun resolveMjpegStreamUrl_shouldIgnoreUnsupportedFallbackProtocol() {
        val result = resolveMjpegStreamUrl(
            cameraId = null,
            fallbackStreamUrl = "rtsp://camera/live",
            baseUrl = "http://192.168.0.181:8300/api/",
            token = "abc",
        )

        assertNull(result)
    }
}
