package com.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.cameramaintenancelog.CameraMaintenanceLogAddRequest;
import com.springboot.model.dto.cameramaintenancelog.CameraMaintenanceLogEditRequest;
import com.springboot.model.dto.cameramaintenancelog.CameraMaintenanceLogQueryRequest;
import com.springboot.model.dto.cameramaintenancelog.CameraMaintenanceLogUpdateRequest;
import com.springboot.model.entity.CameraMaintenanceLog;
import com.springboot.model.vo.CameraMaintenanceLogVO;
import com.springboot.service.CameraMaintenanceLogService;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cameras/maintenance")
public class CameraMaintenanceController {

    @Resource
    private CameraMaintenanceLogService cameraMaintenanceLogService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addCameraMaintenanceLog(
            @RequestBody CameraMaintenanceLogAddRequest cameraMaintenanceLogAddRequest) {
        if (cameraMaintenanceLogAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CameraMaintenanceLog cameraMaintenanceLog = toCameraMaintenanceLog(cameraMaintenanceLogAddRequest);
        cameraMaintenanceLog.setMaintained_at(
                cameraMaintenanceLog.getMaintained_at() == null ? new Date() : cameraMaintenanceLog.getMaintained_at());
        cameraMaintenanceLogService.validCameraMaintenanceLog(cameraMaintenanceLog, true);
        boolean result = cameraMaintenanceLogService.save(cameraMaintenanceLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(cameraMaintenanceLog.getId());
    }

    @PostMapping("/{cameraId}")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addCameraMaintenanceLogByCamera(@PathVariable("cameraId") Long cameraId,
            @RequestBody CameraMaintenanceLogAddRequest request) {
        CameraMaintenanceLogAddRequest addRequest = request == null ? new CameraMaintenanceLogAddRequest() : request;
        addRequest.setCameraId(cameraId);
        return addCameraMaintenanceLog(addRequest);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteCameraMaintenanceLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<CameraMaintenanceLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", deleteRequest.getId());
        CameraMaintenanceLog oldCameraMaintenanceLog = cameraMaintenanceLogService.getOne(queryWrapper);
        ThrowUtils.throwIf(oldCameraMaintenanceLog == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = cameraMaintenanceLogService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateCameraMaintenanceLog(
            @RequestBody CameraMaintenanceLogUpdateRequest cameraMaintenanceLogUpdateRequest) {
        if (cameraMaintenanceLogUpdateRequest == null || cameraMaintenanceLogUpdateRequest.getId() == null
                || cameraMaintenanceLogUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CameraMaintenanceLog cameraMaintenanceLog = toCameraMaintenanceLog(cameraMaintenanceLogUpdateRequest);
        cameraMaintenanceLogService.validCameraMaintenanceLog(cameraMaintenanceLog, false);
        boolean result = cameraMaintenanceLogService.updateById(cameraMaintenanceLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editCameraMaintenanceLog(
            @RequestBody CameraMaintenanceLogEditRequest cameraMaintenanceLogEditRequest) {
        if (cameraMaintenanceLogEditRequest == null || cameraMaintenanceLogEditRequest.getId() == null
                || cameraMaintenanceLogEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CameraMaintenanceLog cameraMaintenanceLog = new CameraMaintenanceLog();
        cameraMaintenanceLog.setId(cameraMaintenanceLogEditRequest.getId());
        cameraMaintenanceLog.setMaintenance_type(cameraMaintenanceLogEditRequest.getMaintenanceType());
        cameraMaintenanceLog.setMaintenance_content(cameraMaintenanceLogEditRequest.getMaintenanceContent());
        cameraMaintenanceLog.setMaintained_by(cameraMaintenanceLogEditRequest.getMaintainedBy());
        cameraMaintenanceLog.setMaintained_at(cameraMaintenanceLogEditRequest.getMaintainedAt());
        cameraMaintenanceLog.setNext_maintenance_at(cameraMaintenanceLogEditRequest.getNextMaintenanceAt());
        cameraMaintenanceLogService.validCameraMaintenanceLog(cameraMaintenanceLog, false);
        boolean result = cameraMaintenanceLogService.updateById(cameraMaintenanceLog);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<CameraMaintenanceLog> getCameraMaintenanceLogById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<CameraMaintenanceLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        CameraMaintenanceLog cameraMaintenanceLog = cameraMaintenanceLogService.getOne(queryWrapper);
        ThrowUtils.throwIf(cameraMaintenanceLog == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(cameraMaintenanceLog);
    }

    @GetMapping("/get/vo")
    public BaseResponse<CameraMaintenanceLogVO> getCameraMaintenanceLogVOById(long id) {
        BaseResponse<CameraMaintenanceLog> response = getCameraMaintenanceLogById(id);
        return ResultUtils.success(cameraMaintenanceLogService.getCameraMaintenanceLogVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<CameraMaintenanceLog>> listCameraMaintenanceLogByPage(
            @RequestBody CameraMaintenanceLogQueryRequest cameraMaintenanceLogQueryRequest) {
        if (cameraMaintenanceLogQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = cameraMaintenanceLogQueryRequest.getCurrent();
        long size = cameraMaintenanceLogQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<CameraMaintenanceLog> cameraMaintenanceLogPage = cameraMaintenanceLogService.page(new Page<>(current, size),
                cameraMaintenanceLogService.getQueryWrapper(cameraMaintenanceLogQueryRequest));
        return ResultUtils.success(cameraMaintenanceLogPage);
    }

    @GetMapping("/{cameraId}")
    public BaseResponse<Page<CameraMaintenanceLogVO>> listByCamera(@PathVariable("cameraId") Long cameraId,
            @RequestParam(value = "current", required = false, defaultValue = "1") long current,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") long pageSize) {
        CameraMaintenanceLogQueryRequest queryRequest = new CameraMaintenanceLogQueryRequest();
        queryRequest.setCameraId(cameraId);
        queryRequest.setCurrent((int) current);
        queryRequest.setPageSize((int) pageSize);
        return listCameraMaintenanceLogVOByPage(queryRequest);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<CameraMaintenanceLogVO>> listCameraMaintenanceLogVOByPage(
            @RequestBody CameraMaintenanceLogQueryRequest cameraMaintenanceLogQueryRequest) {
        BaseResponse<Page<CameraMaintenanceLog>> response = listCameraMaintenanceLogByPage(cameraMaintenanceLogQueryRequest);
        Page<CameraMaintenanceLog> cameraMaintenanceLogPage = response.getData();
        Page<CameraMaintenanceLogVO> cameraMaintenanceLogVOPage = new Page<>(cameraMaintenanceLogPage.getCurrent(),
                cameraMaintenanceLogPage.getSize(), cameraMaintenanceLogPage.getTotal());
        List<CameraMaintenanceLogVO> cameraMaintenanceLogVOList = cameraMaintenanceLogService
                .getCameraMaintenanceLogVO(cameraMaintenanceLogPage.getRecords());
        cameraMaintenanceLogVOPage.setRecords(cameraMaintenanceLogVOList);
        return ResultUtils.success(cameraMaintenanceLogVOPage);
    }

    private CameraMaintenanceLog toCameraMaintenanceLog(CameraMaintenanceLogAddRequest request) {
        CameraMaintenanceLog cameraMaintenanceLog = new CameraMaintenanceLog();
        cameraMaintenanceLog.setCamera_id(request.getCameraId());
        cameraMaintenanceLog.setMaintenance_type(request.getMaintenanceType());
        cameraMaintenanceLog.setMaintenance_content(request.getMaintenanceContent());
        cameraMaintenanceLog.setMaintained_by(request.getMaintainedBy());
        cameraMaintenanceLog.setMaintained_at(request.getMaintainedAt());
        cameraMaintenanceLog.setNext_maintenance_at(request.getNextMaintenanceAt());
        return cameraMaintenanceLog;
    }

    private CameraMaintenanceLog toCameraMaintenanceLog(CameraMaintenanceLogUpdateRequest request) {
        CameraMaintenanceLog cameraMaintenanceLog = new CameraMaintenanceLog();
        cameraMaintenanceLog.setId(request.getId());
        cameraMaintenanceLog.setCamera_id(request.getCameraId());
        cameraMaintenanceLog.setMaintenance_type(request.getMaintenanceType());
        cameraMaintenanceLog.setMaintenance_content(request.getMaintenanceContent());
        cameraMaintenanceLog.setMaintained_by(request.getMaintainedBy());
        cameraMaintenanceLog.setMaintained_at(request.getMaintainedAt());
        cameraMaintenanceLog.setNext_maintenance_at(request.getNextMaintenanceAt());
        return cameraMaintenanceLog;
    }
}
