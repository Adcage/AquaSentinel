package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.constant.RoleConstant;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.security.HmacSignatureVerifier;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.AlertRecordService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.LifeguardService;
import com.springboot.service.MonitoringEventService;
import com.springboot.service.impl.AlertDispatchRoutingService;
import com.springboot.service.impl.AlertPushService;
import com.springboot.websocket.AlertWsPublisher;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InternalAiCallbackControllerTest {

    @Mock
    private HmacSignatureVerifier hmacSignatureVerifier;

    @Mock
    private MonitoringEventService monitoringEventService;

    @Mock
    private AlertRecordService alertRecordService;

    @Mock
    private AiStreamTaskService aiStreamTaskService;

    @Mock
    private CameraDeviceService cameraDeviceService;

    @Mock
    private AlertWsPublisher alertWsPublisher;

    @Mock
    private AlertPushService alertPushService;

    @Mock
    private AlertDispatchRoutingService alertDispatchRoutingService;

    @Mock
    private LifeguardService lifeguardService;

    private InternalAiCallbackController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalAiCallbackController();
        ReflectionTestUtils.setField(controller, "hmacSignatureVerifier", hmacSignatureVerifier);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "monitoringEventService", monitoringEventService);
        ReflectionTestUtils.setField(controller, "alertRecordService", alertRecordService);
        ReflectionTestUtils.setField(controller, "aiStreamTaskService", aiStreamTaskService);
        ReflectionTestUtils.setField(controller, "cameraDeviceService", cameraDeviceService);
        ReflectionTestUtils.setField(controller, "alertWsPublisher", alertWsPublisher);
        ReflectionTestUtils.setField(controller, "alertPushService", alertPushService);
        ReflectionTestUtils.setField(controller, "alertDispatchRoutingService", alertDispatchRoutingService);
        ReflectionTestUtils.setField(controller, "lifeguardService", lifeguardService);
    }

    @Test
    void receiveEventShouldPushToAllOnDutyVenueLifeguards() {
        String requestBody = """
                {
                  "eventUid": "evt_test_5001_001",
                  "cameraId": 5001,
                  "venueId": 2001,
                  "eventType": "DROWNING"
                }
                """;

        when(hmacSignatureVerifier.verify(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(monitoringEventService.getOne(any())).thenReturn(null);
        when(alertRecordService.updateById(any(AlertRecord.class))).thenReturn(true);
        when(alertPushService.pushToApp(any(AlertRecord.class))).thenReturn(false);
        when(alertPushService.pushToPc(any(AlertRecord.class))).thenReturn(false);

        Lifeguard assignee = new Lifeguard();
        assignee.setId(9001L);
        assignee.setUser_id(10001L);
        when(alertDispatchRoutingService.resolveAssignee(2001L, 5001L)).thenReturn(assignee);

        Lifeguard venueLifeguard1 = new Lifeguard();
        venueLifeguard1.setId(9001L);
        venueLifeguard1.setUser_id(10001L);
        Lifeguard venueLifeguard2 = new Lifeguard();
        venueLifeguard2.setId(9002L);
        venueLifeguard2.setUser_id(10002L);
        when(lifeguardService.list(any(QueryWrapper.class))).thenReturn(List.of(venueLifeguard1, venueLifeguard2));

        controller.receiveEvent("k", "t", "s", requestBody);

        ArgumentCaptor<Set<Long>> userIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<String>> roleCodesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(alertWsPublisher).publishAlertCreated(
                anyString(),
                anyString(),
                any(),
                userIdsCaptor.capture(),
                roleCodesCaptor.capture());

        Set<Long> targetUserIds = userIdsCaptor.getValue();
        Set<String> targetRoleCodes = roleCodesCaptor.getValue();
        assertTrue(targetUserIds.contains(10001L));
        assertTrue(targetUserIds.contains(10002L));
        assertTrue(targetRoleCodes.contains(RoleConstant.SUPER_ADMIN));
        assertTrue(targetRoleCodes.contains(RoleConstant.VENUE_ADMIN));
    }
}
