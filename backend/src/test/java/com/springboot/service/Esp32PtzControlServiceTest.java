package com.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class Esp32PtzControlServiceTest {

    @Test
    void resolveDeviceBaseUrlShouldExtractOriginFromHttpStreamUrl() {
        Esp32PtzControlService service = new Esp32PtzControlService();
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setStream_url("http://192.168.137.86:81/stream");

        String baseUrl =
                ReflectionTestUtils.invokeMethod(service, "resolveDeviceBaseUrl", cameraDevice);

        assertEquals("http://192.168.137.86:81", baseUrl);
    }

    @Test
    void resolveDeviceBaseUrlShouldRejectPlatformPreviewPath() {
        Esp32PtzControlService service = new Esp32PtzControlService();
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setStream_url("/streams/cameras/1001/preview");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                ReflectionTestUtils.invokeMethod(
                                        service, "resolveDeviceBaseUrl", cameraDevice));

        assertTrue(exception.getMessage().contains("设备 streamUrl 需为 ESP32 HTTP 地址"));
    }
}
