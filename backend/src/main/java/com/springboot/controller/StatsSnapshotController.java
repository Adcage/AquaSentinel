package com.springboot.controller;

import java.math.BigDecimal;
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
import com.springboot.model.dto.statssnapshot.StatsSnapshotAddRequest;
import com.springboot.model.dto.statssnapshot.StatsSnapshotEditRequest;
import com.springboot.model.dto.statssnapshot.StatsSnapshotQueryRequest;
import com.springboot.model.dto.statssnapshot.StatsSnapshotUpdateRequest;
import com.springboot.model.entity.StatsSnapshot;
import com.springboot.model.vo.StatsSnapshotVO;
import com.springboot.service.StatsSnapshotService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats-snapshots")
public class StatsSnapshotController {

    @Resource private StatsSnapshotService statsSnapshotService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addStatsSnapshot(
            @RequestBody StatsSnapshotAddRequest statsSnapshotAddRequest) {
        if (statsSnapshotAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        StatsSnapshot statsSnapshot = toStatsSnapshot(statsSnapshotAddRequest);
        statsSnapshot.setMetric_value(
                statsSnapshot.getMetric_value() == null
                        ? BigDecimal.ZERO
                        : statsSnapshot.getMetric_value());
        statsSnapshot.setCreated_at(new Date());
        statsSnapshotService.validStatsSnapshot(statsSnapshot, true);
        boolean result = statsSnapshotService.save(statsSnapshot);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(statsSnapshot.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteStatsSnapshot(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = statsSnapshotService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateStatsSnapshot(
            @RequestBody StatsSnapshotUpdateRequest statsSnapshotUpdateRequest) {
        if (statsSnapshotUpdateRequest == null
                || statsSnapshotUpdateRequest.getId() == null
                || statsSnapshotUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        StatsSnapshot statsSnapshot = toStatsSnapshot(statsSnapshotUpdateRequest);
        statsSnapshotService.validStatsSnapshot(statsSnapshot, false);
        boolean result = statsSnapshotService.updateById(statsSnapshot);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editStatsSnapshot(
            @RequestBody StatsSnapshotEditRequest statsSnapshotEditRequest) {
        if (statsSnapshotEditRequest == null
                || statsSnapshotEditRequest.getId() == null
                || statsSnapshotEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        StatsSnapshot statsSnapshot = new StatsSnapshot();
        statsSnapshot.setId(statsSnapshotEditRequest.getId());
        statsSnapshot.setMetric_value(statsSnapshotEditRequest.getMetricValue());
        statsSnapshot.setDimension_json(statsSnapshotEditRequest.getDimensionJson());
        statsSnapshotService.validStatsSnapshot(statsSnapshot, false);
        boolean result = statsSnapshotService.updateById(statsSnapshot);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<StatsSnapshot> getStatsSnapshotById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        StatsSnapshot statsSnapshot = statsSnapshotService.getById(id);
        ThrowUtils.throwIf(statsSnapshot == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(statsSnapshot);
    }

    @GetMapping("/get/vo")
    public BaseResponse<StatsSnapshotVO> getStatsSnapshotVOById(long id) {
        BaseResponse<StatsSnapshot> response = getStatsSnapshotById(id);
        return ResultUtils.success(statsSnapshotService.getStatsSnapshotVO(response.getData()));
    }

    @PostMapping("/list")
    public BaseResponse<List<StatsSnapshot>> listStatsSnapshot(
            @RequestBody StatsSnapshotQueryRequest statsSnapshotQueryRequest) {
        if (statsSnapshotQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(
                statsSnapshotService.list(
                        statsSnapshotService.getQueryWrapper(statsSnapshotQueryRequest)));
    }

    @PostMapping("/list/vo")
    public BaseResponse<List<StatsSnapshotVO>> listStatsSnapshotVO(
            @RequestBody StatsSnapshotQueryRequest statsSnapshotQueryRequest) {
        BaseResponse<List<StatsSnapshot>> response = listStatsSnapshot(statsSnapshotQueryRequest);
        return ResultUtils.success(statsSnapshotService.getStatsSnapshotVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<StatsSnapshot>> listStatsSnapshotByPage(
            @RequestBody StatsSnapshotQueryRequest statsSnapshotQueryRequest) {
        if (statsSnapshotQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = statsSnapshotQueryRequest.getCurrent();
        long size = statsSnapshotQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<StatsSnapshot> statsSnapshotPage =
                statsSnapshotService.page(
                        new Page<>(current, size),
                        statsSnapshotService.getQueryWrapper(statsSnapshotQueryRequest));
        return ResultUtils.success(statsSnapshotPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<StatsSnapshotVO>> listStatsSnapshotVOByPage(
            @RequestBody StatsSnapshotQueryRequest statsSnapshotQueryRequest) {
        BaseResponse<Page<StatsSnapshot>> response =
                listStatsSnapshotByPage(statsSnapshotQueryRequest);
        Page<StatsSnapshot> statsSnapshotPage = response.getData();
        Page<StatsSnapshotVO> statsSnapshotVOPage =
                new Page<>(
                        statsSnapshotPage.getCurrent(),
                        statsSnapshotPage.getSize(),
                        statsSnapshotPage.getTotal());
        statsSnapshotVOPage.setRecords(
                statsSnapshotService.getStatsSnapshotVO(statsSnapshotPage.getRecords()));
        return ResultUtils.success(statsSnapshotVOPage);
    }

    private StatsSnapshot toStatsSnapshot(StatsSnapshotAddRequest request) {
        StatsSnapshot statsSnapshot = new StatsSnapshot();
        statsSnapshot.setGranularity(request.getGranularity());
        statsSnapshot.setSnapshot_date(request.getSnapshotDate());
        statsSnapshot.setSnapshot_hour(request.getSnapshotHour());
        statsSnapshot.setVenue_id(request.getVenueId());
        statsSnapshot.setMetric_type(request.getMetricType());
        statsSnapshot.setMetric_key(request.getMetricKey());
        statsSnapshot.setMetric_value(request.getMetricValue());
        statsSnapshot.setDimension_json(request.getDimensionJson());
        return statsSnapshot;
    }

    private StatsSnapshot toStatsSnapshot(StatsSnapshotUpdateRequest request) {
        StatsSnapshot statsSnapshot = new StatsSnapshot();
        statsSnapshot.setId(request.getId());
        statsSnapshot.setGranularity(request.getGranularity());
        statsSnapshot.setSnapshot_date(request.getSnapshotDate());
        statsSnapshot.setSnapshot_hour(request.getSnapshotHour());
        statsSnapshot.setVenue_id(request.getVenueId());
        statsSnapshot.setMetric_type(request.getMetricType());
        statsSnapshot.setMetric_key(request.getMetricKey());
        statsSnapshot.setMetric_value(request.getMetricValue());
        statsSnapshot.setDimension_json(request.getDimensionJson());
        return statsSnapshot;
    }
}
