package com.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.service.AlertRecordService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.MonitoringEventService;
import com.springboot.service.SystemNoticeConfigService;
import com.springboot.websocket.AlertWsPublisher;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DeviceOfflineAlertService {

    @Resource
    private CameraDeviceService cameraDeviceService;

    @Resource
    private MonitoringEventService monitoringEventService;

    @Resource
    private AlertRecordService alertRecordService;

    @Resource
    private SystemNoticeConfigService systemNoticeConfigService;

    @Resource
    private AlertWsPublisher alertWsPublisher;

    @Resource
    private ObjectMapper objectMapper;

    @Scheduled(initialDelay = 15_000L, fixedDelay = 15_000L)
    public void checkDeviceOfflineAlerts() {
        QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("enabled", 1);
        queryWrapper.eq("is_delete", 0);
        List<CameraDevice> cameras = cameraDeviceService.list(queryWrapper);
        Date now = new Date();
        for (CameraDevice camera : cameras) {
            if (camera == null || camera.getId() == null) {
                continue;
            }
            Date lastHeartbeat = camera.getLast_heartbeat_at();
            if (lastHeartbeat == null) {
                continue;
            }
            int thresholdSec = systemNoticeConfigService.getDeviceOfflineThresholdSec();
            long offlineSec = Math.max(0L, (now.getTime() - lastHeartbeat.getTime()) / 1000L);
            if (offlineSec < thresholdSec) {
                continue;
            }
            if (hasActiveOfflineAlert(camera.getId())) {
                continue;
            }
            createOfflineAlert(camera, now, offlineSec, thresholdSec);
        }
    }

    private boolean hasActiveOfflineAlert(Long cameraId) {
        QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("camera_id", cameraId);
        queryWrapper.eq("alert_type", "DEVICE_OFFLINE");
        queryWrapper.ne("alert_status", "DONE");
        queryWrapper.ne("alert_status", "FALSE_ALARM");
        queryWrapper.last("limit 1");
        return alertRecordService.getOne(queryWrapper) != null;
    }

    private void createOfflineAlert(CameraDevice camera, Date now, long offlineSec, int thresholdSec) {
        MonitoringEvent event = new MonitoringEvent();
        event.setEvent_uid("evt_offline_" + camera.getId() + "_" + System.currentTimeMillis());
        event.setCamera_id(camera.getId());
        event.setEvent_type("DEVICE_OFFLINE");
        event.setRisk_level("HIGH");
        event.setConfidence(new BigDecimal("1.0000"));
        event.setTarget_id(camera.getCamera_code());
        event.setIncident_location("cameraId=" + camera.getId());
        event.setPosition_desc("设备离线");
        event.setVideo_stream_url(camera.getStream_url());
        event.setEvent_time(now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("lastHeartbeatAt", camera.getLast_heartbeat_at() == null ? null : camera.getLast_heartbeat_at().getTime());
        ext.put("offlineDurationSec", offlineSec);
        ext.put("thresholdSec", thresholdSec);
        event.setExt_json(toJsonText(ext));
        event.setCreated_at(now);
        monitoringEventService.save(event);

        AlertRecord alert = new AlertRecord();
        alert.setAlert_uid("ALERT-" + UUID.randomUUID().toString().replace("-", ""));
        alert.setEvent_id(event.getId());
        alert.setCamera_id(camera.getId());
        alert.setVenue_id(camera.getVenue_id());
        alert.setAlert_type("DEVICE_OFFLINE");
        alert.setAlert_status("PENDING");
        alert.setIncident_location("摄像头离线:" + camera.getCamera_code());
        alert.setVideo_stream_url(camera.getStream_url());
        alert.setPushed_to_app(1);
        alert.setPushed_to_pc(1);
        alert.setFirst_push_time(now);
        alert.setCreated_at(now);
        alert.setUpdated_at(now);
        alertRecordService.save(alert);

        Map<String, Object> wsData = new HashMap<>();
        wsData.put("alertId", alert.getId());
        wsData.put("alertUid", alert.getAlert_uid());
        wsData.put("eventId", event.getId());
        wsData.put("cameraId", camera.getId());
        wsData.put("eventType", event.getEvent_type());
        wsData.put("riskLevel", event.getRisk_level());
        wsData.put("offlineDurationSec", offlineSec);
        alertWsPublisher.publishAlertCreated(event.getEvent_uid(), alert.getAlert_uid(), wsData);
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
