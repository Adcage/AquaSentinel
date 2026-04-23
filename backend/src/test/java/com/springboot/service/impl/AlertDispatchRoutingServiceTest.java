package com.springboot.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.entity.VenueZone;
import com.springboot.service.AlertRecordService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.LifeguardLocationLogService;
import com.springboot.service.LifeguardService;
import com.springboot.service.VenueZoneService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertDispatchRoutingServiceTest {

    @Mock private LifeguardService lifeguardService;

    @Mock private LifeguardLocationLogService lifeguardLocationLogService;

    @Mock private CameraDeviceService cameraDeviceService;

    @Mock private VenueZoneService venueZoneService;

    @Mock private AlertRecordService alertRecordService;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AlertDispatchRoutingService alertDispatchRoutingService;

    @Test
    void resolveAssigneeShouldPreferLifeguardInsideCameraZone() {
        Lifeguard guardInZone = buildLifeguard(9001L, 50001L, 2001L);
        Lifeguard guardOutZone = buildLifeguard(9002L, 50002L, 2001L);
        when(lifeguardService.list(org.mockito.ArgumentMatchers.<QueryWrapper<Lifeguard>>any()))
                .thenReturn(List.of(guardInZone, guardOutZone));

        CameraDevice camera = new CameraDevice();
        camera.setId(3001L);
        camera.setZone_id(7001L);
        when(cameraDeviceService.getById(3001L)).thenReturn(camera);

        VenueZone zone = new VenueZone();
        zone.setId(7001L);
        zone.setGeo_json(
                "{\"type\":\"Polygon\",\"coordinates\":[[[120.0,30.0],[120.0,31.0],[121.0,31.0],[121.0,30.0],[120.0,30.0]]]}");
        when(venueZoneService.getById(7001L)).thenReturn(zone);

        when(lifeguardLocationLogService.recentLocations(9001L, 1))
                .thenReturn(List.of(buildLocation(9001L, 120.5, 30.5, 1)));
        when(lifeguardLocationLogService.recentLocations(9002L, 1))
                .thenReturn(List.of(buildLocation(9002L, 122.0, 30.5, 1)));
        when(alertRecordService.count(any())).thenReturn(0L, 0L);

        Lifeguard selected = alertDispatchRoutingService.resolveAssignee(2001L, 3001L);

        assertEquals(9001L, selected.getId());
    }

    @Test
    void resolveAssigneeShouldFallbackToLowerActiveAlertCount() {
        Lifeguard busyGuard = buildLifeguard(9101L, 51001L, 2001L);
        Lifeguard idleGuard = buildLifeguard(9102L, 51002L, 2001L);
        when(lifeguardService.list(org.mockito.ArgumentMatchers.<QueryWrapper<Lifeguard>>any()))
                .thenReturn(List.of(busyGuard, idleGuard));

        CameraDevice camera = new CameraDevice();
        camera.setId(3002L);
        camera.setZone_id(null);
        when(cameraDeviceService.getById(3002L)).thenReturn(camera);

        when(lifeguardLocationLogService.recentLocations(9101L, 1))
                .thenReturn(List.of(buildLocation(9101L, 120.1, 30.1, 1)));
        when(lifeguardLocationLogService.recentLocations(9102L, 1))
                .thenReturn(List.of(buildLocation(9102L, 120.2, 30.2, 1)));
        when(alertRecordService.count(any())).thenReturn(3L, 1L);

        Lifeguard selected = alertDispatchRoutingService.resolveAssignee(2001L, 3002L);

        assertEquals(9102L, selected.getId());
    }

    @Test
    void resolveAssigneeShouldReturnNullWhenNoOnDutyLifeguard() {
        when(lifeguardService.list(org.mockito.ArgumentMatchers.<QueryWrapper<Lifeguard>>any()))
                .thenReturn(List.of());

        Lifeguard selected = alertDispatchRoutingService.resolveAssignee(2001L, 3003L);

        assertNull(selected);
    }

    private Lifeguard buildLifeguard(long id, long userId, long venueId) {
        Lifeguard lifeguard = new Lifeguard();
        lifeguard.setId(id);
        lifeguard.setUser_id(userId);
        lifeguard.setVenue_id(venueId);
        lifeguard.setDuty_status("ON_DUTY");
        lifeguard.setAudit_status("APPROVED");
        return lifeguard;
    }

    private LifeguardLocationLog buildLocation(
            long lifeguardId, double longitude, double latitude, int inFence) {
        LifeguardLocationLog log = new LifeguardLocationLog();
        log.setLifeguard_id(lifeguardId);
        log.setLongitude(BigDecimal.valueOf(longitude));
        log.setLatitude(BigDecimal.valueOf(latitude));
        log.setIn_fence(inFence);
        log.setReported_at(new Date());
        return log;
    }
}
