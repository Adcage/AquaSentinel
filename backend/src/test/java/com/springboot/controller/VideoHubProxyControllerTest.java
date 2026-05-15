package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.config.AppVideoHubProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.security.StreamTokenAuthService;
import com.springboot.service.CameraDeviceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class VideoHubProxyControllerTest {

    @Mock private AppVideoHubProperties videoHubProperties;

    @Mock private StreamTokenAuthService streamTokenAuthService;

    @Mock private CameraDeviceService cameraDeviceService;

    @Mock private RestTemplate restTemplate;

    private VideoHubProxyController controller;

    @BeforeEach
    void setUp() {
        controller = new VideoHubProxyController();
        ReflectionTestUtils.setField(controller, "videoHubProperties", videoHubProperties);
        ReflectionTestUtils.setField(controller, "streamTokenAuthService", streamTokenAuthService);
        ReflectionTestUtils.setField(controller, "cameraDeviceService", cameraDeviceService);
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
    }

    private void stubBaseUrl() {
        when(videoHubProperties.getBaseUrl()).thenReturn("http://127.0.0.1:5100");
    }

    private void stubTokenParamName() {
        when(streamTokenAuthService.resolveTokenParamName()).thenReturn("token");
    }

    private CameraDevice stubCamera(Long cameraId, String streamUrl) {
        CameraDevice camera = new CameraDevice();
        camera.setId(cameraId);
        camera.setStream_url(streamUrl);
        when(cameraDeviceService.getById(cameraId)).thenReturn(camera);
        return camera;
    }

    @Test
    void whipProxyForwardsSdpWithSourceUrl() {
        stubBaseUrl();
        stubTokenParamName();
        doNothing().when(streamTokenAuthService).verifyPreviewToken("valid-token");
        stubCamera(1001L, "http://192.168.1.100/stream");

        byte[] sdpAnswer = "v=0\r\no=- 123 1 IN IP4 0.0.0.0\r\n".getBytes();
        HttpHeaders yoloHeaders = new HttpHeaders();
        yoloHeaders.setContentType(MediaType.valueOf("application/sdp"));
        yoloHeaders.set(HttpHeaders.LOCATION, "/video-hub/sessions/abc-123");
        ResponseEntity<byte[]> yoloResponse =
                new ResponseEntity<>(sdpAnswer, yoloHeaders, HttpStatus.CREATED);

        when(restTemplate.exchange(
                        any(URI.class),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(byte[].class)))
                .thenReturn(yoloResponse);

        Map<String, String> params = Map.of("token", "valid-token");
        ResponseEntity<byte[]> result = controller.whipOffer(1001L, params, "sdp-offer".getBytes());

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(MediaType.valueOf("application/sdp"), result.getHeaders().getContentType());
        assertEquals(
                "/api/video-hub/sessions/abc-123",
                result.getHeaders().getFirst(HttpHeaders.LOCATION));
        verify(streamTokenAuthService).verifyPreviewToken("valid-token");
    }

    @Test
    void whipProxyRejectsInvalidToken() {
        stubTokenParamName();
        doThrow(new BusinessException(com.springboot.common.ErrorCode.NOT_LOGIN_ERROR, "缺少视频流访问令牌"))
                .when(streamTokenAuthService)
                .verifyPreviewToken("bad-token");

        Map<String, String> params = Map.of("token", "bad-token");

        try {
            controller.whipOffer(1001L, params, "sdp-offer".getBytes());
            assertTrue(false, "应抛出 BusinessException");
        } catch (BusinessException e) {
            assertEquals(com.springboot.common.ErrorCode.NOT_LOGIN_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void whipDeleteForwardsToYoloService() {
        stubBaseUrl();
        doNothing().when(restTemplate).delete(any(String.class));

        BaseResponse<Boolean> result = controller.deleteWhipSession("abc-123");

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(restTemplate).delete("http://127.0.0.1:5100/video-hub/sessions/abc-123");
    }

    @Test
    void whipDeleteThrowsOnYoloError() {
        stubBaseUrl();
        doThrow(new RuntimeException("Connection refused"))
                .when(restTemplate)
                .delete(any(String.class));

        try {
            controller.deleteWhipSession("abc-123");
            assertTrue(false, "应抛出 BusinessException");
        } catch (BusinessException e) {
            assertEquals(com.springboot.common.ErrorCode.OPERATION_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void whipProxyLocationHeaderNoRewriteIfAlreadyHasApiPrefix() {
        stubBaseUrl();
        stubTokenParamName();
        doNothing().when(streamTokenAuthService).verifyPreviewToken("valid-token");
        stubCamera(1001L, "http://192.168.1.100/stream");

        byte[] sdpAnswer = "answer".getBytes();
        HttpHeaders yoloHeaders = new HttpHeaders();
        yoloHeaders.set(HttpHeaders.LOCATION, "/api/video-hub/sessions/xyz");
        ResponseEntity<byte[]> yoloResponse =
                new ResponseEntity<>(sdpAnswer, yoloHeaders, HttpStatus.CREATED);

        when(restTemplate.exchange(
                        any(URI.class),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(byte[].class)))
                .thenReturn(yoloResponse);

        Map<String, String> params = Map.of("token", "valid-token");
        ResponseEntity<byte[]> result = controller.whipOffer(1001L, params, "offer".getBytes());

        assertEquals(
                "/api/video-hub/sessions/xyz", result.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
