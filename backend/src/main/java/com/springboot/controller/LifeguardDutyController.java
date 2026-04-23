package com.springboot.controller;

import java.util.List;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.lifeguarddutylog.LifeguardDutyLogAddRequest;
import com.springboot.model.dto.lifeguarddutylog.LifeguardDutyLogEditRequest;
import com.springboot.model.dto.lifeguarddutylog.LifeguardDutyLogQueryRequest;
import com.springboot.model.dto.lifeguarddutylog.LifeguardDutyLogUpdateRequest;
import com.springboot.model.entity.LifeguardDutyLog;
import com.springboot.model.vo.LifeguardDutyLogVO;
import com.springboot.service.LifeguardDutyLogService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lifeguards/duty-logs")
public class LifeguardDutyController {

    @Resource private LifeguardDutyLogService lifeguardDutyLogService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addLifeguardDutyLog(
            @RequestBody LifeguardDutyLogAddRequest lifeguardDutyLogAddRequest) {
        if (lifeguardDutyLogAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardDutyLog lifeguardDutyLog = toLifeguardDutyLog(lifeguardDutyLogAddRequest);
        lifeguardDutyLogService.validLifeguardDutyLog(lifeguardDutyLog, true);
        boolean result = lifeguardDutyLogService.save(lifeguardDutyLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(lifeguardDutyLog.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteLifeguardDutyLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = lifeguardDutyLogService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateLifeguardDutyLog(
            @RequestBody LifeguardDutyLogUpdateRequest lifeguardDutyLogUpdateRequest) {
        if (lifeguardDutyLogUpdateRequest == null
                || lifeguardDutyLogUpdateRequest.getId() == null
                || lifeguardDutyLogUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardDutyLog lifeguardDutyLog = toLifeguardDutyLog(lifeguardDutyLogUpdateRequest);
        lifeguardDutyLogService.validLifeguardDutyLog(lifeguardDutyLog, false);
        boolean result = lifeguardDutyLogService.updateById(lifeguardDutyLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editLifeguardDutyLog(
            @RequestBody LifeguardDutyLogEditRequest lifeguardDutyLogEditRequest) {
        if (lifeguardDutyLogEditRequest == null
                || lifeguardDutyLogEditRequest.getId() == null
                || lifeguardDutyLogEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardDutyLog lifeguardDutyLog = new LifeguardDutyLog();
        lifeguardDutyLog.setId(lifeguardDutyLogEditRequest.getId());
        lifeguardDutyLog.setAction_type(lifeguardDutyLogEditRequest.getActionType());
        lifeguardDutyLog.setLeave_reason(lifeguardDutyLogEditRequest.getLeaveReason());
        lifeguardDutyLog.setPlanned_return_at(lifeguardDutyLogEditRequest.getPlannedReturnAt());
        lifeguardDutyLog.setActual_return_at(lifeguardDutyLogEditRequest.getActualReturnAt());
        lifeguardDutyLog.setApproved_by(lifeguardDutyLogEditRequest.getApprovedBy());
        lifeguardDutyLogService.validLifeguardDutyLog(lifeguardDutyLog, false);
        boolean result = lifeguardDutyLogService.updateById(lifeguardDutyLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<LifeguardDutyLog> getLifeguardDutyLogById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardDutyLog lifeguardDutyLog = lifeguardDutyLogService.getById(id);
        ThrowUtils.throwIf(lifeguardDutyLog == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(lifeguardDutyLog);
    }

    @GetMapping("/get/vo")
    public BaseResponse<LifeguardDutyLogVO> getLifeguardDutyLogVOById(long id) {
        BaseResponse<LifeguardDutyLog> response = getLifeguardDutyLogById(id);
        return ResultUtils.success(
                lifeguardDutyLogService.getLifeguardDutyLogVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<LifeguardDutyLog>> listLifeguardDutyLogByPage(
            @RequestBody LifeguardDutyLogQueryRequest lifeguardDutyLogQueryRequest) {
        if (lifeguardDutyLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = lifeguardDutyLogQueryRequest.getCurrent();
        long size = lifeguardDutyLogQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<LifeguardDutyLog> lifeguardDutyLogPage =
                lifeguardDutyLogService.page(
                        new Page<>(current, size),
                        lifeguardDutyLogService.getQueryWrapper(lifeguardDutyLogQueryRequest));
        return ResultUtils.success(lifeguardDutyLogPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<LifeguardDutyLogVO>> listLifeguardDutyLogVOByPage(
            @RequestBody LifeguardDutyLogQueryRequest lifeguardDutyLogQueryRequest) {
        BaseResponse<Page<LifeguardDutyLog>> response =
                listLifeguardDutyLogByPage(lifeguardDutyLogQueryRequest);
        Page<LifeguardDutyLog> lifeguardDutyLogPage = response.getData();
        Page<LifeguardDutyLogVO> lifeguardDutyLogVOPage =
                new Page<>(
                        lifeguardDutyLogPage.getCurrent(),
                        lifeguardDutyLogPage.getSize(),
                        lifeguardDutyLogPage.getTotal());
        List<LifeguardDutyLogVO> lifeguardDutyLogVOList =
                lifeguardDutyLogService.getLifeguardDutyLogVO(lifeguardDutyLogPage.getRecords());
        lifeguardDutyLogVOPage.setRecords(lifeguardDutyLogVOList);
        return ResultUtils.success(lifeguardDutyLogVOPage);
    }

    private LifeguardDutyLog toLifeguardDutyLog(LifeguardDutyLogAddRequest request) {
        LifeguardDutyLog lifeguardDutyLog = new LifeguardDutyLog();
        lifeguardDutyLog.setLifeguard_id(request.getLifeguardId());
        lifeguardDutyLog.setAction_type(request.getActionType());
        lifeguardDutyLog.setLeave_reason(request.getLeaveReason());
        lifeguardDutyLog.setPlanned_return_at(request.getPlannedReturnAt());
        lifeguardDutyLog.setActual_return_at(request.getActualReturnAt());
        lifeguardDutyLog.setApproved_by(request.getApprovedBy());
        return lifeguardDutyLog;
    }

    private LifeguardDutyLog toLifeguardDutyLog(LifeguardDutyLogUpdateRequest request) {
        LifeguardDutyLog lifeguardDutyLog = new LifeguardDutyLog();
        lifeguardDutyLog.setId(request.getId());
        lifeguardDutyLog.setLifeguard_id(request.getLifeguardId());
        lifeguardDutyLog.setAction_type(request.getActionType());
        lifeguardDutyLog.setLeave_reason(request.getLeaveReason());
        lifeguardDutyLog.setPlanned_return_at(request.getPlannedReturnAt());
        lifeguardDutyLog.setActual_return_at(request.getActualReturnAt());
        lifeguardDutyLog.setApproved_by(request.getApprovedBy());
        return lifeguardDutyLog;
    }
}
