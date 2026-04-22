package com.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.entity.StatsSnapshot;
import com.springboot.model.entity.Venue;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StatsAggregationService {

    private static final String METRIC_TYPE_OVERVIEW = "OVERVIEW";

    @Resource
    private StatsSnapshotService statsSnapshotService;

    @Resource
    private AlertRecordService alertRecordService;

    @Resource
    private CameraDeviceService cameraDeviceService;

    @Resource
    private LifeguardService lifeguardService;

    @Resource
    private MonitoringEventService monitoringEventService;

    @Resource
    private VenueService venueService;

    @Scheduled(cron = "0 0 * * * ?")
    public void snapshotHourScheduled() {
        LocalDateTime targetHour = LocalDateTime.now().minusHours(1).withMinute(0).withSecond(0).withNano(0);
        aggregateAndSave("HOUR", targetHour.toLocalDate(), targetHour.getHour(), targetHour, targetHour.plusHours(1));
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void snapshotDayScheduled() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime start = targetDate.atStartOfDay();
        aggregateAndSave("DAY", targetDate, null, start, start.plusDays(1));
    }

    public Map<String, Object> getOverview(Long venueId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDate yesterday = targetDate.minusDays(1);

        // Fetch today's snapshots (if any)
        QueryWrapper<StatsSnapshot> todayWrapper = new QueryWrapper<>();
        todayWrapper.eq("granularity", "DAY");
        todayWrapper.eq("snapshot_date", toDate(targetDate.atStartOfDay()));
        todayWrapper.eq("metric_type", METRIC_TYPE_OVERVIEW);
        if (venueId == null) {
            todayWrapper.isNull("venue_id");
        } else {
            todayWrapper.eq("venue_id", venueId);
        }
        List<StatsSnapshot> todaySnapshots = statsSnapshotService.list(todayWrapper);

        // Fetch yesterday's snapshots
        QueryWrapper<StatsSnapshot> yesterdayWrapper = new QueryWrapper<>();
        yesterdayWrapper.eq("granularity", "DAY");
        yesterdayWrapper.eq("snapshot_date", toDate(yesterday.atStartOfDay()));
        yesterdayWrapper.eq("metric_type", METRIC_TYPE_OVERVIEW);
        if (venueId == null) {
            yesterdayWrapper.isNull("venue_id");
        } else {
            yesterdayWrapper.eq("venue_id", venueId);
        }
        List<StatsSnapshot> yesterdaySnapshots = statsSnapshotService.list(yesterdayWrapper);

        Map<String, Object> todayMetrics;
        if (LocalDate.now().equals(targetDate)) {
            todayMetrics = buildMetricMap(venueId, targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
        } else if (todaySnapshots.isEmpty()) {
            todayMetrics = buildMetricMap(venueId, targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
        } else {
            todayMetrics = new HashMap<>();
            todayMetrics.put("onlineDeviceCount", metricAsLong(todaySnapshots, "onlineDeviceCount"));
            todayMetrics.put("todayAlertCount", metricAsLong(todaySnapshots, "todayAlertCount"));
            todayMetrics.put("pendingAlertCount", metricAsLong(todaySnapshots, "pendingAlertCount"));
            todayMetrics.put("onDutyLifeguardCount", metricAsLong(todaySnapshots, "onDutyLifeguardCount"));
            todayMetrics.put("currentPoolHeadCount", metricAsLong(todaySnapshots, "currentPoolHeadCount"));
        }

        Map<String, Object> data = new HashMap<>(todayMetrics);
        data.put("onlineDeviceDiff", (long) todayMetrics.get("onlineDeviceCount") - metricAsLong(yesterdaySnapshots, "onlineDeviceCount"));
        data.put("todayAlertDiff", (long) todayMetrics.get("todayAlertCount") - metricAsLong(yesterdaySnapshots, "todayAlertCount"));
        data.put("generatedAt", new Date());
        return data;
    }

    public List<Map<String, Object>> getRanking(LocalDate startDate, LocalDate endDate, Integer limit) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(6) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        int topN = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);

        QueryWrapper<StatsSnapshot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("granularity", "DAY");
        queryWrapper.eq("metric_type", METRIC_TYPE_OVERVIEW);
        queryWrapper.eq("metric_key", "todayAlertCount");
        queryWrapper.ge("snapshot_date", toDate(start.atStartOfDay()));
        queryWrapper.le("snapshot_date", toDate(end.atStartOfDay()));
        queryWrapper.isNotNull("venue_id");
        List<StatsSnapshot> snapshots = statsSnapshotService.list(queryWrapper);

        Map<Long, BigDecimal> venueAlertCountMap = new HashMap<>();
        for (StatsSnapshot snapshot : snapshots) {
            Long venueId = snapshot.getVenue_id();
            if (venueId == null) {
                continue;
            }
            venueAlertCountMap.merge(venueId,
                    snapshot.getMetric_value() == null ? BigDecimal.ZERO : snapshot.getMetric_value(),
                    BigDecimal::add);
        }

        Map<Long, String> venueNameMap = venueService.list().stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getVenue_name, (a, b) -> a));
        List<Map<String, Object>> ranking = new ArrayList<>();
        venueAlertCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("venueId", entry.getKey());
                    item.put("venueName", venueNameMap.getOrDefault(entry.getKey(), ""));
                    item.put("alertCount", entry.getValue().longValue());
                    ranking.add(item);
                });
        return ranking;
    }

    private void aggregateAndSave(String granularity, LocalDate snapshotDate, Integer snapshotHour,
                                  LocalDateTime start, LocalDateTime end) {
        List<Venue> venues = venueService.list();
        for (Venue venue : venues) {
            persistOverviewMetrics(granularity, snapshotDate, snapshotHour, venue.getId(), start, end);
        }
        persistOverviewMetrics(granularity, snapshotDate, snapshotHour, null, start, end);
    }

    private void persistOverviewMetrics(String granularity, LocalDate snapshotDate, Integer snapshotHour,
                                        Long venueId, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> metrics = buildMetricMap(venueId, start, end);
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            if ("generatedAt".equals(entry.getKey())) {
                continue;
            }
            upsertMetric(granularity, snapshotDate, snapshotHour, venueId, entry.getKey(),
                    BigDecimal.valueOf(((Number) entry.getValue()).doubleValue()));
        }
    }

    private Map<String, Object> buildMetricMap(Long venueId, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> data = new HashMap<>();
        Date startDate = toDate(start);
        Date endDate = toDate(end);

        QueryWrapper<CameraDevice> cameraQuery = new QueryWrapper<>();
        cameraQuery.eq("enabled", 1);
        cameraQuery.eq("device_status", "ONLINE");
        cameraQuery.eq("is_delete", 0);
        cameraQuery.eq(venueId != null, "venue_id", venueId);
        long onlineDeviceCount = cameraDeviceService.count(cameraQuery);

        QueryWrapper<AlertRecord> alertRangeQuery = new QueryWrapper<>();
        alertRangeQuery.eq(venueId != null, "venue_id", venueId);
        alertRangeQuery.ge("created_at", startDate);
        alertRangeQuery.lt("created_at", endDate);
        long todayAlertCount = alertRecordService.count(alertRangeQuery);

        QueryWrapper<AlertRecord> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq(venueId != null, "venue_id", venueId);
        pendingQuery.eq("alert_status", "PENDING");
        long pendingAlertCount = alertRecordService.count(pendingQuery);

        QueryWrapper<Lifeguard> onDutyQuery = new QueryWrapper<>();
        onDutyQuery.eq(venueId != null, "venue_id", venueId);
        onDutyQuery.eq("duty_status", "ON_DUTY");
        onDutyQuery.eq("audit_status", "APPROVED");
        long onDutyLifeguardCount = lifeguardService.count(onDutyQuery);

        QueryWrapper<CameraDevice> activeCameraQuery = new QueryWrapper<>();
        activeCameraQuery.eq("enabled", 1);
        activeCameraQuery.eq("device_status", "ONLINE");
        activeCameraQuery.eq("is_delete", 0);
        activeCameraQuery.eq(venueId != null, "venue_id", venueId);
        List<CameraDevice> activeCameras = cameraDeviceService.list(activeCameraQuery);
        List<Long> activeCameraIds = activeCameras.stream().map(CameraDevice::getId).collect(Collectors.toList());
        if (activeCameraIds.isEmpty()) {
            data.put("onlineDeviceCount", onlineDeviceCount);
            data.put("todayAlertCount", todayAlertCount);
            data.put("pendingAlertCount", pendingAlertCount);
            data.put("onDutyLifeguardCount", onDutyLifeguardCount);
            data.put("currentPoolHeadCount", 0L);
            data.put("generatedAt", new Date());
            return data;
        }

        QueryWrapper<MonitoringEvent> poolQuery = new QueryWrapper<>();
        poolQuery.in("camera_id", activeCameraIds);
        poolQuery.ge("event_time", startDate);
        poolQuery.lt("event_time", endDate);
        poolQuery.orderByDesc("event_time");
        List<MonitoringEvent> events = monitoringEventService.list(poolQuery);
        long currentPoolHeadCount = 0L;
        Set<Long> countedCameraIds = new HashSet<>();
        for (MonitoringEvent event : events) {
            Long cameraId = event.getCamera_id();
            if (cameraId == null || countedCameraIds.contains(cameraId)) {
                continue;
            }
            countedCameraIds.add(cameraId);
            if (event.getPool_head_count() != null && event.getPool_head_count() > 0) {
                currentPoolHeadCount += event.getPool_head_count();
            }
        }

        data.put("onlineDeviceCount", onlineDeviceCount);
        data.put("todayAlertCount", todayAlertCount);
        data.put("pendingAlertCount", pendingAlertCount);
        data.put("onDutyLifeguardCount", onDutyLifeguardCount);
        data.put("currentPoolHeadCount", currentPoolHeadCount);
        data.put("generatedAt", new Date());
        return data;
    }

    private void upsertMetric(String granularity, LocalDate snapshotDate, Integer snapshotHour,
                              Long venueId, String metricKey, BigDecimal metricValue) {
        QueryWrapper<StatsSnapshot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("granularity", granularity);
        queryWrapper.eq("snapshot_date", toDate(snapshotDate.atStartOfDay()));
        queryWrapper.eq("metric_type", METRIC_TYPE_OVERVIEW);
        queryWrapper.eq("metric_key", metricKey);
        queryWrapper.eq(snapshotHour != null, "snapshot_hour", snapshotHour);
        queryWrapper.isNull(snapshotHour == null, "snapshot_hour");
        if (venueId == null) {
            queryWrapper.isNull("venue_id");
        } else {
            queryWrapper.eq("venue_id", venueId);
        }

        StatsSnapshot existed = statsSnapshotService.getOne(queryWrapper);
        if (existed == null) {
            StatsSnapshot snapshot = new StatsSnapshot();
            snapshot.setGranularity(granularity);
            snapshot.setSnapshot_date(toDate(snapshotDate.atStartOfDay()));
            snapshot.setSnapshot_hour(snapshotHour);
            snapshot.setVenue_id(venueId);
            snapshot.setMetric_type(METRIC_TYPE_OVERVIEW);
            snapshot.setMetric_key(metricKey);
            snapshot.setMetric_value(metricValue);
            snapshot.setCreated_at(new Date());
            statsSnapshotService.save(snapshot);
            return;
        }

        UpdateWrapper<StatsSnapshot> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", existed.getId());
        updateWrapper.set("metric_value", metricValue);
        updateWrapper.set("created_at", new Date());
        statsSnapshotService.update(updateWrapper);
    }

    private long metricAsLong(List<StatsSnapshot> snapshots, String metricKey) {
        for (StatsSnapshot snapshot : snapshots) {
            if (metricKey.equals(snapshot.getMetric_key())) {
                return snapshot.getMetric_value() == null ? 0L : snapshot.getMetric_value().longValue();
            }
        }
        return 0L;
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
