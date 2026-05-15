package com.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import com.springboot.config.AppVideoHubProperties;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.vo.CameraDeviceVO;
import com.springboot.service.impl.CameraDeviceServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
                uri.toString().startsWith("http://127.0.0.1:5100/video-hub/cameras/1001/stream"));
        assertTrue(uri.toString().contains("source_url=http%3A%2F%2F192.168.1.88%2Fstream"));
    }

    @Test
    void buildPreviewUrlShouldReturnPlatformPreviewEndpoint() {
        CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

        assertEquals("/streams/cameras/1001/preview", service.buildPreviewUrl(httpCamera()));
        assertEquals("", service.buildPreviewUrl(rtspCamera()));
    }

    @Test
    void buildDeviceBaseUrlShouldReturnEsp32Origin() {
        CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

        assertEquals("http://192.168.1.88", service.buildDeviceBaseUrl(httpCamera()));
        assertEquals("", service.buildDeviceBaseUrl(rtspCamera()));
    }

    @Test
    void getCameraDeviceVOShouldIncludePreviewUrlAndDeviceBaseUrl() {
        CameraPreviewRouteService routeService = new CameraPreviewRouteService(newProperties());
        CameraDeviceServiceImpl service = new CameraDeviceServiceImpl();
        ReflectionTestUtils.setField(service, "cameraPreviewRouteService", routeService);

        CameraDeviceVO vo = service.getCameraDeviceVO(httpCamera());

        assertEquals("/streams/cameras/1001/preview", vo.getPreviewUrl());
        assertEquals("http://192.168.1.88", vo.getDeviceBaseUrl());
        assertEquals("http://192.168.1.88/stream", vo.getStreamUrl());
    }

    private AppVideoHubProperties newProperties() {
        AppVideoHubProperties properties = new AppVideoHubProperties();
        properties.setBaseUrl("http://127.0.0.1:5100");
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
