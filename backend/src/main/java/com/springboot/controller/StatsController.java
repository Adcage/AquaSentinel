package com.springboot.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.model.dto.stats.StatsExportRequest;
import com.springboot.model.dto.stats.StatsTrendRequest;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.StatsSnapshot;
import com.springboot.service.AlertRecordService;
import com.springboot.service.StatsAggregationService;
import com.springboot.service.StatsSnapshotService;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatsController {

    @Value("${spring.file.upload.path:src/main/resources/files/}")
    private String uploadPath;

    @Resource private StatsSnapshotService statsSnapshotService;

    @Resource private StatsAggregationService statsAggregationService;

    @Resource private AlertRecordService alertRecordService;

    @GetMapping("/overview")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getOverview(
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = null;
        if (StringUtils.isNotBlank(date)) {
            targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return ResultUtils.success(statsAggregationService.getOverview(venueId, targetDate));
    }

    @PostMapping("/trend")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> trend(
            @RequestBody(required = false) StatsTrendRequest request) {
        QueryWrapper<StatsSnapshot> queryWrapper = new QueryWrapper<>();
        if (request != null) {
            queryWrapper.eq(request.getVenueId() != null, "venue_id", request.getVenueId());
            queryWrapper.eq(
                    StringUtils.isNotBlank(request.getMetricType()),
                    "metric_type",
                    request.getMetricType());
            queryWrapper.eq(
                    StringUtils.isNotBlank(request.getMetricKey()),
                    "metric_key",
                    request.getMetricKey());
            queryWrapper.eq(
                    StringUtils.isNotBlank(request.getGranularity()),
                    "granularity",
                    request.getGranularity());
            queryWrapper.ge(
                    request.getStartDate() != null, "snapshot_date", request.getStartDate());
            queryWrapper.le(request.getEndDate() != null, "snapshot_date", request.getEndDate());
        }
        queryWrapper.orderByAsc("snapshot_date", "snapshot_hour");
        queryWrapper.last("limit 200");
        List<StatsSnapshot> snapshotList = statsSnapshotService.list(queryWrapper);
        List<Map<String, Object>> points = new ArrayList<>();
        for (StatsSnapshot snapshot : snapshotList) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", snapshot.getSnapshot_date());
            point.put("hour", snapshot.getSnapshot_hour());
            point.put("value", snapshot.getMetric_value());
            point.put("metricKey", snapshot.getMetric_key());
            points.add(point);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("points", points);
        data.put("count", points.size());
        data.put("generatedAt", new Date());
        return ResultUtils.success(data);
    }

    @GetMapping("/ranking")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> ranking(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer limit) {
        LocalDate start =
                StringUtils.isBlank(startDate)
                        ? null
                        : LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end =
                StringUtils.isBlank(endDate)
                        ? null
                        : LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        List<Map<String, Object>> ranking = statsAggregationService.getRanking(start, end, limit);
        Map<String, Object> data = new HashMap<>();
        data.put("items", ranking);
        data.put("count", ranking.size());
        data.put("generatedAt", new Date());
        return ResultUtils.success(data);
    }

    @PostMapping("/export/excel")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> exportExcel(
            @RequestBody(required = false) StatsExportRequest request) {
        return ResultUtils.success(buildExportResult("excel", request));
    }

    @PostMapping("/export/csv")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> exportCsv(
            @RequestBody(required = false) StatsExportRequest request) {
        return ResultUtils.success(buildExportResult("csv", request));
    }

    private Map<String, Object> buildExportResult(String format, StatsExportRequest request) {
        List<AlertRecord> records = listExportRecords(request);
        String fileName =
                String.format(
                        "stats_export_%d.%s",
                        System.currentTimeMillis(), "excel".equals(format) ? "xlsx" : "csv");
        String downloadPath = writeExportFile(fileName, format, records);
        Map<String, Object> data = new HashMap<>();
        data.put("format", format);
        data.put("status", "READY");
        data.put("downloadUrl", downloadPath);
        data.put("fileName", fileName);
        data.put("recordCount", records.size());
        data.put("requestedAt", new Date());
        data.put("filters", request);
        return data;
    }

    private List<AlertRecord> listExportRecords(StatsExportRequest request) {
        QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
        if (request != null) {
            queryWrapper.eq(request.getVenueId() != null, "venue_id", request.getVenueId());
            String alertType = normalizeAlertType(request.getMetricType());
            queryWrapper.eq(StringUtils.isNotBlank(alertType), "alert_type", alertType);
            queryWrapper.ge(request.getStartDate() != null, "created_at", request.getStartDate());
            queryWrapper.le(request.getEndDate() != null, "created_at", request.getEndDate());
        }
        queryWrapper.orderByDesc("created_at", "id");
        queryWrapper.last("limit 5000");
        return alertRecordService.list(queryWrapper);
    }

    private String writeExportFile(String fileName, String format, List<AlertRecord> records) {
        try {
            Path exportDir = Paths.get(uploadPath, "exports", "stats");
            Files.createDirectories(exportDir);
            Path outputPath = exportDir.resolve(fileName);
            if ("excel".equals(format)) {
                List<AlertExportRow> rows = new ArrayList<>();
                for (AlertRecord record : records) {
                    rows.add(new AlertExportRow(record));
                }
                EasyExcel.write(outputPath.toFile(), AlertExportRow.class)
                        .sheet("alerts")
                        .doWrite(rows);
            } else {
                writeCsv(outputPath, records);
            }
            return "/files/exports/stats/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("统计导出失败: " + e.getMessage(), e);
        }
    }

    private void writeCsv(Path outputPath, List<AlertRecord> records) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(
                    "报警ID,报警编码,事件ID,摄像头ID,场馆ID,救生员ID,报警类型,报警状态,紧急联系人,紧急联系电话,事发位置,视频流地址,检测结果,推送APP,推送PC,首次推送时间,处理完成时间,创建时间,更新时间");
            writer.newLine();
            for (AlertRecord record : records) {
                writer.write(
                        String.format(
                                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                record.getId(),
                                safeText(record.getAlert_uid()),
                                record.getEvent_id(),
                                record.getCamera_id(),
                                record.getVenue_id(),
                                record.getLifeguard_id(),
                                safeText(record.getAlert_type()),
                                safeText(record.getAlert_status()),
                                safeText(record.getEmergency_contact_name()),
                                safeText(record.getEmergency_contact_phone()),
                                safeText(record.getIncident_location()),
                                safeText(record.getVideo_stream_url()),
                                safeText(record.getDetection_result()),
                                record.getPushed_to_app(),
                                record.getPushed_to_pc(),
                                record.getFirst_push_time(),
                                record.getResolved_time(),
                                record.getCreated_at(),
                                record.getUpdated_at()));
                writer.newLine();
            }
        }
    }

    private String normalizeAlertType(String metricType) {
        if (StringUtils.isBlank(metricType) || "ALERT".equalsIgnoreCase(metricType)) {
            return null;
        }
        String normalized = metricType.trim().toUpperCase();
        if ("DROWNING".equals(normalized)) {
            return "DROWING";
        }
        if ("OVER_CAPACITY".equals(normalized)) {
            return "OVERCROWD";
        }
        if ("CROSS_BORDER".equals(normalized)) {
            return "OFF_POST";
        }
        return normalized;
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public static class AlertExportRow {

        @ExcelProperty("报警ID")
        private Long id;

        @ExcelProperty("报警编码")
        private String alertUid;

        @ExcelProperty("事件ID")
        private Long eventId;

        @ExcelProperty("摄像头ID")
        private Long cameraId;

        @ExcelProperty("场馆ID")
        private Long venueId;

        @ExcelProperty("救生员ID")
        private Long lifeguardId;

        @ExcelProperty("报警类型")
        private String alertType;

        @ExcelProperty("报警状态")
        private String alertStatus;

        @ExcelProperty("紧急联系人")
        private String emergencyContactName;

        @ExcelProperty("紧急联系电话")
        private String emergencyContactPhone;

        @ExcelProperty("事发位置")
        private String incidentLocation;

        @ExcelProperty("视频流地址")
        private String videoStreamUrl;

        @ExcelProperty("检测结果")
        private String detectionResult;

        @ExcelProperty("推送APP")
        private Integer pushedToApp;

        @ExcelProperty("推送PC")
        private Integer pushedToPc;

        @ExcelProperty("首次推送时间")
        private Date firstPushTime;

        @ExcelProperty("处理完成时间")
        private Date resolvedTime;

        @ExcelProperty("创建时间")
        private Date createdAt;

        @ExcelProperty("更新时间")
        private Date updatedAt;

        public AlertExportRow() {}

        public AlertExportRow(AlertRecord record) {
            this.id = record.getId();
            this.alertUid = record.getAlert_uid();
            this.eventId = record.getEvent_id();
            this.cameraId = record.getCamera_id();
            this.venueId = record.getVenue_id();
            this.lifeguardId = record.getLifeguard_id();
            this.alertType = record.getAlert_type();
            this.alertStatus = record.getAlert_status();
            this.emergencyContactName = record.getEmergency_contact_name();
            this.emergencyContactPhone = record.getEmergency_contact_phone();
            this.incidentLocation = record.getIncident_location();
            this.videoStreamUrl = record.getVideo_stream_url();
            this.detectionResult = record.getDetection_result();
            this.pushedToApp = record.getPushed_to_app();
            this.pushedToPc = record.getPushed_to_pc();
            this.firstPushTime = record.getFirst_push_time();
            this.resolvedTime = record.getResolved_time();
            this.createdAt = record.getCreated_at();
            this.updatedAt = record.getUpdated_at();
        }

        public Long getId() {
            return id;
        }

        public String getAlertUid() {
            return alertUid;
        }

        public Long getEventId() {
            return eventId;
        }

        public Long getCameraId() {
            return cameraId;
        }

        public Long getVenueId() {
            return venueId;
        }

        public Long getLifeguardId() {
            return lifeguardId;
        }

        public String getAlertType() {
            return alertType;
        }

        public String getAlertStatus() {
            return alertStatus;
        }

        public String getEmergencyContactName() {
            return emergencyContactName;
        }

        public String getEmergencyContactPhone() {
            return emergencyContactPhone;
        }

        public String getIncidentLocation() {
            return incidentLocation;
        }

        public String getVideoStreamUrl() {
            return videoStreamUrl;
        }

        public String getDetectionResult() {
            return detectionResult;
        }

        public Integer getPushedToApp() {
            return pushedToApp;
        }

        public Integer getPushedToPc() {
            return pushedToPc;
        }

        public Date getFirstPushTime() {
            return firstPushTime;
        }

        public Date getResolvedTime() {
            return resolvedTime;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getUpdatedAt() {
            return updatedAt;
        }
    }
}
