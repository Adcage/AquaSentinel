package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.data.DataAnalysisReportQueryRequest;
import com.springboot.model.dto.data.DataPreprocessQueryRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.EnvSensorSample;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.entity.StatsSnapshot;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.EnvSensorSampleService;
import com.springboot.service.MonitoringEventService;
import com.springboot.service.StatsSnapshotService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data")
public class DataPipelineController {

    private static final String METRIC_TYPE_ANALYSIS_REPORT = "ANALYSIS_REPORT";

    @Resource
    private CameraDeviceService cameraDeviceService;

    @Resource
    private AiStreamTaskService aiStreamTaskService;

    @Resource
    private EnvSensorSampleService envSensorSampleService;

    @Resource
    private MonitoringEventService monitoringEventService;

    @Resource
    private StatsSnapshotService statsSnapshotService;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/collect/status")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getCollectStatus(@RequestParam(required = false) Long cameraId,
                                                               @RequestParam(required = false) Long venueId) {
        if (cameraId == null && venueId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraId和venueId不能同时为空");
        }
        CameraDevice cameraDevice = null;
        Long targetVenueId = venueId;
        if (cameraId != null) {
            cameraDevice = cameraDeviceService.getById(cameraId);
            if (cameraDevice == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "摄像头不存在");
            }
            if (targetVenueId == null) {
                targetVenueId = cameraDevice.getVenue_id();
            }
        }

        List<Long> cameraIds = resolveCameraIds(cameraId, targetVenueId);
        String streamCollectStatus = "STOPPED";
        String streamTaskCode = null;
        Date streamUpdatedAt = null;
        if (!cameraIds.isEmpty()) {
            QueryWrapper<AiStreamTask> streamTaskQuery = new QueryWrapper<>();
            streamTaskQuery.in("camera_id", cameraIds);
            streamTaskQuery.orderByDesc("updated_at");
            streamTaskQuery.last("limit 1");
            AiStreamTask latestTask = aiStreamTaskService.getOne(streamTaskQuery);
            if (latestTask != null) {
                streamTaskCode = latestTask.getTask_code();
                streamUpdatedAt = latestTask.getUpdated_at();
                if ("RUNNING".equalsIgnoreCase(latestTask.getTask_status())
                        || "STARTING".equalsIgnoreCase(latestTask.getTask_status())) {
                    streamCollectStatus = "RUNNING";
                } else {
                    streamCollectStatus = "STOPPED";
                }
            }
        }

        QueryWrapper<EnvSensorSample> latestSensorQuery = new QueryWrapper<>();
        latestSensorQuery.eq(targetVenueId != null, "venue_id", targetVenueId);
        latestSensorQuery.orderByDesc("sample_time");
        latestSensorQuery.last("limit 1");
        EnvSensorSample latestSensor = envSensorSampleService.getOne(latestSensorQuery);

        String sensorCollectStatus = "STOPPED";
        Date latestSampleTime = null;
        String latestQuality = null;
        if (latestSensor != null) {
            latestSampleTime = latestSensor.getSample_time();
            latestQuality = latestSensor.getQuality_flag();
            long staleSeconds = 300;
            long nowEpochMillis = System.currentTimeMillis();
            long sampleEpochMillis = latestSampleTime == null ? 0L : latestSampleTime.getTime();
            sensorCollectStatus = (nowEpochMillis - sampleEpochMillis) <= staleSeconds * 1000L ? "RUNNING" : "STALE";
        }

        QueryWrapper<EnvSensorSample> sampleCountQuery = new QueryWrapper<>();
        sampleCountQuery.eq(targetVenueId != null, "venue_id", targetVenueId);
        sampleCountQuery.ge("sample_time", Date.from(Instant.now().minusSeconds(1800)));
        long sensorSamplesIn30Minutes = envSensorSampleService.count(sampleCountQuery);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cameraId", cameraId);
        data.put("venueId", targetVenueId);
        data.put("streamCollectStatus", streamCollectStatus);
        data.put("sensorCollectStatus", sensorCollectStatus);
        data.put("streamTaskCode", streamTaskCode);
        data.put("streamUpdatedAt", streamUpdatedAt);
        data.put("lastSensorSampleTime", latestSampleTime);
        data.put("latestSensorQuality", latestQuality);
        data.put("sensorSampleCount30m", sensorSamplesIn30Minutes);
        data.put("cameraDeviceStatus", cameraDevice == null ? null : cameraDevice.getDevice_status());
        data.put("cameraHealthStatus", cameraDevice == null ? null : cameraDevice.getHealth_status());
        return ResultUtils.success(data);
    }

    @PostMapping("/preprocess/list/page")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> listPreprocessByPage(@RequestBody(required = false) DataPreprocessQueryRequest request) {
        DataPreprocessQueryRequest queryRequest = request == null ? new DataPreprocessQueryRequest() : request;
        long current = Math.max(1, queryRequest.getCurrent());
        long pageSize = Math.min(Math.max(1, queryRequest.getPageSize()), 100);

        QueryWrapper<MonitoringEvent> pageQuery = buildPreprocessEventQuery(queryRequest);
        pageQuery.orderByDesc("event_time");
        Page<MonitoringEvent> eventPage = monitoringEventService.page(new Page<>(current, pageSize), pageQuery);

        QueryWrapper<MonitoringEvent> processedCountQuery = buildPreprocessEventQuery(queryRequest);
        processedCountQuery.isNotNull("bbox_json");
        long processedCount = monitoringEventService.count(processedCountQuery);

        QueryWrapper<MonitoringEvent> errorCountQuery = buildPreprocessEventQuery(queryRequest);
        errorCountQuery.isNull("bbox_json");
        long errorCount = monitoringEventService.count(errorCountQuery);

        List<Map<String, Object>> records = new ArrayList<>();
        for (MonitoringEvent event : eventPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", event.getId());
            item.put("eventUid", event.getEvent_uid());
            item.put("taskId", event.getTask_id());
            item.put("eventType", event.getEvent_type());
            item.put("riskLevel", event.getRisk_level());
            item.put("confidence", event.getConfidence());
            item.put("bboxJson", event.getBbox_json());
            item.put("processedAt", event.getEvent_time());
            boolean success = event.getBbox_json() != null;
            item.put("status", success ? "SUCCESS" : "FAILED");
            item.put("errorMessage", success ? null : "bbox缺失");
            records.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("current", eventPage.getCurrent());
        data.put("pageSize", eventPage.getSize());
        data.put("total", eventPage.getTotal());
        data.put("processedCount", processedCount);
        data.put("errorCount", errorCount);
        data.put("records", records);
        return ResultUtils.success(data);
    }

    @PostMapping("/analysis/report/list")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<List<Map<String, Object>>> listAnalysisReport(@RequestBody(required = false) DataAnalysisReportQueryRequest request) {
        DataAnalysisReportQueryRequest queryRequest = request == null ? new DataAnalysisReportQueryRequest() : request;
        Date startAt = queryRequest.getStartTime() == null
                ? Date.from(Instant.now().minusSeconds(7L * 24 * 60 * 60))
                : new Date(queryRequest.getStartTime());
        Date endAt = queryRequest.getEndTime() == null ? new Date() : new Date(queryRequest.getEndTime());
        if (!startAt.before(endAt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间范围不合法");
        }

        QueryWrapper<MonitoringEvent> eventQuery = new QueryWrapper<>();
        eventQuery.ge("event_time", startAt);
        eventQuery.le("event_time", endAt);
        if (StringUtils.isNotBlank(queryRequest.getType())) {
            eventQuery.eq("event_type", queryRequest.getType().trim());
        }
        if (queryRequest.getVenueId() != null) {
            List<Long> cameraIds = resolveCameraIds(null, queryRequest.getVenueId());
            if (cameraIds.isEmpty()) {
                List<Map<String, Object>> emptyReports = List.of(buildAnalysisReport(
                        queryRequest.getVenueId(),
                        startAt,
                        endAt,
                        queryRequest.getType(),
                        List.of(),
                        Map.of(),
                        Map.of(),
                        List.of()));
                return ResultUtils.success(emptyReports);
            }
            eventQuery.in("camera_id", cameraIds);
        }
        eventQuery.orderByDesc("event_time");
        List<MonitoringEvent> events = monitoringEventService.list(eventQuery);

        Map<String, Long> riskLevelCount = new LinkedHashMap<>();
        Map<String, Long> typeCount = new LinkedHashMap<>();
        Map<String, Long> locationCount = new HashMap<>();
        List<Map<String, Object>> heatmapData = new ArrayList<>();
        long highRiskCount = 0L;

        for (MonitoringEvent event : events) {
            String riskLevel = StringUtils.defaultIfBlank(event.getRisk_level(), "UNKNOWN");
            riskLevelCount.merge(riskLevel, 1L, Long::sum);
            String eventType = StringUtils.defaultIfBlank(event.getEvent_type(), "UNKNOWN");
            typeCount.merge(eventType, 1L, Long::sum);
            if ("HIGH".equalsIgnoreCase(riskLevel)) {
                highRiskCount++;
            }
            if (StringUtils.isNotBlank(event.getIncident_location())) {
                locationCount.merge(event.getIncident_location(), 1L, Long::sum);
            }
            Map<String, Double> center = extractBboxCenter(event.getBbox_json());
            if (center != null) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("x", center.get("x"));
                point.put("y", center.get("y"));
                point.put("weight", event.getConfidence());
                point.put("eventUid", event.getEvent_uid());
                point.put("targetId", event.getTarget_id());
                heatmapData.add(point);
            }
        }

        List<Map<String, Object>> topIncidentLocations = locationCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(entry -> {
                    Map<String, Object> locationItem = new LinkedHashMap<>();
                    locationItem.put("incidentLocation", entry.getKey());
                    locationItem.put("count", entry.getValue());
                    return locationItem;
                })
                .toList();

        persistAnalysisMetric(queryRequest.getVenueId(), "analysisTotal", events.size());
        persistAnalysisMetric(queryRequest.getVenueId(), "analysisHighRisk", highRiskCount);

        Map<String, Object> report = buildAnalysisReport(
                queryRequest.getVenueId(),
                startAt,
                endAt,
                queryRequest.getType(),
                events,
                riskLevelCount,
                typeCount,
                heatmapData);
        ((Map<String, Object>) report.get("patternSummary")).put("topIncidentLocations", topIncidentLocations);

        return ResultUtils.success(List.of(report));
    }

    private QueryWrapper<MonitoringEvent> buildPreprocessEventQuery(DataPreprocessQueryRequest request) {
        QueryWrapper<MonitoringEvent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(request.getTaskId() != null, "task_id", request.getTaskId());
        if (request.getVenueId() != null) {
            List<Long> cameraIds = resolveCameraIds(null, request.getVenueId());
            if (cameraIds.isEmpty()) {
                queryWrapper.eq("id", -1L);
                return queryWrapper;
            }
            queryWrapper.in("camera_id", cameraIds);
        }
        if (request.getStartTime() != null) {
            queryWrapper.ge("event_time", new Date(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            queryWrapper.le("event_time", new Date(request.getEndTime()));
        }
        if (StringUtils.isNotBlank(request.getStatus())) {
            if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
                queryWrapper.isNotNull("bbox_json");
            } else if ("FAILED".equalsIgnoreCase(request.getStatus())) {
                queryWrapper.isNull("bbox_json");
            }
        }
        return queryWrapper;
    }

    private List<Long> resolveCameraIds(Long cameraId, Long venueId) {
        if (cameraId != null) {
            return List.of(cameraId);
        }
        if (venueId == null) {
            return List.of();
        }
        QueryWrapper<CameraDevice> cameraQuery = new QueryWrapper<>();
        cameraQuery.eq("venue_id", venueId);
        cameraQuery.eq("is_delete", 0);
        List<CameraDevice> cameras = cameraDeviceService.list(cameraQuery);
        if (cameras.isEmpty()) {
            return List.of();
        }
        return cameras.stream().map(CameraDevice::getId).toList();
    }

    private Map<String, Object> buildAnalysisReport(Long venueId, Date startAt, Date endAt, String type,
                                                    List<MonitoringEvent> events,
                                                    Map<String, Long> riskLevelCount,
                                                    Map<String, Long> typeCount,
                                                    List<Map<String, Object>> heatmapData) {
        Map<String, Object> patternSummary = new LinkedHashMap<>();
        patternSummary.put("totalEvents", events.size());
        patternSummary.put("byRiskLevel", riskLevelCount);
        patternSummary.put("byEventType", typeCount);
        patternSummary.put("topIncidentLocations", List.of());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportId", "analysis_" + System.currentTimeMillis());
        report.put("venueId", venueId);
        report.put("startTime", startAt);
        report.put("endTime", endAt);
        report.put("type", StringUtils.defaultIfBlank(type, "ALL"));
        report.put("patternSummary", patternSummary);
        report.put("heatmapData", heatmapData);
        report.put("generatedAt", new Date());
        return report;
    }

    private Map<String, Double> extractBboxCenter(Object bboxJson) {
        if (bboxJson == null) {
            return null;
        }
        try {
            JsonNode node;
            if (bboxJson instanceof String text) {
                if (StringUtils.isBlank(text)) {
                    return null;
                }
                node = objectMapper.readTree(text);
            } else {
                node = objectMapper.valueToTree(bboxJson);
            }
            if (node == null || !node.isObject()) {
                return null;
            }
            double xMin = node.path("xMin").asDouble(Double.NaN);
            double yMin = node.path("yMin").asDouble(Double.NaN);
            double xMax = node.path("xMax").asDouble(Double.NaN);
            double yMax = node.path("yMax").asDouble(Double.NaN);
            if (Double.isNaN(xMin) || Double.isNaN(yMin) || Double.isNaN(xMax) || Double.isNaN(yMax)) {
                return null;
            }
            Map<String, Double> center = new HashMap<>();
            center.put("x", (xMin + xMax) / 2.0d);
            center.put("y", (yMin + yMax) / 2.0d);
            return center;
        } catch (Exception e) {
            return null;
        }
    }

    private void persistAnalysisMetric(Long venueId, String metricKey, long metricValue) {
        LocalDate nowDate = LocalDate.now();
        Date snapshotDate = Date.from(nowDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        QueryWrapper<StatsSnapshot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("granularity", "DAY");
        queryWrapper.eq("snapshot_date", snapshotDate);
        queryWrapper.isNull("snapshot_hour");
        queryWrapper.eq("metric_type", METRIC_TYPE_ANALYSIS_REPORT);
        queryWrapper.eq("metric_key", metricKey);
        if (venueId == null) {
            queryWrapper.isNull("venue_id");
        } else {
            queryWrapper.eq("venue_id", venueId);
        }

        StatsSnapshot snapshot = statsSnapshotService.getOne(queryWrapper);
        if (snapshot == null) {
            snapshot = new StatsSnapshot();
            snapshot.setGranularity("DAY");
            snapshot.setSnapshot_date(snapshotDate);
            snapshot.setSnapshot_hour(null);
            snapshot.setVenue_id(venueId);
            snapshot.setMetric_type(METRIC_TYPE_ANALYSIS_REPORT);
            snapshot.setMetric_key(metricKey);
            snapshot.setMetric_value(BigDecimal.valueOf(metricValue));
            snapshot.setCreated_at(new Date());
            statsSnapshotService.save(snapshot);
            return;
        }
        snapshot.setMetric_value(BigDecimal.valueOf(metricValue));
        snapshot.setCreated_at(new Date());
        statsSnapshotService.updateById(snapshot);
    }
}
