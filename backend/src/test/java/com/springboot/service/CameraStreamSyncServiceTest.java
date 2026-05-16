package com.springboot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CameraStreamSyncServiceTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private CameraDeviceService cameraDeviceService;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private CameraStreamSyncService service;
    private final ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CameraStreamSyncService();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(service, "cameraDeviceService", cameraDeviceService);
        ReflectionTestUtils.setField(service, "objectMapper", realMapper);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void upsertCameraStream_writesToRedis_whenHttpStreamAndEnabled() {
        service.upsertCameraStream(1L, "http://192.168.1.88/stream", 1);
        verify(hashOperations).put(eq("aqua:camera:streams"), eq("1"), anyString());
        verify(stringRedisTemplate).convertAndSend(eq("aqua:camera:events"), anyString());
    }

    @Test
    void upsertCameraStream_writesToRedis_whenHttpsStreamAndEnabled() {
        service.upsertCameraStream(2L, "https://example.com/stream", 1);
        verify(hashOperations).put(eq("aqua:camera:streams"), eq("2"), anyString());
    }

    @Test
    void upsertCameraStream_writesToRedis_whenRtspStreamAndEnabled() {
        service.upsertCameraStream(3L, "rtsp://10.10.10.1/live/1", 1);
        verify(hashOperations).put(eq("aqua:camera:streams"), eq("3"), anyString());
        verify(stringRedisTemplate).convertAndSend(eq("aqua:camera:events"), anyString());
    }

    @Test
    void upsertCameraStream_removesFromRedis_whenDisabled() {
        service.upsertCameraStream(4L, "http://192.168.1.88/stream", 0);
        verify(hashOperations).delete(eq("aqua:camera:streams"), eq("4"));
        verify(hashOperations, never()).put(anyString(), anyString(), anyString());
    }

    @Test
    void upsertCameraStream_removesFromRedis_whenBlankStreamUrl() {
        service.upsertCameraStream(5L, "", 1);
        verify(hashOperations).delete(eq("aqua:camera:streams"), eq("5"));
    }

    @Test
    void upsertCameraStream_removesFromRedis_whenUnsupportedProtocol() {
        service.upsertCameraStream(6L, "udp://192.168.1.88/stream", 1);
        verify(hashOperations).delete(eq("aqua:camera:streams"), eq("6"));
        verify(hashOperations, never()).put(anyString(), anyString(), anyString());
    }

    @Test
    void upsertCameraStream_skips_whenCameraIdNull() {
        service.upsertCameraStream(null, "http://x/stream", 1);
        verify(hashOperations, never()).put(anyString(), anyString(), anyString());
        verify(hashOperations, never()).delete(anyString(), any());
    }

    @Test
    void removeCameraStream_deletesFromRedis() {
        service.removeCameraStream(7L);
        verify(hashOperations).delete(eq("aqua:camera:streams"), eq("7"));
        verify(stringRedisTemplate).convertAndSend(eq("aqua:camera:events"), anyString());
    }

    @Test
    void removeCameraStream_skips_whenCameraIdNull() {
        service.removeCameraStream(null);
        verify(hashOperations, never()).delete(anyString(), any());
    }
}
