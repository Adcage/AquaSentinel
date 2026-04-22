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
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationLogAddRequest;
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationLogEditRequest;
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationLogQueryRequest;
import com.springboot.model.dto.lifeguardlocationlog.LifeguardLocationLogUpdateRequest;
import com.springboot.model.entity.LifeguardLocationLog;
import com.springboot.model.vo.LifeguardLocationLogVO;
import com.springboot.service.LifeguardLocationLogService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lifeguards/location-logs")
public class LifeguardLocationController {

    @Resource
    private LifeguardLocationLogService lifeguardLocationLogService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addLifeguardLocationLog(@RequestBody LifeguardLocationLogAddRequest lifeguardLocationLogAddRequest) {
        if (lifeguardLocationLogAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardLocationLog lifeguardLocationLog = toLifeguardLocationLog(lifeguardLocationLogAddRequest);
        lifeguardLocationLogService.validLifeguardLocationLog(lifeguardLocationLog, true);
        boolean result = lifeguardLocationLogService.save(lifeguardLocationLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(lifeguardLocationLog.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteLifeguardLocationLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = lifeguardLocationLogService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateLifeguardLocationLog(
            @RequestBody LifeguardLocationLogUpdateRequest lifeguardLocationLogUpdateRequest) {
        if (lifeguardLocationLogUpdateRequest == null || lifeguardLocationLogUpdateRequest.getId() == null
                || lifeguardLocationLogUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardLocationLog lifeguardLocationLog = toLifeguardLocationLog(lifeguardLocationLogUpdateRequest);
        lifeguardLocationLogService.validLifeguardLocationLog(lifeguardLocationLog, false);
        boolean result = lifeguardLocationLogService.updateById(lifeguardLocationLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editLifeguardLocationLog(@RequestBody LifeguardLocationLogEditRequest lifeguardLocationLogEditRequest) {
        if (lifeguardLocationLogEditRequest == null || lifeguardLocationLogEditRequest.getId() == null
                || lifeguardLocationLogEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardLocationLog lifeguardLocationLog = new LifeguardLocationLog();
        lifeguardLocationLog.setId(lifeguardLocationLogEditRequest.getId());
        lifeguardLocationLog.setLongitude(lifeguardLocationLogEditRequest.getLongitude());
        lifeguardLocationLog.setLatitude(lifeguardLocationLogEditRequest.getLatitude());
        lifeguardLocationLog.setIn_fence(lifeguardLocationLogEditRequest.getInFence());
        lifeguardLocationLog.setReport_source(lifeguardLocationLogEditRequest.getReportSource());
        lifeguardLocationLog.setReported_at(lifeguardLocationLogEditRequest.getReportedAt());
        lifeguardLocationLogService.validLifeguardLocationLog(lifeguardLocationLog, false);
        boolean result = lifeguardLocationLogService.updateById(lifeguardLocationLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<LifeguardLocationLog> getLifeguardLocationLogById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LifeguardLocationLog lifeguardLocationLog = lifeguardLocationLogService.getById(id);
        ThrowUtils.throwIf(lifeguardLocationLog == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(lifeguardLocationLog);
    }

    @GetMapping("/get/vo")
    public BaseResponse<LifeguardLocationLogVO> getLifeguardLocationLogVOById(long id) {
        BaseResponse<LifeguardLocationLog> response = getLifeguardLocationLogById(id);
        return ResultUtils.success(lifeguardLocationLogService.getLifeguardLocationLogVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<LifeguardLocationLog>> listLifeguardLocationLogByPage(
            @RequestBody LifeguardLocationLogQueryRequest lifeguardLocationLogQueryRequest) {
        if (lifeguardLocationLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = lifeguardLocationLogQueryRequest.getCurrent();
        long size = lifeguardLocationLogQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<LifeguardLocationLog> lifeguardLocationLogPage = lifeguardLocationLogService.page(new Page<>(current, size),
                lifeguardLocationLogService.getQueryWrapper(lifeguardLocationLogQueryRequest));
        return ResultUtils.success(lifeguardLocationLogPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<LifeguardLocationLogVO>> listLifeguardLocationLogVOByPage(
            @RequestBody LifeguardLocationLogQueryRequest lifeguardLocationLogQueryRequest) {
        BaseResponse<Page<LifeguardLocationLog>> response = listLifeguardLocationLogByPage(lifeguardLocationLogQueryRequest);
        Page<LifeguardLocationLog> lifeguardLocationLogPage = response.getData();
        Page<LifeguardLocationLogVO> lifeguardLocationLogVOPage = new Page<>(lifeguardLocationLogPage.getCurrent(),
                lifeguardLocationLogPage.getSize(), lifeguardLocationLogPage.getTotal());
        List<LifeguardLocationLogVO> lifeguardLocationLogVOList = lifeguardLocationLogService.getLifeguardLocationLogVO(
                lifeguardLocationLogPage.getRecords());
        lifeguardLocationLogVOPage.setRecords(lifeguardLocationLogVOList);
        return ResultUtils.success(lifeguardLocationLogVOPage);
    }

    private LifeguardLocationLog toLifeguardLocationLog(LifeguardLocationLogAddRequest request) {
        LifeguardLocationLog lifeguardLocationLog = new LifeguardLocationLog();
        lifeguardLocationLog.setLifeguard_id(request.getLifeguardId());
        lifeguardLocationLog.setVenue_id(request.getVenueId());
        lifeguardLocationLog.setLongitude(request.getLongitude());
        lifeguardLocationLog.setLatitude(request.getLatitude());
        lifeguardLocationLog.setIn_fence(request.getInFence());
        lifeguardLocationLog.setReport_source(request.getReportSource());
        lifeguardLocationLog.setReported_at(request.getReportedAt());
        return lifeguardLocationLog;
    }

    private LifeguardLocationLog toLifeguardLocationLog(LifeguardLocationLogUpdateRequest request) {
        LifeguardLocationLog lifeguardLocationLog = new LifeguardLocationLog();
        lifeguardLocationLog.setId(request.getId());
        lifeguardLocationLog.setLifeguard_id(request.getLifeguardId());
        lifeguardLocationLog.setVenue_id(request.getVenueId());
        lifeguardLocationLog.setLongitude(request.getLongitude());
        lifeguardLocationLog.setLatitude(request.getLatitude());
        lifeguardLocationLog.setIn_fence(request.getInFence());
        lifeguardLocationLog.setReport_source(request.getReportSource());
        lifeguardLocationLog.setReported_at(request.getReportedAt());
        return lifeguardLocationLog;
    }
}
