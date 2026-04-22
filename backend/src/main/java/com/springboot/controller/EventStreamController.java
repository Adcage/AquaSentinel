package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.entity.VenueZone;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.MonitoringEventService;
import com.springboot.service.VenueZoneService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventStreamController {

    @Resource
    private CameraDeviceService cameraDeviceService;

    @Resource
    private VenueZoneService venueZoneService;

    @Resource
    private MonitoringEventService monitoringEventService;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/annotated-stream")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getAnnotatedStream(@RequestParam Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraId不能为空");
        }
        CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
        if (cameraDevice == null || Integer.valueOf(1).equals(cameraDevice.getIs_delete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "摄像头不存在");
        }

        QueryWrapper<MonitoringEvent> eventQuery = new QueryWrapper<>();
        eventQuery.eq("camera_id", cameraId);
        eventQuery.orderByDesc("event_time");
        eventQuery.last("limit 30");
        List<MonitoringEvent> eventList = monitoringEventService.list(eventQuery);

        String streamUrl = StringUtils.defaultIfBlank(cameraDevice.getStream_url(), "");
        for (MonitoringEvent event : eventList) {
            if (StringUtils.isNotBlank(event.getVideo_stream_url())) {
                streamUrl = event.getVideo_stream_url();
                break;
            }
        }

        List<Map<String, Object>> greenOverlays = new ArrayList<>();
        if (cameraDevice.getZone_id() != null) {
            VenueZone venueZone = venueZoneService.getById(cameraDevice.getZone_id());
            if (venueZone != null && venueZone.getGeo_json() != null) {
                Map<String, Object> green = new LinkedHashMap<>();
                green.put("overlayType", "POLYGON");
                green.put("color", "GREEN");
                green.put("zoneCode", venueZone.getZone_code());
                green.put("zoneName", venueZone.getZone_name());
                green.put("geoJson", parseJsonValue(venueZone.getGeo_json()));
                green.put("label", "正常区域");
                greenOverlays.add(green);
            }
        }

        List<Map<String, Object>> redOverlays = new ArrayList<>();
        for (MonitoringEvent event : eventList) {
            if (event.getBbox_json() == null) {
                continue;
            }
            Map<String, Object> red = new LinkedHashMap<>();
            red.put("overlayType", "BBOX");
            red.put("color", "RED");
            red.put("eventUid", event.getEvent_uid());
            red.put("eventType", event.getEvent_type());
            red.put("riskLevel", event.getRisk_level());
            red.put("targetId", event.getTarget_id());
            red.put("bbox", parseJsonValue(event.getBbox_json()));
            red.put("occurredAt", event.getEvent_time());
            redOverlays.add(red);
        }

        Map<String, Object> overlayMeta = new LinkedHashMap<>();
        overlayMeta.put("generatedAt", new Date());
        overlayMeta.put("greenOverlays", greenOverlays);
        overlayMeta.put("redOverlays", redOverlays);
        overlayMeta.put("overlayCount", greenOverlays.size() + redOverlays.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cameraId", cameraId);
        data.put("streamUrl", streamUrl);
        data.put("overlayMeta", overlayMeta);
        return ResultUtils.success(data);
    }

    @GetMapping
    public BaseResponse<List<Map<String, Object>>> listEvents(@RequestParam(required = false) Long cameraId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        QueryWrapper<MonitoringEvent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(cameraId != null, "camera_id", cameraId);
        queryWrapper.eq(StringUtils.isNotBlank(eventType), "event_type", eventType);
        queryWrapper.ge(startTime != null, "event_time", new Date(startTime == null ? 0L : startTime));
        queryWrapper.le(endTime != null, "event_time", new Date(endTime == null ? 0L : endTime));
        queryWrapper.orderByDesc("event_time", "id");
        int queryLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 200));
        queryWrapper.last("limit " + queryLimit);
        List<MonitoringEvent> eventList = monitoringEventService.list(queryWrapper);

        List<Map<String, Object>> data = new ArrayList<>();
        for (MonitoringEvent event : eventList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", event.getId());
            item.put("eventUid", event.getEvent_uid());
            item.put("cameraId", event.getCamera_id());
            item.put("eventType", event.getEvent_type());
            item.put("riskLevel", event.getRisk_level());
            item.put("confidence", event.getConfidence());
            item.put("targetId", event.getTarget_id());
            item.put("poolHeadCount", event.getPool_head_count());
            item.put("bbox", parseJsonValue(event.getBbox_json()));
            item.put("eventTime", event.getEvent_time());
            item.put("incidentLocation", event.getIncident_location());
            item.put("videoStreamUrl", event.getVideo_stream_url());
            data.add(item);
        }
        return ResultUtils.success(data);
    }

    private Object parseJsonValue(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            if (raw instanceof String text) {
                if (StringUtils.isBlank(text)) {
                    return null;
                }
                JsonNode node = objectMapper.readTree(text);
                return objectMapper.convertValue(node, Object.class);
            }
            JsonNode node = objectMapper.valueToTree(raw);
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }
}
