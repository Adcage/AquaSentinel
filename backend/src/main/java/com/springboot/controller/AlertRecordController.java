package com.springboot.controller;

import java.util.Date;
import java.util.List;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.alertrecord.AlertRecordAddRequest;
import com.springboot.model.dto.alertrecord.AlertRecordEditRequest;
import com.springboot.model.dto.alertrecord.AlertRecordQueryRequest;
import com.springboot.model.dto.alertrecord.AlertRecordUpdateRequest;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.vo.AlertRecordVO;
import com.springboot.service.AlertRecordService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alert-records")
public class AlertRecordController {

    @Resource private AlertRecordService alertRecordService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addAlertRecord(
            @RequestBody AlertRecordAddRequest alertRecordAddRequest) {
        if (alertRecordAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertRecord alertRecord = toAlertRecord(alertRecordAddRequest);
        alertRecord.setAlert_status(
                alertRecord.getAlert_status() == null ? "PENDING" : alertRecord.getAlert_status());
        alertRecord.setAlert_type(
                alertRecord.getAlert_type() == null ? "DROWING" : alertRecord.getAlert_type());
        alertRecord.setPushed_to_app(
                alertRecord.getPushed_to_app() == null ? 0 : alertRecord.getPushed_to_app());
        alertRecord.setPushed_to_pc(
                alertRecord.getPushed_to_pc() == null ? 0 : alertRecord.getPushed_to_pc());
        alertRecord.setCreated_at(new Date());
        alertRecord.setUpdated_at(new Date());
        alertRecordService.validAlertRecord(alertRecord, true);
        boolean result = alertRecordService.save(alertRecord);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(alertRecord.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteAlertRecord(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = alertRecordService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateAlertRecord(
            @RequestBody AlertRecordUpdateRequest alertRecordUpdateRequest) {
        if (alertRecordUpdateRequest == null
                || alertRecordUpdateRequest.getId() == null
                || alertRecordUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertRecord alertRecord = toAlertRecord(alertRecordUpdateRequest);
        alertRecordService.validAlertRecord(alertRecord, false);
        boolean result = alertRecordService.updateById(alertRecord);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editAlertRecord(
            @RequestBody AlertRecordEditRequest alertRecordEditRequest) {
        if (alertRecordEditRequest == null
                || alertRecordEditRequest.getId() == null
                || alertRecordEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setId(alertRecordEditRequest.getId());
        alertRecord.setLifeguard_id(alertRecordEditRequest.getLifeguardId());
        alertRecord.setAlert_status(alertRecordEditRequest.getAlertStatus());
        alertRecord.setEmergency_contact_name(alertRecordEditRequest.getEmergencyContactName());
        alertRecord.setEmergency_contact_phone(alertRecordEditRequest.getEmergencyContactPhone());
        alertRecord.setIncident_location(alertRecordEditRequest.getIncidentLocation());
        alertRecord.setVideo_stream_url(alertRecordEditRequest.getVideoStreamUrl());
        alertRecordService.validAlertRecord(alertRecord, false);
        boolean result = alertRecordService.updateById(alertRecord);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<AlertRecord> getAlertRecordById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AlertRecord alertRecord = alertRecordService.getById(id);
        ThrowUtils.throwIf(alertRecord == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(alertRecord);
    }

    @GetMapping("/get/vo")
    public BaseResponse<AlertRecordVO> getAlertRecordVOById(long id) {
        BaseResponse<AlertRecord> response = getAlertRecordById(id);
        return ResultUtils.success(alertRecordService.getAlertRecordVO(response.getData()));
    }

    @PostMapping("/list")
    public BaseResponse<List<AlertRecord>> listAlertRecord(
            @RequestBody AlertRecordQueryRequest alertRecordQueryRequest) {
        if (alertRecordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(
                alertRecordService.list(
                        alertRecordService.getQueryWrapper(alertRecordQueryRequest)));
    }

    @PostMapping("/list/vo")
    public BaseResponse<List<AlertRecordVO>> listAlertRecordVO(
            @RequestBody AlertRecordQueryRequest alertRecordQueryRequest) {
        BaseResponse<List<AlertRecord>> response = listAlertRecord(alertRecordQueryRequest);
        return ResultUtils.success(alertRecordService.getAlertRecordVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<AlertRecord>> listAlertRecordByPage(
            @RequestBody AlertRecordQueryRequest alertRecordQueryRequest) {
        if (alertRecordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = alertRecordQueryRequest.getCurrent();
        long size = alertRecordQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<AlertRecord> alertRecordPage =
                alertRecordService.page(
                        new Page<>(current, size),
                        alertRecordService.getQueryWrapper(alertRecordQueryRequest));
        return ResultUtils.success(alertRecordPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AlertRecordVO>> listAlertRecordVOByPage(
            @RequestBody AlertRecordQueryRequest alertRecordQueryRequest) {
        BaseResponse<Page<AlertRecord>> response = listAlertRecordByPage(alertRecordQueryRequest);
        Page<AlertRecord> alertRecordPage = response.getData();
        Page<AlertRecordVO> alertRecordVOPage =
                new Page<>(
                        alertRecordPage.getCurrent(),
                        alertRecordPage.getSize(),
                        alertRecordPage.getTotal());
        alertRecordVOPage.setRecords(
                alertRecordService.getAlertRecordVO(alertRecordPage.getRecords()));
        return ResultUtils.success(alertRecordVOPage);
    }

    private AlertRecord toAlertRecord(AlertRecordAddRequest request) {
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setAlert_uid(request.getAlertUid());
        alertRecord.setEvent_id(request.getEventId());
        alertRecord.setCamera_id(request.getCameraId());
        alertRecord.setVenue_id(request.getVenueId());
        alertRecord.setLifeguard_id(request.getLifeguardId());
        alertRecord.setAlert_type(request.getAlertType());
        alertRecord.setAlert_status(request.getAlertStatus());
        alertRecord.setEmergency_contact_name(request.getEmergencyContactName());
        alertRecord.setEmergency_contact_phone(request.getEmergencyContactPhone());
        alertRecord.setIncident_location(request.getIncidentLocation());
        alertRecord.setVideo_stream_url(request.getVideoStreamUrl());
        return alertRecord;
    }

    private AlertRecord toAlertRecord(AlertRecordUpdateRequest request) {
        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setId(request.getId());
        alertRecord.setAlert_uid(request.getAlertUid());
        alertRecord.setEvent_id(request.getEventId());
        alertRecord.setCamera_id(request.getCameraId());
        alertRecord.setVenue_id(request.getVenueId());
        alertRecord.setLifeguard_id(request.getLifeguardId());
        alertRecord.setAlert_type(request.getAlertType());
        alertRecord.setAlert_status(request.getAlertStatus());
        alertRecord.setEmergency_contact_name(request.getEmergencyContactName());
        alertRecord.setEmergency_contact_phone(request.getEmergencyContactPhone());
        alertRecord.setIncident_location(request.getIncidentLocation());
        alertRecord.setVideo_stream_url(request.getVideoStreamUrl());
        alertRecord.setPushed_to_app(request.getPushedToApp());
        alertRecord.setPushed_to_pc(request.getPushedToPc());
        alertRecord.setFirst_push_time(request.getFirstPushTime());
        alertRecord.setResolved_time(request.getResolvedTime());
        return alertRecord;
    }
}
