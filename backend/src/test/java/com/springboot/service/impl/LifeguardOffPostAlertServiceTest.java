package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.LifeguardDutyLog;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.service.AlertRecordService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.LifeguardDutyLogService;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.service.LifeguardService;
import com.springboot.service.MonitoringEventService;
import com.springboot.service.SystemNoticeConfigService;
import com.springboot.websocket.AlertWsPublisher;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LifeguardOffPostAlertServiceTest {

    @Mock private LifeguardService lifeguardService;

    @Mock private LifeguardLocationLogService lifeguardLocationLogService;

    @Mock private LifeguardDutyLogService lifeguardDutyLogService;

    @Mock private CameraDeviceService cameraDeviceService;

    @Mock private MonitoringEventService monitoringEventService;

    @Mock private AlertRecordService alertRecordService;

    @Mock private AlertWsPublisher alertWsPublisher;

    @Mock private ObjectMapper objectMapper;

    @Mock private SystemNoticeConfigService systemNoticeConfigService;

    @InjectMocks private LifeguardOffPostAlertService lifeguardOffPostAlertService;

    private LifeguardLocationLog locationLog;
    private Lifeguard lifeguard;

    @BeforeEach
    void setUp() {
        locationLog = new LifeguardLocationLog();
        locationLog.setLifeguard_id(8011L);
        locationLog.setVenue_id(2001L);
        locationLog.setLongitude(new java.math.BigDecimal("121.480312"));
        locationLog.setLatitude(new java.math.BigDecimal("31.225341"));
        locationLog.setIn_fence(0);
        locationLog.setReported_at(new Date());

        lifeguard = new Lifeguard();
        lifeguard.setId(8011L);
        lifeguard.setVenue_id(2001L);
        lifeguard.setFull_name("测试救生员");
        lifeguard.setDuty_status("ON_DUTY");

        when(lifeguardService.getById(8011L)).thenReturn(lifeguard);
        when(systemNoticeConfigService.getOffDutyThresholdSec()).thenReturn(10);
    }

    @Test
    void checkAfterLocationReportShouldCreateAlertWhenConsecutiveOutFenceReached()
            throws Exception {
        LifeguardLocationLog oldOutFence = new LifeguardLocationLog();
        oldOutFence.setIn_fence(0);
        oldOutFence.setReported_at(new Date(System.currentTimeMillis() - 30_000));
        List<LifeguardLocationLog> recent = Arrays.asList(locationLog, oldOutFence);
        when(lifeguardLocationLogService.recentLocations(8011L, 100)).thenReturn(recent);
        when(lifeguardDutyLogService.list(any(QueryWrapper.class))).thenReturn(List.of());
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(5001L);
        cameraDevice.setVenue_id(2001L);
        when(cameraDeviceService.list(any(QueryWrapper.class))).thenReturn(List.of(cameraDevice));
        when(alertRecordService.list(any(QueryWrapper.class))).thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"k\":\"v\"}");
        when(monitoringEventService.save(any()))
                .thenAnswer(
                        invocation -> {
                            MonitoringEvent event = invocation.getArgument(0);
                            event.setId(91001L);
                            return true;
                        });
        when(alertRecordService.save(any()))
                .thenAnswer(
                        invocation -> {
                            AlertRecord record = invocation.getArgument(0);
                            record.setId(81001L);
                            return true;
                        });

        Map<String, Object> result =
                lifeguardOffPostAlertService.checkAfterLocationReport(locationLog);

        assertTrue((Boolean) result.get("offPostAlert"));
        assertTrue((Boolean) result.get("created"));
        assertFalse((Boolean) result.get("duplicate"));
        assertEquals(81001L, result.get("alertId"));
        verify(alertWsPublisher).publishAlertCreated(any(), any(), any());
    }

    @Test
    void checkAfterLocationReportShouldSkipAlertWhenLeaveReportStillValid() {
        lifeguard.setDuty_status("LEAVE");
        LifeguardDutyLog leaveLog = new LifeguardDutyLog();
        leaveLog.setAction_type("LEAVE_REPORT");
        leaveLog.setPlanned_return_at(new Date(System.currentTimeMillis() + 300_000));
        leaveLog.setActual_return_at(null);
        when(lifeguardDutyLogService.list(any(QueryWrapper.class))).thenReturn(List.of(leaveLog));

        Map<String, Object> result =
                lifeguardOffPostAlertService.checkAfterLocationReport(locationLog);

        assertFalse((Boolean) result.get("offPostAlert"));
        assertEquals("LEAVE_REPORTED", result.get("reason"));
        verify(monitoringEventService, never()).save(any());
        verify(alertRecordService, never()).save(any());
    }

    @Test
    void checkAfterLocationReportShouldNotCreateNewAlertWhenActiveAlertExists() {
        LifeguardLocationLog oldOutFence = new LifeguardLocationLog();
        oldOutFence.setIn_fence(0);
        oldOutFence.setReported_at(new Date(System.currentTimeMillis() - 30_000));
        when(lifeguardLocationLogService.recentLocations(8011L, 100))
                .thenReturn(List.of(locationLog, oldOutFence));
        when(lifeguardDutyLogService.list(any(QueryWrapper.class))).thenReturn(List.of());
        AlertRecord active = new AlertRecord();
        active.setId(99001L);
        active.setAlert_uid("ALERT-active-offpost");
        active.setAlert_status("PENDING");
        when(alertRecordService.list(any(QueryWrapper.class))).thenReturn(List.of(active));

        Map<String, Object> result =
                lifeguardOffPostAlertService.checkAfterLocationReport(locationLog);

        assertTrue((Boolean) result.get("offPostAlert"));
        assertFalse((Boolean) result.get("created"));
        assertTrue((Boolean) result.get("duplicate"));
        assertEquals(99001L, result.get("alertId"));
        verify(monitoringEventService, never()).save(any());
        verify(alertRecordService, never()).save(any());
        verify(alertWsPublisher, never()).publishAlertCreated(any(), any(), any());
    }

    @Test
    void checkAfterLocationReportShouldRespectConfiguredThreshold() {
        when(systemNoticeConfigService.getOffDutyThresholdSec()).thenReturn(60);

        LifeguardLocationLog oldOutFence = new LifeguardLocationLog();
        oldOutFence.setIn_fence(0);
        oldOutFence.setReported_at(new Date(System.currentTimeMillis() - 30_000));
        when(lifeguardLocationLogService.recentLocations(8011L, 100))
                .thenReturn(List.of(locationLog, oldOutFence));
        when(lifeguardDutyLogService.list(any(QueryWrapper.class))).thenReturn(List.of());

        Map<String, Object> result =
                lifeguardOffPostAlertService.checkAfterLocationReport(locationLog);

        assertFalse((Boolean) result.get("offPostAlert"));
        assertEquals("THRESHOLD_NOT_REACHED", result.get("reason"));
        assertEquals(60, result.get("threshold"));
        verify(monitoringEventService, never()).save(any());
        verify(alertRecordService, never()).save(any());
    }
}
