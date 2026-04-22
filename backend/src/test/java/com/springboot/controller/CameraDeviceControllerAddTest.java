package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.springboot.common.BaseResponse;
import com.springboot.model.dto.cameradevice.CameraDeviceAddRequest;
import com.springboot.model.entity.CameraDevice;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.CameraDeviceService;
import com.springboot.websocket.AlertWsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CameraDeviceControllerAddTest {

    @Mock
    private CameraDeviceService cameraDeviceService;

    @Mock
    private AlertWsPublisher alertWsPublisher;

    @Mock
    private AiStreamTaskService aiStreamTaskService;

    private CameraDeviceController controller;

    @BeforeEach
    void setUp() {
        controller = new CameraDeviceController();
        ReflectionTestUtils.setField(controller, "cameraDeviceService", cameraDeviceService);
        ReflectionTestUtils.setField(controller, "alertWsPublisher", alertWsPublisher);
        ReflectionTestUtils.setField(controller, "aiStreamTaskService", aiStreamTaskService);
    }

    @Test
    void addCameraDeviceShouldNormalizeZeroZoneIdToNull() {
        CameraDeviceAddRequest request = new CameraDeviceAddRequest();
        request.setVenueId(1L);
        request.setZoneId(0L);
        request.setCameraCode("CAM-001");
        request.setCameraName("测试设备");
        request.setStreamUrl("rtsp://example/stream");

        when(cameraDeviceService.save(any(CameraDevice.class))).thenAnswer(invocation -> {
            CameraDevice cameraDevice = invocation.getArgument(0);
            cameraDevice.setId(1001L);
            return true;
        });

        BaseResponse<Long> response = controller.addCameraDevice(request);

        assertEquals(0, response.getCode());
        assertEquals(1001L, response.getData());

        ArgumentCaptor<CameraDevice> captor = ArgumentCaptor.forClass(CameraDevice.class);
        org.mockito.Mockito.verify(cameraDeviceService).save(captor.capture());
        assertNull(captor.getValue().getZone_id());
    }

    @Test
    void addCameraDeviceShouldKeepPositiveZoneId() {
        CameraDeviceAddRequest request = new CameraDeviceAddRequest();
        request.setVenueId(1L);
        request.setZoneId(12L);
        request.setCameraCode("CAM-002");
        request.setCameraName("测试设备2");
        request.setStreamUrl("rtsp://example/stream2");

        when(cameraDeviceService.save(any(CameraDevice.class))).thenAnswer(invocation -> {
            CameraDevice cameraDevice = invocation.getArgument(0);
            cameraDevice.setId(1002L);
            return true;
        });

        controller.addCameraDevice(request);

        ArgumentCaptor<CameraDevice> captor = ArgumentCaptor.forClass(CameraDevice.class);
        org.mockito.Mockito.verify(cameraDeviceService).save(captor.capture());
        assertEquals(12L, captor.getValue().getZone_id());
    }
}
