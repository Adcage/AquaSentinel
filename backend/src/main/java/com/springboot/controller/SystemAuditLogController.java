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
import com.springboot.model.dto.systemauditlog.SystemAuditLogAddRequest;
import com.springboot.model.dto.systemauditlog.SystemAuditLogEditRequest;
import com.springboot.model.dto.systemauditlog.SystemAuditLogQueryRequest;
import com.springboot.model.dto.systemauditlog.SystemAuditLogUpdateRequest;
import com.springboot.model.entity.SystemAuditLog;
import com.springboot.model.vo.SystemAuditLogVO;
import com.springboot.service.SystemAuditLogService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system-audit-logs")
public class SystemAuditLogController {

    @Resource private SystemAuditLogService systemAuditLogService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addSystemAuditLog(
            @RequestBody SystemAuditLogAddRequest systemAuditLogAddRequest) {
        if (systemAuditLogAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemAuditLog systemAuditLog = toSystemAuditLog(systemAuditLogAddRequest);
        systemAuditLog.setCreated_at(new Date());
        systemAuditLogService.validSystemAuditLog(systemAuditLog, true);
        boolean result = systemAuditLogService.save(systemAuditLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(systemAuditLog.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteSystemAuditLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = systemAuditLogService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateSystemAuditLog(
            @RequestBody SystemAuditLogUpdateRequest systemAuditLogUpdateRequest) {
        if (systemAuditLogUpdateRequest == null
                || systemAuditLogUpdateRequest.getId() == null
                || systemAuditLogUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemAuditLog systemAuditLog = toSystemAuditLog(systemAuditLogUpdateRequest);
        systemAuditLogService.validSystemAuditLog(systemAuditLog, false);
        boolean result = systemAuditLogService.updateById(systemAuditLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editSystemAuditLog(
            @RequestBody SystemAuditLogEditRequest systemAuditLogEditRequest) {
        if (systemAuditLogEditRequest == null
                || systemAuditLogEditRequest.getId() == null
                || systemAuditLogEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemAuditLog systemAuditLog = new SystemAuditLog();
        systemAuditLog.setId(systemAuditLogEditRequest.getId());
        systemAuditLog.setResponse_code(systemAuditLogEditRequest.getResponseCode());
        systemAuditLog.setResponse_message(systemAuditLogEditRequest.getResponseMessage());
        systemAuditLog.setCost_ms(systemAuditLogEditRequest.getCostMs());
        systemAuditLogService.validSystemAuditLog(systemAuditLog, false);
        boolean result = systemAuditLogService.updateById(systemAuditLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<SystemAuditLog> getSystemAuditLogById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SystemAuditLog systemAuditLog = systemAuditLogService.getById(id);
        ThrowUtils.throwIf(systemAuditLog == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(systemAuditLog);
    }

    @GetMapping("/get/vo")
    public BaseResponse<SystemAuditLogVO> getSystemAuditLogVOById(long id) {
        BaseResponse<SystemAuditLog> response = getSystemAuditLogById(id);
        return ResultUtils.success(systemAuditLogService.getSystemAuditLogVO(response.getData()));
    }

    @PostMapping("/list")
    public BaseResponse<List<SystemAuditLog>> listSystemAuditLog(
            @RequestBody SystemAuditLogQueryRequest systemAuditLogQueryRequest) {
        if (systemAuditLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(
                systemAuditLogService.list(
                        systemAuditLogService.getQueryWrapper(systemAuditLogQueryRequest)));
    }

    @PostMapping("/list/vo")
    public BaseResponse<List<SystemAuditLogVO>> listSystemAuditLogVO(
            @RequestBody SystemAuditLogQueryRequest systemAuditLogQueryRequest) {
        BaseResponse<List<SystemAuditLog>> response =
                listSystemAuditLog(systemAuditLogQueryRequest);
        return ResultUtils.success(systemAuditLogService.getSystemAuditLogVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<SystemAuditLog>> listSystemAuditLogByPage(
            @RequestBody SystemAuditLogQueryRequest systemAuditLogQueryRequest) {
        if (systemAuditLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = systemAuditLogQueryRequest.getCurrent();
        long size = systemAuditLogQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<SystemAuditLog> systemAuditLogPage =
                systemAuditLogService.page(
                        new Page<>(current, size),
                        systemAuditLogService.getQueryWrapper(systemAuditLogQueryRequest));
        return ResultUtils.success(systemAuditLogPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SystemAuditLogVO>> listSystemAuditLogVOByPage(
            @RequestBody SystemAuditLogQueryRequest systemAuditLogQueryRequest) {
        BaseResponse<Page<SystemAuditLog>> response =
                listSystemAuditLogByPage(systemAuditLogQueryRequest);
        Page<SystemAuditLog> systemAuditLogPage = response.getData();
        Page<SystemAuditLogVO> systemAuditLogVOPage =
                new Page<>(
                        systemAuditLogPage.getCurrent(),
                        systemAuditLogPage.getSize(),
                        systemAuditLogPage.getTotal());
        systemAuditLogVOPage.setRecords(
                systemAuditLogService.getSystemAuditLogVO(systemAuditLogPage.getRecords()));
        return ResultUtils.success(systemAuditLogVOPage);
    }

    private SystemAuditLog toSystemAuditLog(SystemAuditLogAddRequest request) {
        SystemAuditLog systemAuditLog = new SystemAuditLog();
        systemAuditLog.setTrace_id(request.getTraceId());
        systemAuditLog.setLog_category(request.getLogCategory());
        systemAuditLog.setOperator_id(request.getOperatorId());
        systemAuditLog.setOperator_name(request.getOperatorName());
        systemAuditLog.setClient_ip(request.getClientIp());
        systemAuditLog.setRequest_uri(request.getRequestUri());
        systemAuditLog.setRequest_method(request.getRequestMethod());
        systemAuditLog.setRequest_body(request.getRequestBody());
        systemAuditLog.setResponse_code(request.getResponseCode());
        systemAuditLog.setResponse_message(request.getResponseMessage());
        systemAuditLog.setCost_ms(request.getCostMs());
        return systemAuditLog;
    }

    private SystemAuditLog toSystemAuditLog(SystemAuditLogUpdateRequest request) {
        SystemAuditLog systemAuditLog = new SystemAuditLog();
        systemAuditLog.setId(request.getId());
        systemAuditLog.setTrace_id(request.getTraceId());
        systemAuditLog.setLog_category(request.getLogCategory());
        systemAuditLog.setOperator_id(request.getOperatorId());
        systemAuditLog.setOperator_name(request.getOperatorName());
        systemAuditLog.setClient_ip(request.getClientIp());
        systemAuditLog.setRequest_uri(request.getRequestUri());
        systemAuditLog.setRequest_method(request.getRequestMethod());
        systemAuditLog.setRequest_body(request.getRequestBody());
        systemAuditLog.setResponse_code(request.getResponseCode());
        systemAuditLog.setResponse_message(request.getResponseMessage());
        systemAuditLog.setCost_ms(request.getCostMs());
        return systemAuditLog;
    }
}
