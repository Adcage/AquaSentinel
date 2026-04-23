package com.springboot.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
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
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LifeguardOffPostAlertService {

    private static final int DEFAULT_OFF_DUTY_THRESHOLD_SEC = 60;

    private static final int RECENT_LOCATION_LIMIT = 100;

    @Resource private LifeguardService lifeguardService;

    @Resource private LifeguardLocationLogService lifeguardLocationLogService;

    @Resource private LifeguardDutyLogService lifeguardDutyLogService;

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private MonitoringEventService monitoringEventService;

    @Resource private AlertRecordService alertRecordService;

    @Resource private AlertWsPublisher alertWsPublisher;

    @Resource private ObjectMapper objectMapper;

    @Resource private SystemNoticeConfigService systemNoticeConfigService;

    @Value("${app.lifeguard.off-post-threshold-sec:60}")
    private int offPostThresholdSec;

    public Map<String, Object> checkAfterLocationReport(LifeguardLocationLog locationLog) {
        if (locationLog == null || locationLog.getLifeguard_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "lifeguardId不能为空");
        }
        Lifeguard lifeguard = lifeguardService.getById(locationLog.getLifeguard_id());
        if (lifeguard == null || Objects.equals(lifeguard.getIs_delete(), 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "救生员不存在");
        }
        Date now = locationLog.getReported_at() == null ? new Date() : locationLog.getReported_at();
        int thresholdSec = getOffDutyThresholdSec();
        Map<String, Object> result = buildBaseResult(lifeguard, locationLog, thresholdSec);

        if (!Objects.equals(locationLog.getIn_fence(), 0)) {
            result.put("reason", "IN_FENCE");
            return result;
        }

        if (!StringUtils.equalsAnyIgnoreCase(lifeguard.getDuty_status(), "ON_DUTY", "LEAVE")) {
            result.put("reason", "NOT_ON_DUTY");
            return result;
        }

        LeaveState leaveState = resolveLeaveState(lifeguard.getId(), now);
        if (StringUtils.equalsIgnoreCase(lifeguard.getDuty_status(), "LEAVE")
                && leaveState.withinApprovedWindow) {
            result.put("reason", "LEAVE_REPORTED");
            return result;
        }

        int outFenceDurationSec = calculateOutFenceDurationSec(lifeguard.getId(), now);
        result.put("outFenceDurationSec", outFenceDurationSec);
        result.put("threshold", thresholdSec);
        if (outFenceDurationSec < thresholdSec) {
            result.put("reason", "THRESHOLD_NOT_REACHED");
            return result;
        }

        AlertRecord activeAlert = findActiveOffPostAlert(lifeguard.getId());
        if (activeAlert != null) {
            result.put("offPostAlert", true);
            result.put("created", false);
            result.put("duplicate", true);
            result.put("alertId", activeAlert.getId());
            result.put("alertUid", activeAlert.getAlert_uid());
            result.put("reason", "ACTIVE_ALERT_EXISTS");
            return result;
        }

        CameraDevice cameraDevice = resolveVenueCamera(lifeguard.getVenue_id());
        if (cameraDevice == null) {
            result.put("reason", "CAMERA_NOT_FOUND");
            return result;
        }

        String offPostType = leaveState.leaveOverdue ? "LEAVE_TIMEOUT" : "FENCE_OUT";

        MonitoringEvent monitoringEvent =
                buildMonitoringEvent(
                        locationLog,
                        lifeguard,
                        cameraDevice,
                        offPostType,
                        outFenceDurationSec,
                        now,
                        thresholdSec);
        monitoringEventService.validMonitoringEvent(monitoringEvent, true);
        boolean eventSaved = monitoringEventService.save(monitoringEvent);
        ThrowUtils.throwIf(!eventSaved, ErrorCode.OPERATION_ERROR, "脱岗事件创建失败");

        AlertRecord alertRecord =
                buildAlertRecord(
                        locationLog, lifeguard, cameraDevice, monitoringEvent, offPostType, now);
        alertRecordService.validAlertRecord(alertRecord, true);
        boolean alertSaved = alertRecordService.save(alertRecord);
        ThrowUtils.throwIf(!alertSaved, ErrorCode.OPERATION_ERROR, "脱岗报警创建失败");

        Map<String, Object> wsData = new HashMap<>();
        wsData.put("alertId", alertRecord.getId());
        wsData.put("alertUid", alertRecord.getAlert_uid());
        wsData.put("eventId", monitoringEvent.getId());
        wsData.put("eventType", monitoringEvent.getEvent_type());
        wsData.put("riskLevel", monitoringEvent.getRisk_level());
        wsData.put("lifeguardId", lifeguard.getId());
        wsData.put("offPostType", offPostType);
        alertWsPublisher.publishAlertCreated(
                monitoringEvent.getEvent_uid(), alertRecord.getAlert_uid(), wsData);

        result.put("offPostAlert", true);
        result.put("created", true);
        result.put("duplicate", false);
        result.put("alertId", alertRecord.getId());
        result.put("alertUid", alertRecord.getAlert_uid());
        result.put("eventId", monitoringEvent.getId());
        result.put("offPostType", offPostType);
        result.put("reason", "ALERT_CREATED");
        return result;
    }

    public Map<String, Object> checkByLifeguard(Long lifeguardId) {
        if (lifeguardId == null || lifeguardId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "lifeguardId错误");
        }
        List<LifeguardLocationLog> locationLogs =
                lifeguardLocationLogService.recentLocations(lifeguardId, 1);
        LifeguardLocationLog latest = locationLogs.isEmpty() ? null : locationLogs.get(0);
        if (latest == null) {
            Lifeguard lifeguard = lifeguardService.getById(lifeguardId);
            if (lifeguard == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "救生员不存在");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("lifeguardId", lifeguardId);
            result.put("dutyStatus", lifeguard.getDuty_status());
            result.put("inFence", null);
            result.put("offPostAlert", false);
            result.put("reason", "NO_LOCATION");
            return result;
        }
        return checkAfterLocationReport(latest);
    }

    private MonitoringEvent buildMonitoringEvent(
            LifeguardLocationLog locationLog,
            Lifeguard lifeguard,
            CameraDevice cameraDevice,
            String offPostType,
            int outFenceDurationSec,
            Date now,
            int thresholdSec) {
        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setEvent_uid(
                "evt_offpost_" + lifeguard.getId() + "_" + System.currentTimeMillis());
        monitoringEvent.setCamera_id(cameraDevice.getId());
        monitoringEvent.setEvent_type("OFF_POST");
        monitoringEvent.setRisk_level("HIGH");
        monitoringEvent.setConfidence(new BigDecimal("0.9500"));
        monitoringEvent.setTarget_id("lifeguard-" + lifeguard.getId());
        monitoringEvent.setPosition_desc("lifeguardId=" + lifeguard.getId());
        monitoringEvent.setEmergency_contact_name(lifeguard.getFull_name());
        monitoringEvent.setEmergency_contact_phone(lifeguard.getPhone());
        monitoringEvent.setIncident_location(buildIncidentLocation(locationLog));
        monitoringEvent.setVideo_stream_url(cameraDevice.getStream_url());
        monitoringEvent.setEvent_time(now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("offPostType", offPostType);
        ext.put("outFenceDurationSec", outFenceDurationSec);
        ext.put("thresholdSec", thresholdSec);
        ext.put("reportSource", locationLog.getReport_source());
        ext.put("reportedAt", now.getTime());
        monitoringEvent.setExt_json(toJsonText(ext));
        monitoringEvent.setCreated_at(new Date());
        return monitoringEvent;
    }

    private AlertRecord buildAlertRecord(
            LifeguardLocationLog locationLog,
            Lifeguard lifeguard,
            CameraDevice cameraDevice,
            MonitoringEvent monitoringEvent,
            String offPostType,
            Date now) {
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setAlert_uid("ALERT-" + UUID.randomUUID().toString().replace("-", ""));
        alertRecord.setEvent_id(monitoringEvent.getId());
        alertRecord.setCamera_id(cameraDevice.getId());
        alertRecord.setVenue_id(cameraDevice.getVenue_id());
        alertRecord.setLifeguard_id(lifeguard.getId());
        alertRecord.setAlert_type("OFF_POST");
        alertRecord.setAlert_status("PENDING");
        alertRecord.setEmergency_contact_name(lifeguard.getFull_name());
        alertRecord.setEmergency_contact_phone(lifeguard.getPhone());
        alertRecord.setIncident_location(offPostType + ":" + buildIncidentLocation(locationLog));
        alertRecord.setVideo_stream_url(cameraDevice.getStream_url());
        alertRecord.setPushed_to_app(1);
        alertRecord.setPushed_to_pc(1);
        alertRecord.setFirst_push_time(now);
        alertRecord.setCreated_at(now);
        alertRecord.setUpdated_at(now);
        return alertRecord;
    }

    private String buildIncidentLocation(LifeguardLocationLog locationLog) {
        if (locationLog == null) {
            return "UNKNOWN";
        }
        String lng =
                locationLog.getLongitude() == null
                        ? "unknown"
                        : locationLog.getLongitude().toPlainString();
        String lat =
                locationLog.getLatitude() == null
                        ? "unknown"
                        : locationLog.getLatitude().toPlainString();
        return "lng=" + lng + ",lat=" + lat;
    }

    private Map<String, Object> buildBaseResult(
            Lifeguard lifeguard, LifeguardLocationLog locationLog, int thresholdSec) {
        Map<String, Object> result = new HashMap<>();
        result.put("lifeguardId", lifeguard.getId());
        result.put("dutyStatus", lifeguard.getDuty_status());
        result.put("inFence", locationLog.getIn_fence());
        result.put("offPostAlert", false);
        result.put("created", false);
        result.put("duplicate", false);
        result.put("threshold", thresholdSec);
        return result;
    }

    private int getOffDutyThresholdSec() {
        int configured = systemNoticeConfigService.getOffDutyThresholdSec();
        if (configured > 0) {
            return configured;
        }
        return Math.max(
                1, offPostThresholdSec > 0 ? offPostThresholdSec : DEFAULT_OFF_DUTY_THRESHOLD_SEC);
    }

    private int calculateOutFenceDurationSec(Long lifeguardId, Date now) {
        List<LifeguardLocationLog> recent =
                lifeguardLocationLogService.recentLocations(lifeguardId, RECENT_LOCATION_LIMIT);
        Date earliestOutFenceAt = null;
        for (LifeguardLocationLog locationLog : recent) {
            if (locationLog == null || !Objects.equals(locationLog.getIn_fence(), 0)) {
                break;
            }
            earliestOutFenceAt = locationLog.getReported_at();
        }
        if (earliestOutFenceAt == null || now == null) {
            return 0;
        }
        long durationMillis = now.getTime() - earliestOutFenceAt.getTime();
        if (durationMillis <= 0) {
            return 0;
        }
        return (int) (durationMillis / 1000L);
    }

    private AlertRecord findActiveOffPostAlert(Long lifeguardId) {
        QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lifeguard_id", lifeguardId);
        queryWrapper.eq("alert_type", "OFF_POST");
        queryWrapper.ne("alert_status", "DONE");
        queryWrapper.ne("alert_status", "FALSE_ALARM");
        queryWrapper.orderByDesc("created_at", "id");
        queryWrapper.last("limit 1");
        List<AlertRecord> records = alertRecordService.list(queryWrapper);
        return records.isEmpty() ? null : records.get(0);
    }

    private CameraDevice resolveVenueCamera(Long venueId) {
        if (venueId == null || venueId <= 0) {
            return null;
        }
        QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("venue_id", venueId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("enabled", 1);
        queryWrapper.orderByDesc("id");
        queryWrapper.last("limit 1");
        List<CameraDevice> enabledDevices = cameraDeviceService.list(queryWrapper);
        if (!enabledDevices.isEmpty()) {
            return enabledDevices.get(0);
        }

        QueryWrapper<CameraDevice> fallbackWrapper = new QueryWrapper<>();
        fallbackWrapper.eq("venue_id", venueId);
        fallbackWrapper.eq("is_delete", 0);
        fallbackWrapper.orderByDesc("id");
        fallbackWrapper.last("limit 1");
        List<CameraDevice> fallbackDevices = cameraDeviceService.list(fallbackWrapper);
        return fallbackDevices.isEmpty() ? null : fallbackDevices.get(0);
    }

    private LeaveState resolveLeaveState(Long lifeguardId, Date now) {
        QueryWrapper<LifeguardDutyLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("lifeguard_id", lifeguardId);
        queryWrapper.eq("action_type", "LEAVE_REPORT");
        queryWrapper.orderByDesc("created_at", "id");
        queryWrapper.last("limit 1");
        List<LifeguardDutyLog> dutyLogs = lifeguardDutyLogService.list(queryWrapper);
        if (dutyLogs.isEmpty()) {
            return new LeaveState(false, false);
        }
        LifeguardDutyLog leaveLog = dutyLogs.get(0);
        if (leaveLog.getActual_return_at() != null) {
            return new LeaveState(false, false);
        }
        if (leaveLog.getPlanned_return_at() == null) {
            return new LeaveState(true, false);
        }
        boolean overdue = leaveLog.getPlanned_return_at().before(now);
        return new LeaveState(!overdue, overdue);
    }

    private String toJsonText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static final class LeaveState {

        private final boolean withinApprovedWindow;

        private final boolean leaveOverdue;

        private LeaveState(boolean withinApprovedWindow, boolean leaveOverdue) {
            this.withinApprovedWindow = withinApprovedWindow;
            this.leaveOverdue = leaveOverdue;
        }
    }
}
