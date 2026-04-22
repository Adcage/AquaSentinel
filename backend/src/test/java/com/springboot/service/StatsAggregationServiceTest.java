package com.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.entity.StatsSnapshot;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsAggregationServiceTest {

    @Mock
    private StatsSnapshotService statsSnapshotService;

    @Mock
    private AlertRecordService alertRecordService;

    @Mock
    private CameraDeviceService cameraDeviceService;

    @Mock
    private LifeguardService lifeguardService;

    @Mock
    private MonitoringEventService monitoringEventService;

    @Mock
    private VenueService venueService;

    @InjectMocks
    private StatsAggregationService statsAggregationService;

    @Test
    void getOverviewShouldSumLatestHeadCountAcrossActiveCameras() {
        when(statsSnapshotService.list(any(QueryWrapper.class))).thenReturn(List.of());
        when(cameraDeviceService.count(any(QueryWrapper.class))).thenReturn(4L);
        when(alertRecordService.count(any(QueryWrapper.class))).thenReturn(1L, 0L);
        when(lifeguardService.count(any(QueryWrapper.class))).thenReturn(2L);
        when(cameraDeviceService.list(any(QueryWrapper.class))).thenReturn(List.of(
                camera(5001L),
                camera(5003L)
        ));
        when(monitoringEventService.list(any(QueryWrapper.class))).thenReturn(List.of(
                event(5001L, 3, new Date(1_000L)),
                event(5001L, 2, new Date(900L)),
                event(5003L, 1, new Date(950L))
        ));

        Map<String, Object> overview = statsAggregationService.getOverview(null, LocalDate.now());

        assertEquals(4L, ((Number) overview.get("onlineDeviceCount")).longValue());
        assertEquals(4L, ((Number) overview.get("currentPoolHeadCount")).longValue());
    }

    @Test
    void getOverviewShouldFilterSoftDeletedDevicesWhenCountingOnline() {
        when(statsSnapshotService.list(any(QueryWrapper.class))).thenReturn(List.of());
        when(cameraDeviceService.count(any(QueryWrapper.class))).thenReturn(0L);
        when(alertRecordService.count(any(QueryWrapper.class))).thenReturn(0L, 0L);
        when(lifeguardService.count(any(QueryWrapper.class))).thenReturn(0L);
        when(cameraDeviceService.list(any(QueryWrapper.class))).thenReturn(List.of());

        statsAggregationService.getOverview(null, LocalDate.now());

        ArgumentCaptor<QueryWrapper<CameraDevice>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(cameraDeviceService).count(captor.capture());
        String sqlSegment = String.valueOf(captor.getValue().getSqlSegment()).toLowerCase();
        assertTrue(sqlSegment.contains("is_delete"));
    }

    private static CameraDevice camera(Long id) {
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(id);
        return cameraDevice;
    }

    private static MonitoringEvent event(Long cameraId, Integer poolHeadCount, Date eventTime) {
        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setCamera_id(cameraId);
        monitoringEvent.setPool_head_count(poolHeadCount);
        monitoringEvent.setEvent_time(eventTime);
        return monitoringEvent;
    }
}
