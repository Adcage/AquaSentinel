package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import com.springboot.common.BaseResponse;
import com.springboot.model.dto.cameradevice.CameraDeviceBatchDisableRequest;
import com.springboot.model.vo.BatchOperateResultVO;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.CameraDeviceService;
import com.springboot.websocket.AlertWsPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CameraDeviceControllerBatchDisableTest {

    @Mock private CameraDeviceService cameraDeviceService;

    @Mock private AlertWsPublisher alertWsPublisher;

    @Mock private AiStreamTaskService aiStreamTaskService;

    private CameraDeviceController controller;

    @BeforeEach
    void setUp() {
        controller = new CameraDeviceController();
        ReflectionTestUtils.setField(controller, "cameraDeviceService", cameraDeviceService);
        ReflectionTestUtils.setField(controller, "alertWsPublisher", alertWsPublisher);
        ReflectionTestUtils.setField(controller, "aiStreamTaskService", aiStreamTaskService);
    }

    @Test
    void batchDisableShouldReturnDetailResult() {
        CameraDeviceBatchDisableRequest request = new CameraDeviceBatchDisableRequest();
        request.setCameraIds(Arrays.asList(1L, 2L, 3L));

        BatchOperateResultVO expected = new BatchOperateResultVO();
        expected.setSuccessIds(Arrays.asList(1L, 3L));
        expected.setSuccessCount(2);
        expected.setFailedCount(1);
        BatchOperateResultVO.FailedItem failed = new BatchOperateResultVO.FailedItem();
        failed.setId(2L);
        failed.setReason("设备不存在");
        expected.setFailed(Collections.singletonList(failed));

        when(cameraDeviceService.batchDisableCameraDevices(request.getCameraIds()))
                .thenReturn(expected);

        BaseResponse<BatchOperateResultVO> response = controller.batchDisableCameraDevices(request);

        assertEquals(0, response.getCode());
        assertEquals(2, response.getData().getSuccessCount());
        assertEquals(1, response.getData().getFailedCount());
        assertEquals("设备不存在", response.getData().getFailed().get(0).getReason());
    }
}
