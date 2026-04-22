package com.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.monitoringevent.MonitoringEventAddRequest;
import com.springboot.model.dto.monitoringevent.MonitoringEventEditRequest;
import com.springboot.model.dto.monitoringevent.MonitoringEventQueryRequest;
import com.springboot.model.dto.monitoringevent.MonitoringEventUpdateRequest;
import com.springboot.model.entity.MonitoringEvent;
import com.springboot.model.vo.MonitoringEventVO;
import com.springboot.service.MonitoringEventService;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitoring-events")
public class MonitoringEventController {

    @Resource
    private MonitoringEventService monitoringEventService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addMonitoringEvent(@RequestBody MonitoringEventAddRequest monitoringEventAddRequest) {
        if (monitoringEventAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MonitoringEvent monitoringEvent = toMonitoringEvent(monitoringEventAddRequest);
        monitoringEvent.setEvent_time(monitoringEvent.getEvent_time() == null ? new Date() : monitoringEvent.getEvent_time());
        monitoringEvent.setCreated_at(new Date());
        monitoringEventService.validMonitoringEvent(monitoringEvent, true);
        boolean result = monitoringEventService.save(monitoringEvent);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(monitoringEvent.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteMonitoringEvent(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = monitoringEventService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateMonitoringEvent(@RequestBody MonitoringEventUpdateRequest monitoringEventUpdateRequest) {
        if (monitoringEventUpdateRequest == null || monitoringEventUpdateRequest.getId() == null
                || monitoringEventUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MonitoringEvent monitoringEvent = toMonitoringEvent(monitoringEventUpdateRequest);
        monitoringEventService.validMonitoringEvent(monitoringEvent, false);
        boolean result = monitoringEventService.updateById(monitoringEvent);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editMonitoringEvent(@RequestBody MonitoringEventEditRequest monitoringEventEditRequest) {
        if (monitoringEventEditRequest == null || monitoringEventEditRequest.getId() == null
                || monitoringEventEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setId(monitoringEventEditRequest.getId());
        monitoringEvent.setRisk_level(monitoringEventEditRequest.getRiskLevel());
        monitoringEvent.setConfidence(monitoringEventEditRequest.getConfidence());
        monitoringEvent.setPool_head_count(monitoringEventEditRequest.getPoolHeadCount());
        monitoringEvent.setPosition_desc(monitoringEventEditRequest.getPositionDesc());
        monitoringEvent.setEmergency_contact_name(monitoringEventEditRequest.getEmergencyContactName());
        monitoringEvent.setEmergency_contact_phone(monitoringEventEditRequest.getEmergencyContactPhone());
        monitoringEvent.setIncident_location(monitoringEventEditRequest.getIncidentLocation());
        monitoringEvent.setVideo_stream_url(monitoringEventEditRequest.getVideoStreamUrl());
        monitoringEvent.setEvent_time(monitoringEventEditRequest.getEventTime());
        monitoringEvent.setExt_json(monitoringEventEditRequest.getExtJson());
        monitoringEventService.validMonitoringEvent(monitoringEvent, false);
        boolean result = monitoringEventService.updateById(monitoringEvent);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<MonitoringEvent> getMonitoringEventById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MonitoringEvent monitoringEvent = monitoringEventService.getById(id);
        ThrowUtils.throwIf(monitoringEvent == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(monitoringEvent);
    }

    @GetMapping("/get/vo")
    public BaseResponse<MonitoringEventVO> getMonitoringEventVOById(long id) {
        BaseResponse<MonitoringEvent> response = getMonitoringEventById(id);
        return ResultUtils.success(monitoringEventService.getMonitoringEventVO(response.getData()));
    }

    @PostMapping("/list")
    public BaseResponse<List<MonitoringEvent>> listMonitoringEvent(@RequestBody MonitoringEventQueryRequest monitoringEventQueryRequest) {
        if (monitoringEventQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(monitoringEventService.list(monitoringEventService.getQueryWrapper(monitoringEventQueryRequest)));
    }

    @PostMapping("/list/vo")
    public BaseResponse<List<MonitoringEventVO>> listMonitoringEventVO(@RequestBody MonitoringEventQueryRequest monitoringEventQueryRequest) {
        BaseResponse<List<MonitoringEvent>> response = listMonitoringEvent(monitoringEventQueryRequest);
        return ResultUtils.success(monitoringEventService.getMonitoringEventVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<MonitoringEvent>> listMonitoringEventByPage(@RequestBody MonitoringEventQueryRequest monitoringEventQueryRequest) {
        if (monitoringEventQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = monitoringEventQueryRequest.getCurrent();
        long size = monitoringEventQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<MonitoringEvent> monitoringEventPage = monitoringEventService.page(new Page<>(current, size),
                monitoringEventService.getQueryWrapper(monitoringEventQueryRequest));
        return ResultUtils.success(monitoringEventPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<MonitoringEventVO>> listMonitoringEventVOByPage(@RequestBody MonitoringEventQueryRequest monitoringEventQueryRequest) {
        BaseResponse<Page<MonitoringEvent>> response = listMonitoringEventByPage(monitoringEventQueryRequest);
        Page<MonitoringEvent> monitoringEventPage = response.getData();
        Page<MonitoringEventVO> monitoringEventVOPage = new Page<>(monitoringEventPage.getCurrent(), monitoringEventPage.getSize(),
                monitoringEventPage.getTotal());
        monitoringEventVOPage.setRecords(monitoringEventService.getMonitoringEventVO(monitoringEventPage.getRecords()));
        return ResultUtils.success(monitoringEventVOPage);
    }

    private MonitoringEvent toMonitoringEvent(MonitoringEventAddRequest request) {
        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setEvent_uid(request.getEventUid());
        monitoringEvent.setCamera_id(request.getCameraId());
        monitoringEvent.setTask_id(request.getTaskId());
        monitoringEvent.setEvent_type(request.getEventType());
        monitoringEvent.setRisk_level(request.getRiskLevel());
        monitoringEvent.setConfidence(request.getConfidence());
        monitoringEvent.setTarget_id(request.getTargetId());
        monitoringEvent.setPool_head_count(request.getPoolHeadCount());
        monitoringEvent.setBbox_json(request.getBboxJson());
        monitoringEvent.setPosition_desc(request.getPositionDesc());
        monitoringEvent.setEmergency_contact_name(request.getEmergencyContactName());
        monitoringEvent.setEmergency_contact_phone(request.getEmergencyContactPhone());
        monitoringEvent.setIncident_location(request.getIncidentLocation());
        monitoringEvent.setVideo_stream_url(request.getVideoStreamUrl());
        monitoringEvent.setEvent_time(request.getEventTime());
        monitoringEvent.setExt_json(request.getExtJson());
        return monitoringEvent;
    }

    private MonitoringEvent toMonitoringEvent(MonitoringEventUpdateRequest request) {
        MonitoringEvent monitoringEvent = new MonitoringEvent();
        monitoringEvent.setId(request.getId());
        monitoringEvent.setEvent_uid(request.getEventUid());
        monitoringEvent.setCamera_id(request.getCameraId());
        monitoringEvent.setTask_id(request.getTaskId());
        monitoringEvent.setEvent_type(request.getEventType());
        monitoringEvent.setRisk_level(request.getRiskLevel());
        monitoringEvent.setConfidence(request.getConfidence());
        monitoringEvent.setTarget_id(request.getTargetId());
        monitoringEvent.setPool_head_count(request.getPoolHeadCount());
        monitoringEvent.setBbox_json(request.getBboxJson());
        monitoringEvent.setPosition_desc(request.getPositionDesc());
        monitoringEvent.setEmergency_contact_name(request.getEmergencyContactName());
        monitoringEvent.setEmergency_contact_phone(request.getEmergencyContactPhone());
        monitoringEvent.setIncident_location(request.getIncidentLocation());
        monitoringEvent.setVideo_stream_url(request.getVideoStreamUrl());
        monitoringEvent.setEvent_time(request.getEventTime());
        monitoringEvent.setExt_json(request.getExtJson());
        return monitoringEvent;
    }
}
