package com.springboot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import com.springboot.config.AppAiEngineProperties;
import com.springboot.model.entity.CameraDevice;

import org.junit.jupiter.api.Test;

class CameraPreviewRouteServiceTest {

    @Test
    void supportsVideoHubShouldAcceptHttpCameraSource() {
        CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

        assertTrue(service.supportsVideoHub(httpCamera()));
        assertFalse(service.supportsVideoHub(rtspCamera()));
    }

    @Test
    void buildVideoHubStreamUriShouldIncludeCameraIdAndEncodedSourceUrl() {
        CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

        URI uri = service.buildVideoHubStreamUri(httpCamera());

        assertTrue(
                uri.toString().startsWith("http://127.0.0.1:5000/video-hub/cameras/1001/stream"));
        assertTrue(uri.toString().contains("source_url=http%3A%2F%2F192.168.1.88%2Fstream"));
    }

    private AppAiEngineProperties newProperties() {
        AppAiEngineProperties properties = new AppAiEngineProperties();
        properties.setBaseUrl("http://127.0.0.1:5000");
        return properties;
    }

    private CameraDevice httpCamera() {
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(1001L);
        cameraDevice.setProtocol("HTTP");
        cameraDevice.setStream_url("http://192.168.1.88/stream");
        return cameraDevice;
    }

    private CameraDevice rtspCamera() {
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(1002L);
        cameraDevice.setProtocol("RTSP");
        cameraDevice.setStream_url("rtsp://192.168.1.99/live");
        return cameraDevice;
    }
}
