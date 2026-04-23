package com.springboot.controller;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.metrics.event.AlertEventReceivedEvent;
import com.springboot.metrics.event.AlertProcessingCompletedEvent;
import com.springboot.model.dto.internalai.InternalAiEventRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.CameraDevice;
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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class InternalAiCallbackController {

    @Resource private HmacSignatureVerifier hmacSignatureVerifier;

    @Resource private ObjectMapper objectMapper;

    @Resource private MonitoringEventService monitoringEventService;

    @Resource private AlertRecordService alertRecordService;

    @Resource private AiStreamTaskService aiStreamTaskService;

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private AlertWsPublisher alertWsPublisher;

    @Resource private AlertPushService alertPushService;

    @Resource private AlertDispatchRoutingService alertDispatchRoutingService;

    @Resource private LifeguardService lifeguardService;

    @Resource
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @PostMapping("/events")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Map<String, Object>> receiveEvent(
            @RequestHeader("X-AI-Key") String key,
            @RequestHeader("X-AI-Timestamp") String timestamp,
            @RequestHeader("X-AI-Signature") String signature,
            @RequestBody String requestBody) {
        long startTime = System.currentTimeMillis();
        boolean verified = hmacSignatureVerifier.verify(key, timestamp, signature, requestBody);
        if (!verified) {
            applicationEventPublisher.publishEvent(
                    new AlertEventReceivedEvent(false, "HMAC_VERIFY_FAILED"));
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "回调签名校验失败");
        }
        InternalAiEventRequest request;
        try {
            request = objectMapper.readValue(requestBody, InternalAiEventRequest.class);
        } catch (Exception e) {
            applicationEventPublisher.publishEvent(
                    new AlertEventReceivedEvent(false, "DESERIALIZE_FAILED"));
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调报文格式错误");
        }
        if (request == null) {
            applicationEventPublisher.publishEvent(
                    new AlertEventReceivedEvent(false, "NULL_REQUEST"));
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调报文不能为空");
        }
        Long cameraId = request.getCameraId();
        if (cameraId == null && StringUtils.isNotBlank(request.getCameraCode())) {
            QueryWrapper<CameraDevice> cameraQuery = new QueryWrapper<>();
            cameraQuery.eq("camera_code", request.getCameraCode());
            CameraDevice cameraDevice = cameraDeviceService.getOne(cameraQuery);
            if (cameraDevice != null) {
                cameraId = cameraDevice.getId();
            }
        }
        String eventType =
                StringUtils.defaultIfBlank(request.getEventType(), request.getRiskType());
        if (StringUtils.isBlank(request.getEventUid())
                || cameraId == null
                || StringUtils.isBlank(eventType)) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "eventUid/cameraId(or cameraCode)/eventType(or riskType)不能为空");
        }

        QueryWrapper<MonitoringEvent> existsQuery = new QueryWrapper<>();
        existsQuery.eq("event_uid", request.getEventUid());
        MonitoringEvent existedEvent = monitoringEventService.getOne(existsQuery);
        Map<String, Object> data = new HashMap<>();
        data.put("eventUid", request.getEventUid());
        if (existedEvent != null) {
            data.put("duplicate", true);
            return ResultUtils.success(data);
        }

        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setEvent_uid(request.getEventUid());
        monitoringEvent.setCamera_id(cameraId);
        monitoringEvent.setEvent_type(eventType);
        monitoringEvent.setRisk_level(StringUtils.defaultIfBlank(request.getRiskLevel(), "MEDIUM"));
        monitoringEvent.setConfidence(request.getConfidence());
        monitoringEvent.setTarget_id(request.getTargetId());
        monitoringEvent.setPool_head_count(request.getPoolHeadCount());
        monitoringEvent.setBbox_json(toJsonText(request.getBboxJson()));
        monitoringEvent.setPosition_desc(request.getPositionDesc());
        monitoringEvent.setEmergency_contact_name(request.getEmergencyContactName());
        monitoringEvent.setEmergency_contact_phone(request.getEmergencyContactPhone());
        monitoringEvent.setIncident_location(request.getIncidentLocation());
        monitoringEvent.setVideo_stream_url(request.getVideoStreamUrl());
        Date eventTime = resolveEventTime(request);
        monitoringEvent.setEvent_time(eventTime);
        monitoringEvent.setExt_json(toJsonText(request.getExtJson()));
        monitoringEvent.setCreated_at(new Date());
        if (request.getTaskId() != null) {
            monitoringEvent.setTask_id(request.getTaskId());
        } else if (StringUtils.isNotBlank(request.getTaskCode())) {
            AiStreamTask aiStreamTask = aiStreamTaskService.getTaskByCode(request.getTaskCode());
            monitoringEvent.setTask_id(aiStreamTask.getId());
        }
        monitoringEventService.validMonitoringEvent(monitoringEvent, true);
        monitoringEventService.save(monitoringEvent);

        Long venueId = request.getVenueId();
        if (venueId == null) {
            CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
            if (cameraDevice != null) {
                venueId = cameraDevice.getVenue_id();
            }
        }
        if (venueId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "venueId不能为空");
        }

        String alertUid = "ALERT-" + UUID.randomUUID().toString().replace("-", "");
        Lifeguard assignee = alertDispatchRoutingService.resolveAssignee(venueId, cameraId);
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setAlert_uid(alertUid);
        alertRecord.setEvent_id(monitoringEvent.getId());
        alertRecord.setCamera_id(cameraId);
        alertRecord.setVenue_id(venueId);
        alertRecord.setLifeguard_id(assignee == null ? null : assignee.getId());
        alertRecord.setAlert_type(StringUtils.defaultIfBlank(request.getAlertType(), "DROWING"));
        alertRecord.setAlert_status("PENDING");
        alertRecord.setEmergency_contact_name(request.getEmergencyContactName());
        alertRecord.setEmergency_contact_phone(request.getEmergencyContactPhone());
        alertRecord.setIncident_location(request.getIncidentLocation());
        alertRecord.setVideo_stream_url(request.getVideoStreamUrl());
        alertRecord.setDetection_result(buildDetectionResult(request));
        alertRecord.setPushed_to_app(0);
        alertRecord.setPushed_to_pc(0);
        alertRecord.setCreated_at(new Date());
        alertRecord.setUpdated_at(new Date());
        alertRecordService.validAlertRecord(alertRecord, true);
        alertRecordService.save(alertRecord);

        boolean appPushed = alertPushService.pushToApp(alertRecord);
        boolean pcPushed = alertPushService.pushToPc(alertRecord);
        AlertRecord pushUpdate = new AlertRecord();
        pushUpdate.setId(alertRecord.getId());
        pushUpdate.setPushed_to_app(appPushed ? 1 : 0);
        pushUpdate.setPushed_to_pc(pcPushed ? 1 : 0);
        if (appPushed || pcPushed) {
            pushUpdate.setFirst_push_time(new Date());
        }
        pushUpdate.setUpdated_at(new Date());
        alertRecordService.updateById(pushUpdate);

        Map<String, Object> wsData = new HashMap<>();
        wsData.put("alertId", alertRecord.getId());
        wsData.put("alertUid", alertUid);
        wsData.put("eventId", monitoringEvent.getId());
        wsData.put("cameraId", cameraId);
        wsData.put("lifeguardId", assignee == null ? null : assignee.getId());
        wsData.put("eventType", monitoringEvent.getEvent_type());
        wsData.put("riskLevel", monitoringEvent.getRisk_level());
        wsData.put("targetId", request.getTargetId());
        wsData.put("confidence", request.getConfidence());
        wsData.put("detectTime", request.getDetectTime());
        wsData.put("videoStreamUrl", request.getVideoStreamUrl());
        wsData.put("riskPoint", extractMapValue(request.getExtJson(), "riskPoint"));
        wsData.put("riskScore", extractNumberValue(request.getExtJson(), "riskScore"));
        wsData.put("durationSec", extractNumberValue(request.getExtJson(), "durationSec"));
        wsData.put("triggered", extractBooleanValue(request.getExtJson(), "triggered"));
        wsData.put("ruleHits", extractListValue(request.getExtJson(), "ruleHits"));
        wsData.put("emergencyContactName", request.getEmergencyContactName());
        wsData.put("emergencyContactPhone", request.getEmergencyContactPhone());
        Set<Long> targetUserIds = resolveVenueTargetUserIds(venueId, assignee);
        if (!targetUserIds.isEmpty()) {
            Set<String> targetRoleCodes = new LinkedHashSet<>();
            targetRoleCodes.add(RoleConstant.SUPER_ADMIN);
            targetRoleCodes.add(RoleConstant.VENUE_ADMIN);
            alertWsPublisher.publishAlertCreated(
                    request.getEventUid(), alertUid, wsData, targetUserIds, targetRoleCodes);
        } else {
            alertWsPublisher.publishAlertCreated(request.getEventUid(), alertUid, wsData);
        }

        data.put("duplicate", false);
        data.put("eventId", monitoringEvent.getId());
        data.put("alertId", alertRecord.getId());
        data.put("alertUid", alertUid);

        applicationEventPublisher.publishEvent(new AlertEventReceivedEvent(true, eventType));
        applicationEventPublisher.publishEvent(
                new AlertProcessingCompletedEvent(
                        true, System.currentTimeMillis() - startTime, request.getEventUid()));

        return ResultUtils.success(data);
    }

    private Date resolveEventTime(InternalAiEventRequest request) {
        if (request.getEventTime() != null) {
            return request.getEventTime();
        }
        if (StringUtils.isBlank(request.getDetectTime())) {
            return new Date();
        }
        String detectTime = request.getDetectTime().trim();
        try {
            Instant instant = Instant.parse(detectTime);
            return Date.from(instant);
        } catch (Exception ignored) {
        }
        try {
            OffsetDateTime offsetDateTime =
                    OffsetDateTime.parse(detectTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Date.from(offsetDateTime.toInstant());
        } catch (Exception ignored) {
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(detectTime);
        } catch (Exception ignored) {
        }
        return new Date();
    }

    private Set<Long> resolveVenueTargetUserIds(Long venueId, Lifeguard assignee) {
        Set<Long> targetUserIds = new LinkedHashSet<>();
        if (assignee != null && assignee.getUser_id() != null && assignee.getUser_id() > 0) {
            targetUserIds.add(assignee.getUser_id());
        }
        if (venueId == null || venueId <= 0) {
            return targetUserIds;
        }
        QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("venue_id", venueId);
        queryWrapper.eq("audit_status", "APPROVED");
        queryWrapper.eq("duty_status", "ON_DUTY");
        queryWrapper.eq("is_delete", 0);
        List<Lifeguard> lifeguards = lifeguardService.list(queryWrapper);
        if (lifeguards == null || lifeguards.isEmpty()) {
            return targetUserIds;
        }
        for (Lifeguard item : lifeguards) {
            if (item == null || item.getUser_id() == null || item.getUser_id() <= 0) {
                continue;
            }
            targetUserIds.add(item.getUser_id());
        }
        return targetUserIds;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMapObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        try {
            return objectMapper.convertValue(value, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Number extractNumberValue(Object source, String key) {
        Map<String, Object> map = toMapObject(source);
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean extractBooleanValue(Object source, String key) {
        Map<String, Object> map = toMapObject(source);
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        if ("true".equals(text) || "1".equals(text) || "yes".equals(text) || "on".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "0".equals(text) || "no".equals(text) || "off".equals(text)) {
            return false;
        }
        return null;
    }

    private Object extractMapValue(Object source, String key) {
        Map<String, Object> map = toMapObject(source);
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    private Object extractListValue(Object source, String key) {
        Map<String, Object> map = toMapObject(source);
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof List<?>) {
            return value;
        }
        return null;
    }

    private String buildDetectionResult(InternalAiEventRequest request) {
        StringBuilder sb = new StringBuilder();

        // 添加位置描述
        if (StringUtils.isNotBlank(request.getPositionDesc())) {
            sb.append(request.getPositionDesc());
        }

        // 从 extJson 中提取规则命中信息
        Map<String, Object> extMap = toMapObject(request.getExtJson());
        if (extMap != null) {
            Object ruleHits = extMap.get("ruleHits");
            if (ruleHits instanceof List<?> hitsList && !hitsList.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("，");
                }
                sb.append("触发规则：");
                List<String> rules = new java.util.ArrayList<>();
                for (Object hit : hitsList) {
                    if (hit != null) {
                        rules.add(hit.toString());
                    }
                }
                sb.append(String.join("、", rules));
            }

            // 添加风险等级
            Object riskLevel = extMap.get("riskLevel");
            if (riskLevel != null) {
                if (sb.length() > 0) {
                    sb.append("，");
                }
                sb.append("风险等级：").append(riskLevel);
            }

            // 添加持续时间
            Object durationSec = extMap.get("durationSec");
            if (durationSec != null) {
                sb.append("，持续：").append(durationSec).append("秒");
            }
        }

        if (sb.length() == 0) {
            return "AI检测到异常行为，请及时查看视频流确认现场情况";
        }
        return sb.toString();
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
}
