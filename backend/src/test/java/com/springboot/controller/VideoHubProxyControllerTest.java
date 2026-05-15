package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.security.StreamTokenAuthService;
import com.springboot.service.CameraDeviceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VideoHubProxyControllerTest {

    @Mock private StreamTokenAuthService streamTokenAuthService;

    @Mock private CameraDeviceService cameraDeviceService;

    private VideoHubProxyController controller;

    @BeforeEach
    void setUp() {
        controller = new VideoHubProxyController();
        ReflectionTestUtils.setField(controller, "streamTokenAuthService", streamTokenAuthService);
        ReflectionTestUtils.setField(controller, "cameraDeviceService", cameraDeviceService);
    }

    @Test
    void verifyPreviewTokenReturnsSuccessWhenTokenIsValid() {
        doNothing().when(streamTokenAuthService).verifyPreviewToken("valid-token");

        BaseResponse<Boolean> result = controller.verifyPreviewToken("valid-token");

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(streamTokenAuthService).verifyPreviewToken("valid-token");
    }

    @Test
    void verifyPreviewTokenThrowsWhenTokenIsInvalid() {
        doThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "缺少视频流访问令牌"))
                .when(streamTokenAuthService)
                .verifyPreviewToken("bad-token");

        try {
            controller.verifyPreviewToken("bad-token");
            assertTrue(false, "应抛出 BusinessException");
        } catch (BusinessException e) {
            assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void resolveCameraSourceReturnsSourceUrlWhenCameraExists() {
        CameraDevice camera = new CameraDevice();
        camera.setId(1001L);
        camera.setStream_url("http://192.168.137.173/stream");
        doNothing().when(streamTokenAuthService).verifyPreviewToken("valid-token");
        when(cameraDeviceService.getById(1001L)).thenReturn(camera);

        BaseResponse<java.util.Map<String, String>> result =
                controller.resolveCameraSource(1001L, "valid-token");

        assertEquals(0, result.getCode());
        assertEquals("http://192.168.137.173/stream", result.getData().get("sourceUrl"));
        verify(streamTokenAuthService).verifyPreviewToken("valid-token");
        verify(cameraDeviceService).getById(1001L);
    }

    @Test
    void resolveCameraSourceThrowsWhenCameraMissing() {
        doNothing().when(streamTokenAuthService).verifyPreviewToken("valid-token");
        when(cameraDeviceService.getById(1001L)).thenReturn(null);

        try {
            controller.resolveCameraSource(1001L, "valid-token");
            assertTrue(false, "应抛出 BusinessException");
        } catch (BusinessException e) {
            assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void deleteWhipSessionReturnsSuccess() {
        BaseResponse<Boolean> result = controller.deleteWhipSession("abc-123");

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
    }
}
