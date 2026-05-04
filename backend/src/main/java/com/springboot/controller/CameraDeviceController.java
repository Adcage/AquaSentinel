package com.springboot.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.DeleteRequest;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.cameradevice.CameraDeviceAddRequest;
import com.springboot.model.dto.cameradevice.CameraDeviceBatchDeleteRequest;
import com.springboot.model.dto.cameradevice.CameraDeviceBatchDisableRequest;
import com.springboot.model.dto.cameradevice.CameraDeviceEditRequest;
import com.springboot.model.dto.cameradevice.CameraDeviceQueryRequest;
import com.springboot.model.dto.cameradevice.CameraDeviceUpdateRequest;
import com.springboot.model.dto.cameradevice.CameraPtzControlRequest;
import com.springboot.model.dto.monitor.StartMonitorTaskRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.vo.BatchOperateResultVO;
import com.springboot.model.vo.CameraDeviceVO;
import com.springboot.ratelimit.RateLimit;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.Esp32PtzControlService;
import com.springboot.websocket.AlertWsPublisher;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cameras")
public class CameraDeviceController {

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private AlertWsPublisher alertWsPublisher;

    @Resource private AiStreamTaskService aiStreamTaskService;

    @Resource private Esp32PtzControlService esp32PtzControlService;

    @RateLimit(
            capacity = 10,
            refillRate = 10,
            refillPeriodSeconds = 60,
            key = "camera:add",
            keyType = "USER",
            fallbackMessage = "设备操作请求过于频繁")
    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addCameraDevice(
            @RequestBody CameraDeviceAddRequest cameraDeviceAddRequest) {
        if (cameraDeviceAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CameraDevice cameraDevice = toCameraDevice(cameraDeviceAddRequest);
        cameraDevice.setProtocol(StringUtils.defaultIfBlank(cameraDevice.getProtocol(), "RTSP"));
        cameraDevice.setDevice_status(
                StringUtils.defaultIfBlank(cameraDevice.getDevice_status(), "OFFLINE"));
        cameraDevice.setHealth_status(
                StringUtils.defaultIfBlank(cameraDevice.getHealth_status(), "NORMAL"));
        cameraDevice.setEnabled(cameraDevice.getEnabled() == null ? 1 : cameraDevice.getEnabled());
        cameraDevice.setIs_delete(0);
        cameraDeviceService.validCameraDevice(cameraDevice, true);
        boolean result = cameraDeviceService.save(cameraDevice);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        publishCameraStatusChanged(
                cameraDevice.getId(),
                cameraDevice.getCamera_code(),
                cameraDevice.getDevice_status(),
                cameraDevice.getHealth_status());
        return ResultUtils.success(cameraDevice.getId());
    }

    @RateLimit(
            capacity = 10,
            refillRate = 10,
            refillPeriodSeconds = 60,
            key = "camera:delete",
            keyType = "USER",
            fallbackMessage = "设备操作请求过于频繁")
    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteCameraDevice(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", deleteRequest.getId());
        queryWrapper.eq("is_delete", 0);
        CameraDevice oldCameraDevice = cameraDeviceService.getOne(queryWrapper);
        ThrowUtils.throwIf(oldCameraDevice == null, ErrorCode.NOT_FOUND_ERROR);
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(deleteRequest.getId());
        cameraDevice.setIs_delete(1);
        boolean result = cameraDeviceService.updateById(cameraDevice);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        publishCameraStatusChanged(
                oldCameraDevice.getId(),
                oldCameraDevice.getCamera_code(),
                "DELETED",
                oldCameraDevice.getHealth_status());
        return ResultUtils.success(true);
    }

    @RateLimit(
            capacity = 10,
            refillRate = 10,
            refillPeriodSeconds = 60,
            key = "camera:update",
            keyType = "USER",
            fallbackMessage = "设备操作请求过于频繁")
    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateCameraDevice(
            @RequestBody CameraDeviceUpdateRequest cameraDeviceUpdateRequest) {
        if (cameraDeviceUpdateRequest == null
                || cameraDeviceUpdateRequest.getId() == null
                || cameraDeviceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CameraDevice old = cameraDeviceService.getById(cameraDeviceUpdateRequest.getId());
        CameraDevice cameraDevice = toCameraDevice(cameraDeviceUpdateRequest);
        cameraDeviceService.validCameraDevice(cameraDevice, false);
        boolean result = cameraDeviceService.updateById(cameraDevice);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        CameraDevice latest = cameraDeviceService.getById(cameraDevice.getId());
        if (latest != null) {
            publishCameraStatusChanged(
                    latest.getId(),
                    latest.getCamera_code(),
                    latest.getDevice_status(),
                    latest.getHealth_status());
        }
        if (old != null
                && StringUtils.isNotBlank(cameraDeviceUpdateRequest.getStreamUrl())
                && !cameraDeviceUpdateRequest.getStreamUrl().equals(old.getStream_url())) {
            restartAiTaskIfRunning(cameraDeviceUpdateRequest.getId());
        }
        return ResultUtils.success(true);
    }

    @RateLimit(
            capacity = 10,
            refillRate = 10,
            refillPeriodSeconds = 60,
            key = "camera:edit",
            keyType = "USER",
            fallbackMessage = "设备操作请求过于频繁")
    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editCameraDevice(
            @RequestBody CameraDeviceEditRequest cameraDeviceEditRequest) {
        if (cameraDeviceEditRequest == null
                || cameraDeviceEditRequest.getId() == null
                || cameraDeviceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CameraDevice old = cameraDeviceService.getById(cameraDeviceEditRequest.getId());
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(cameraDeviceEditRequest.getId());
        cameraDevice.setZone_id(normalizeZoneId(cameraDeviceEditRequest.getZoneId()));
        cameraDevice.setCamera_name(cameraDeviceEditRequest.getCameraName());
        cameraDevice.setStream_url(cameraDeviceEditRequest.getStreamUrl());
        cameraDevice.setProtocol(cameraDeviceEditRequest.getProtocol());
        cameraDevice.setDevice_status(cameraDeviceEditRequest.getDeviceStatus());
        cameraDevice.setHealth_status(cameraDeviceEditRequest.getHealthStatus());
        cameraDevice.setEnabled(cameraDeviceEditRequest.getEnabled());
        cameraDevice.setLast_heartbeat_at(cameraDeviceEditRequest.getLastHeartbeatAt());
        cameraDeviceService.validCameraDevice(cameraDevice, false);
        boolean result = cameraDeviceService.updateById(cameraDevice);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        CameraDevice latest = cameraDeviceService.getById(cameraDevice.getId());
        if (latest != null) {
            publishCameraStatusChanged(
                    latest.getId(),
                    latest.getCamera_code(),
                    latest.getDevice_status(),
                    latest.getHealth_status());
        }
        if (old != null
                && StringUtils.isNotBlank(cameraDeviceEditRequest.getStreamUrl())
                && !cameraDeviceEditRequest.getStreamUrl().equals(old.getStream_url())) {
            restartAiTaskIfRunning(cameraDeviceEditRequest.getId());
        }
        return ResultUtils.success(true);
    }

    @RateLimit(
            capacity = 5,
            refillRate = 5,
            refillPeriodSeconds = 60,
            key = "camera:batch:disable",
            keyType = "USER",
            fallbackMessage = "设备批量禁用请求过于频繁")
    @PostMapping("/batch/disable")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<BatchOperateResultVO> batchDisableCameraDevices(
            @RequestBody CameraDeviceBatchDisableRequest request) {
        if (request == null || request.getCameraIds() == null || request.getCameraIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备ID列表不能为空");
        }
        if (request.getCameraIds().size() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单次最多处理200台设备");
        }
        BatchOperateResultVO result =
                cameraDeviceService.batchDisableCameraDevices(request.getCameraIds());
        for (Long successId : result.getSuccessIds()) {
            CameraDevice latest = cameraDeviceService.getById(successId);
            if (latest != null) {
                publishCameraStatusChanged(
                        latest.getId(),
                        latest.getCamera_code(),
                        latest.getDevice_status(),
                        latest.getHealth_status());
            }
        }
        return ResultUtils.success(result);
    }

    @RateLimit(
            capacity = 10,
            refillRate = 10,
            refillPeriodSeconds = 60,
            key = "camera:batch:delete",
            keyType = "USER",
            fallbackMessage = "设备批量删除请求过于频繁")
    @PostMapping("/batch/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<BatchOperateResultVO> batchDeleteCameraDevices(
            @RequestBody CameraDeviceBatchDeleteRequest request) {
        if (request == null || request.getCameraIds() == null || request.getCameraIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备ID列表不能为空");
        }
        if (request.getCameraIds().size() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单次最多处理200台设备");
        }
        BatchOperateResultVO result =
                cameraDeviceService.batchDeleteCameraDevices(request.getCameraIds());
        return ResultUtils.success(result);
    }

    @PostMapping("/control/ptz")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> controlPtz(
            @RequestBody CameraPtzControlRequest request) {
        if (request == null || request.getCameraId() == null || request.getCameraId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraId 不能为空");
        }
        CameraDevice cameraDevice = cameraDeviceService.getById(request.getCameraId());
        ThrowUtils.throwIf(cameraDevice == null, ErrorCode.NOT_FOUND_ERROR, "设备不存在");
        ThrowUtils.throwIf(
                cameraDevice.getEnabled() != null && cameraDevice.getEnabled() == 0,
                ErrorCode.OPERATION_ERROR,
                "设备未启用，无法控制");
        Map<String, Object> result = esp32PtzControlService.control(cameraDevice, request);
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    public BaseResponse<CameraDevice> getCameraDeviceById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("is_delete", 0);
        CameraDevice cameraDevice = cameraDeviceService.getOne(queryWrapper);
        ThrowUtils.throwIf(cameraDevice == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(cameraDevice);
    }

    @GetMapping("/get/vo")
    public BaseResponse<CameraDeviceVO> getCameraDeviceVOById(long id) {
        BaseResponse<CameraDevice> response = getCameraDeviceById(id);
        return ResultUtils.success(cameraDeviceService.getCameraDeviceVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<CameraDevice>> listCameraDeviceByPage(
            @RequestBody CameraDeviceQueryRequest cameraDeviceQueryRequest) {
        if (cameraDeviceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = cameraDeviceQueryRequest.getCurrent();
        long size = cameraDeviceQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<CameraDevice> cameraDevicePage =
                cameraDeviceService.page(
                        new Page<>(current, size),
                        cameraDeviceService.getQueryWrapper(cameraDeviceQueryRequest));
        return ResultUtils.success(cameraDevicePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<CameraDeviceVO>> listCameraDeviceVOByPage(
            @RequestBody CameraDeviceQueryRequest cameraDeviceQueryRequest) {
        BaseResponse<Page<CameraDevice>> response =
                listCameraDeviceByPage(cameraDeviceQueryRequest);
        Page<CameraDevice> cameraDevicePage = response.getData();
        Page<CameraDeviceVO> cameraDeviceVOPage =
                new Page<>(
                        cameraDevicePage.getCurrent(),
                        cameraDevicePage.getSize(),
                        cameraDevicePage.getTotal());
        List<CameraDeviceVO> cameraDeviceVOList =
                cameraDeviceService.getCameraDeviceVO(cameraDevicePage.getRecords());
        cameraDeviceVOPage.setRecords(cameraDeviceVOList);
        return ResultUtils.success(cameraDeviceVOPage);
    }

    private CameraDevice toCameraDevice(CameraDeviceAddRequest request) {
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setVenue_id(request.getVenueId());
        cameraDevice.setZone_id(normalizeZoneId(request.getZoneId()));
        cameraDevice.setCamera_code(request.getCameraCode());
        cameraDevice.setCamera_name(request.getCameraName());
        cameraDevice.setStream_url(request.getStreamUrl());
        cameraDevice.setProtocol(request.getProtocol());
        cameraDevice.setDevice_status(request.getDeviceStatus());
        cameraDevice.setHealth_status(request.getHealthStatus());
        cameraDevice.setEnabled(request.getEnabled());
        cameraDevice.setLast_heartbeat_at(request.getLastHeartbeatAt());
        return cameraDevice;
    }

    private CameraDevice toCameraDevice(CameraDeviceUpdateRequest request) {
        CameraDevice cameraDevice = new CameraDevice();
        cameraDevice.setId(request.getId());
        cameraDevice.setVenue_id(request.getVenueId());
        cameraDevice.setZone_id(normalizeZoneId(request.getZoneId()));
        cameraDevice.setCamera_code(request.getCameraCode());
        cameraDevice.setCamera_name(request.getCameraName());
        cameraDevice.setStream_url(request.getStreamUrl());
        cameraDevice.setProtocol(request.getProtocol());
        cameraDevice.setDevice_status(request.getDeviceStatus());
        cameraDevice.setHealth_status(request.getHealthStatus());
        cameraDevice.setEnabled(request.getEnabled());
        cameraDevice.setLast_heartbeat_at(request.getLastHeartbeatAt());
        return cameraDevice;
    }

    private Long normalizeZoneId(Long zoneId) {
        if (zoneId == null || zoneId <= 0) {
            return null;
        }
        return zoneId;
    }

    private void restartAiTaskIfRunning(Long cameraId) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiStreamTask> q =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            q.eq("camera_id", cameraId);
            q.in("task_status", "RUNNING", "STARTING");
            q.orderByDesc("updated_at");
            q.last("limit 1");
            AiStreamTask running = aiStreamTaskService.getOne(q);
            if (running == null) {
                return;
            }
            try {
                aiStreamTaskService.stopTask(running.getTask_code());
            } catch (Exception ignored) {
            }
            StartMonitorTaskRequest restartReq = new StartMonitorTaskRequest();
            restartReq.setCameraId(cameraId);
            restartReq.setFrameIntervalMs(running.getFrame_interval_ms());
            restartReq.setCallbackUrl(running.getCallback_url());
            aiStreamTaskService.startTask(restartReq);
        } catch (Exception e) {
            // 自动重启失败不影响主流程
        }
    }

    private void publishCameraStatusChanged(
            Long cameraId, String cameraCode, String deviceStatus, String healthStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("cameraId", cameraId);
        data.put("cameraCode", cameraCode);
        data.put("deviceStatus", deviceStatus);
        data.put("healthStatus", healthStatus);
        alertWsPublisher.publishCameraStatusChanged(
                "camera-status-" + cameraId + "-" + System.currentTimeMillis(), data);
    }
}
