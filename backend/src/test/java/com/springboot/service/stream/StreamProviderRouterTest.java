package com.springboot.service.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.springboot.config.AppStreamProxyProperties;
import com.springboot.model.entity.CameraDevice;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreamProviderRouterTest {

    @Test
    void openShouldFallbackToNextProviderWhenPrimaryFails() {
        AppStreamProxyProperties properties = new AppStreamProxyProperties();
        properties.setMode("auto");
        properties.setProviderPriority(List.of("broken", "ok"));
        StreamProvider broken = new StreamProvider() {
            @Override
            public String name() {
                return "broken";
            }

            @Override
            public boolean supports(String sourceProtocol) {
                return true;
            }

            @Override
            public StreamSession open(CameraDevice cameraDevice, StreamOpenRequest request) {
                throw new RuntimeException("broken");
            }
        };
        StreamProvider ok = new StreamProvider() {
            @Override
            public String name() {
                return "ok";
            }

            @Override
            public boolean supports(String sourceProtocol) {
                return true;
            }

            @Override
            public StreamSession open(CameraDevice cameraDevice, StreamOpenRequest request) {
                return new StreamSession("ok", "multipart/x-mixed-replace", cameraDevice.getStream_url(),
                        outputStream -> {
                        }, null);
            }
        };
        StreamProviderRouter router = new StreamProviderRouter(properties, List.of(broken, ok));

        StreamSession session = router.open(newCamera(), StreamOpenRequest.external("auto"));

        assertEquals("ok", session.getProviderName());
    }

    @Test
    void openShouldFailWhenPreferredProviderUnavailableAndFallbackDisabled() {
        AppStreamProxyProperties properties = new AppStreamProxyProperties();
        StreamProviderRouter router = new StreamProviderRouter(properties, List.of());

        StreamOpenRequest request = StreamOpenRequest.builder()
                .preferredProvider("ffmpeg")
                .allowFallback(false)
                .internalRequest(false)
                .build();

        assertThrows(Exception.class, () -> router.open(newCamera(), request));
    }

    private CameraDevice newCamera() {
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(1001L);
        cameraDevice.setProtocol("RTSP");
        cameraDevice.setStream_url("rtsp://example/live");
        return cameraDevice;
    }
}
