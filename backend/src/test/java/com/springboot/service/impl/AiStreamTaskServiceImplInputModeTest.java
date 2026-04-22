package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.springboot.config.AppAiEngineProperties;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.monitor.StartMonitorTaskRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.CameraDevice;
import com.springboot.service.AiEngineClient;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.SystemNoticeConfigService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiStreamTaskServiceImplInputModeTest {

    @Mock
    private CameraDeviceService cameraDeviceService;

    @Mock
    private AiEngineClient aiEngineClient;

    @Mock
    private SystemNoticeConfigService systemNoticeConfigService;

    @Test
    void startTaskShouldUseSourceStreamWhenModeIsSource() {
        AppAiEngineProperties properties = buildAiProperties("source");
        AiStreamTaskServiceImpl service = buildService(properties);

        StartMonitorTaskRequest request = new StartMonitorTaskRequest();
        request.setCameraId(1001L);
        request.setTaskCode("TASK_CAM_1001_1");

        service.startTask(request);

        verify(aiEngineClient).startTask(
                eq("TASK_CAM_1001_1"),
                eq("CAM-1001"),
                eq("rtsp://cam/live"),
                eq(200),
                eq("http://127.0.0.1:8300/api/streams/cameras/1001/preview"),
                eq(3.0));
    }

    @Test
    void startTaskShouldUseProxyStreamWhenModeIsProxy() {
        AppAiEngineProperties properties = buildAiProperties("proxy");
        AiStreamTaskServiceImpl service = buildService(properties);

        StartMonitorTaskRequest request = new StartMonitorTaskRequest();
        request.setCameraId(1001L);
        request.setTaskCode("TASK_CAM_1001_2");

        AiStreamTask result = service.startTask(request);

        assertNotNull(result);
        verify(aiEngineClient).startTask(
                eq("TASK_CAM_1001_2"),
                eq("CAM-1001"),
                eq("http://127.0.0.1:8300/api/internal/streams/cameras/1001/preview"),
                eq(200),
                eq("http://127.0.0.1:8300/api/streams/cameras/1001/preview"),
                eq(3.0));
    }

    @Test
    void startTaskShouldRejectDeletedCamera() {
        AppAiEngineProperties properties = buildAiProperties("source");
        AiStreamTaskServiceImpl service = new AiStreamTaskServiceImpl(
                cameraDeviceService,
                aiEngineClient,
                properties,
                systemNoticeConfigService);

        CameraDevice deleted = new CameraDevice();
        deleted.setId(1001L);
        deleted.setCamera_code("CAM-1001");
        deleted.setStream_url("rtsp://cam/live");
        deleted.setEnabled(1);
        deleted.setIs_delete(1);
        when(cameraDeviceService.getById(1001L)).thenReturn(deleted);

        StartMonitorTaskRequest request = new StartMonitorTaskRequest();
        request.setCameraId(1001L);
        request.setTaskCode("TASK_CAM_1001_3");

        assertThrows(BusinessException.class, () -> service.startTask(request));
    }

    @Test
    void stopTaskShouldIgnoreEngine404AndMarkStopped() {
        AppAiEngineProperties properties = buildAiProperties("source");
        AiStreamTaskServiceImpl service = org.mockito.Mockito.spy(
                new AiStreamTaskServiceImpl(cameraDeviceService, aiEngineClient, properties, systemNoticeConfigService));

        AiStreamTask storedTask = new AiStreamTask();
        storedTask.setId(1L);
        storedTask.setTask_code("TASK_CAM_1001_1");
        doReturn(storedTask).when(service).getTaskByCode("TASK_CAM_1001_1");
        doReturn(true).when(service).update(any(UpdateWrapper.class));

        when(aiEngineClient.stopTask("TASK_CAM_1001_1"))
                .thenThrow(new BusinessException(ErrorCode.SYSTEM_ERROR, "AI引擎调用失败: HTTP 404"));

        boolean stopped = service.stopTask("TASK_CAM_1001_1");

        assertTrue(stopped);
    }

    private AiStreamTaskServiceImpl buildService(AppAiEngineProperties properties) {
        AiStreamTaskServiceImpl service = org.mockito.Mockito.spy(
                new AiStreamTaskServiceImpl(cameraDeviceService, aiEngineClient, properties, systemNoticeConfigService));

        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(1001L);
        cameraDevice.setCamera_code("CAM-1001");
        cameraDevice.setStream_url("rtsp://cam/live");
        cameraDevice.setProtocol("RTSP");
        cameraDevice.setVenue_id(1L);
        cameraDevice.setEnabled(1);
        cameraDevice.setIs_delete(0);
        when(cameraDeviceService.getById(1001L)).thenReturn(cameraDevice);

        doReturn(null).when(service).getOne(any(QueryWrapper.class));
        doNothing().when(service).validAiStreamTask(any(AiStreamTask.class), eq(true));
        doReturn(true).when(service).saveOrUpdate(any(AiStreamTask.class));
        doReturn(true).when(service).update(any(UpdateWrapper.class));

        AiStreamTask storedTask = new AiStreamTask();
        storedTask.setTask_code("TASK_CAM_1001_1");
        storedTask.setCamera_id(1001L);
        doReturn(storedTask).when(service).getTaskByCode(anyString());

        when(systemNoticeConfigService.getDrowningAlertThresholdSec()).thenReturn(3);

        when(aiEngineClient.startTask(anyString(), anyString(), anyString(), any(), anyString(), any()))
                .thenReturn(Map.of("status", "RUNNING"));
        return service;
    }

    private AppAiEngineProperties buildAiProperties(String inputMode) {
        AppAiEngineProperties properties = new AppAiEngineProperties();
        properties.setInputStreamMode(inputMode);
        properties.setProxyBaseUrl("http://127.0.0.1:8300/api");
        properties.setInternalPreviewPathTemplate("/internal/streams/cameras/{cameraId}/preview");
        properties.setDisplayPreviewPathTemplate("/streams/cameras/{cameraId}/preview");
        return properties;
    }
}
