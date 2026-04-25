package com.springboot.messaging.consumer;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.springboot.messaging.model.AlertEventMessage;
import com.springboot.messaging.serializer.MessageSerializer;
import com.springboot.metrics.event.AlertEventReceivedEvent;
import com.springboot.metrics.event.AlertProcessingCompletedEvent;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.Lifeguard;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.service.AlertRecordService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.LifeguardService;
import com.springboot.service.MonitoringEventService;
import com.springboot.service.impl.AlertDispatchRoutingService;
import com.springboot.service.impl.AlertPushService;
import com.springboot.websocket.AlertWsPublisher;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertRecordConsumer {

    @Resource private MonitoringEventService monitoringEventService;

    @Resource private AlertRecordService alertRecordService;

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private AlertWsPublisher alertWsPublisher;

    @Resource private AlertPushService alertPushService;

    @Resource private AlertDispatchRoutingService alertDispatchRoutingService;

    @Resource private LifeguardService lifeguardService;

    @Resource private MessageSerializer messageSerializer;

    @Resource private ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = "${app.messaging.rabbitmq.alert-record-queue:alert.record.queue}")
    public void onMessage(Message message) {
        long startTime = System.currentTimeMillis();
        String messageBody = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        AlertEventMessage eventMsg;
        try {
            eventMsg = messageSerializer.deserialize(messageBody, AlertEventMessage.class);
        } catch (Exception e) {
            log.error("报警消息反序列化失败: {}", messageBody, e);
            eventPublisher.publishEvent(new AlertEventReceivedEvent(false, "DESERIALIZE_FAILED"));
            return;
        }

        eventPublisher.publishEvent(new AlertEventReceivedEvent(true, eventMsg.getEventType()));

        try {
            processAlertEvent(eventMsg);
            long latency = System.currentTimeMillis() - startTime;
            eventPublisher.publishEvent(
                    new AlertProcessingCompletedEvent(true, latency, eventMsg.getEventUid()));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("报警消息处理失败, eventUid={}", eventMsg.getEventUid(), e);
            eventPublisher.publishEvent(
                    new AlertProcessingCompletedEvent(false, latency, eventMsg.getEventUid()));
            throw e;
        }
    }

    private void processAlertEvent(AlertEventMessage eventMsg) {
        String eventUid = eventMsg.getEventUid();
        if (StringUtils.isBlank(eventUid)) {
            log.warn("报警消息缺少eventUid，跳过处理");
            return;
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MonitoringEvent> existsQuery =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        existsQuery.eq("event_uid", eventUid);
        MonitoringEvent existedEvent = monitoringEventService.getOne(existsQuery);
        if (existedEvent != null) {
            log.info("报警事件已处理，跳过重复消息, eventUid={}", eventUid);
            return;
        }

        Long cameraId = eventMsg.getCameraId();
        if (cameraId == null && StringUtils.isNotBlank(eventMsg.getCameraCode())) {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CameraDevice> cameraQuery =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            cameraQuery.eq("camera_code", eventMsg.getCameraCode());
            CameraDevice cameraDevice = cameraDeviceService.getOne(cameraQuery);
            if (cameraDevice != null) {
                cameraId = cameraDevice.getId();
            }
        }

        String eventType =
                StringUtils.defaultIfBlank(
                        eventMsg.getEventType(),
                        eventMsg.getRiskLevel() != null ? "DROWNING" : "DROWNING");
        if (cameraId == null || StringUtils.isBlank(eventType)) {
            log.warn("报警消息缺少必要字段cameraId或eventType, eventUid={}", eventUid);
            return;
        }

        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setEvent_uid(eventUid);
        monitoringEvent.setCamera_id(cameraId);
        monitoringEvent.setEvent_type(eventType);
        monitoringEvent.setRisk_level(
                StringUtils.defaultIfBlank(eventMsg.getRiskLevel(), "MEDIUM"));
        monitoringEvent.setConfidence(eventMsg.getConfidence());
        monitoringEvent.setTarget_id(eventMsg.getTargetId());
        monitoringEvent.setPool_head_count(eventMsg.getPoolHeadCount());
        monitoringEvent.setBbox_json(toJsonText(eventMsg.getBboxJson()));
        monitoringEvent.setPosition_desc(eventMsg.getPositionDesc());
        monitoringEvent.setEmergency_contact_name(eventMsg.getEmergencyContactName());
        monitoringEvent.setEmergency_contact_phone(eventMsg.getEmergencyContactPhone());
        monitoringEvent.setIncident_location(eventMsg.getIncidentLocation());
        monitoringEvent.setVideo_stream_url(eventMsg.getVideoStreamUrl());
        monitoringEvent.setEvent_time(
                eventMsg.getEventTime() != null ? eventMsg.getEventTime() : new Date());
        monitoringEvent.setExt_json(toJsonText(eventMsg.getExtJson()));
        monitoringEvent.setCreated_at(new Date());
        monitoringEventService.validMonitoringEvent(monitoringEvent, true);
        monitoringEventService.save(monitoringEvent);

        Long venueId = eventMsg.getVenueId();
        if (venueId == null) {
            CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
            if (cameraDevice != null) {
                venueId = cameraDevice.getVenue_id();
            }
        }

        String alertUid = "ALERT-" + UUID.randomUUID().toString().replace("-", "");
        Lifeguard assignee = alertDispatchRoutingService.resolveAssignee(venueId, cameraId);
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setAlert_uid(alertUid);
        alertRecord.setEvent_id(monitoringEvent.getId());
        alertRecord.setCamera_id(cameraId);
        alertRecord.setVenue_id(venueId);
        alertRecord.setLifeguard_id(assignee == null ? null : assignee.getId());
        alertRecord.setAlert_type(StringUtils.defaultIfBlank(eventMsg.getAlertType(), eventType));
        alertRecord.setAlert_status("PENDING");
        alertRecord.setEmergency_contact_name(eventMsg.getEmergencyContactName());
        alertRecord.setEmergency_contact_phone(eventMsg.getEmergencyContactPhone());
        alertRecord.setIncident_location(eventMsg.getIncidentLocation());
        alertRecord.setVideo_stream_url(eventMsg.getVideoStreamUrl());
        alertRecord.setDetection_result(buildDetectionResult(eventMsg));
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
        wsData.put("targetId", eventMsg.getTargetId());
        wsData.put("confidence", eventMsg.getConfidence());
        wsData.put("detectTime", eventMsg.getDetectTime());
        wsData.put("videoStreamUrl", eventMsg.getVideoStreamUrl());
        wsData.put("riskPoint", extractMapValue(eventMsg.getExtJson(), "riskPoint"));
        wsData.put("riskScore", extractNumberValue(eventMsg.getExtJson(), "riskScore"));
        wsData.put("durationSec", extractNumberValue(eventMsg.getExtJson(), "durationSec"));
        wsData.put("triggered", extractBooleanValue(eventMsg.getExtJson(), "triggered"));
        wsData.put("ruleHits", extractListValue(eventMsg.getExtJson(), "ruleHits"));
        wsData.put("emergencyContactName", eventMsg.getEmergencyContactName());
        wsData.put("emergencyContactPhone", eventMsg.getEmergencyContactPhone());

        Set<Long> targetUserIds = resolveVenueTargetUserIds(venueId, assignee);
        if (!targetUserIds.isEmpty()) {
            Set<String> targetRoleCodes = new LinkedHashSet<>();
            targetRoleCodes.add("SUPER_ADMIN");
            targetRoleCodes.add("VENUE_ADMIN");
            alertWsPublisher.publishAlertCreated(
                    eventUid, alertUid, wsData, targetUserIds, targetRoleCodes);
        } else {
            alertWsPublisher.publishAlertCreated(eventUid, alertUid, wsData);
        }

        log.info(
                "报警事件处理完成, eventUid={}, alertUid={}, source={}",
                eventUid,
                alertUid,
                eventMsg.getSource());
    }

    private Set<Long> resolveVenueTargetUserIds(Long venueId, Lifeguard assignee) {
        Set<Long> targetUserIds = new LinkedHashSet<>();
        if (assignee != null && assignee.getUser_id() != null && assignee.getUser_id() > 0) {
            targetUserIds.add(assignee.getUser_id());
        }
        if (venueId == null || venueId <= 0) {
            return targetUserIds;
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Lifeguard> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("venue_id", venueId);
        queryWrapper.eq("audit_status", "APPROVED");
        queryWrapper.eq("duty_status", "ON_DUTY");
        queryWrapper.eq("is_delete", 0);
        List<Lifeguard> lifeguards = lifeguardService.list(queryWrapper);
        if (lifeguards == null || lifeguards.isEmpty()) {
            return targetUserIds;
        }
        for (Lifeguard item : lifeguards) {
            if (item != null && item.getUser_id() != null && item.getUser_id() > 0) {
                targetUserIds.add(item.getUser_id());
            }
        }
        return targetUserIds;
    }

    private String buildDetectionResult(AlertEventMessage eventMsg) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(eventMsg.getPositionDesc())) {
            sb.append(eventMsg.getPositionDesc());
        }
        Map<String, Object> extMap = toMapObject(eventMsg.getExtJson());
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
            Object riskLevel = extMap.get("riskLevel");
            if (riskLevel != null) {
                if (sb.length() > 0) {
                    sb.append("，");
                }
                sb.append("风险等级：").append(riskLevel);
            }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMapObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        try {
            return messageSerializer.deserialize(messageSerializer.serialize(value), Map.class);
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
        if ("true".equals(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "0".equals(text)) {
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

    private String toJsonText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return messageSerializer.serialize(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
