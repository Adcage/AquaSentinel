package com.springboot.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.exception.ThrowUtils;
import com.springboot.model.dto.monitor.MonitorTaskControlRequest;
import com.springboot.model.dto.monitor.StartMonitorTaskRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.CameraDevice;
import com.springboot.service.AiEngineClient;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.MonitorRealtimeQueryService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/tasks")
public class MonitorTaskController {

    private static final Logger log = LoggerFactory.getLogger(MonitorTaskController.class);

    private static final long AUTO_START_COOLDOWN_MS = 15_000L;

    private final Map<Long, Long> cameraAutoStartCooldownUntilMap = new ConcurrentHashMap<>();

    @Resource private AiStreamTaskService aiStreamTaskService;

    @Resource private AiEngineClient aiEngineClient;

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private MonitorRealtimeQueryService monitorRealtimeQueryService;

    private Map<String, Object> buildCameraSite(Long cameraId) {
        if (cameraId == null) {
            return new HashMap<>();
        }
        CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
        if (cameraDevice == null) {
            return new HashMap<>();
        }
        Map<String, Object> cameraSite = new HashMap<>();
        cameraSite.put("cameraId", cameraDevice.getId());
        cameraSite.put("cameraCode", cameraDevice.getCamera_code());
        cameraSite.put("venueId", cameraDevice.getVenue_id());
        cameraSite.put("zoneId", cameraDevice.getZone_id());
        return cameraSite;
    }

    private boolean hasRunningTaskByCamera(Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            return false;
        }
        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("camera_id", cameraId);
        queryWrapper.in("task_status", "RUNNING", "STARTING");
        queryWrapper.last("limit 1");
        return aiStreamTaskService.getOne(queryWrapper) != null;
    }

    private boolean isCameraActive(CameraDevice cameraDevice) {
        if (cameraDevice == null || cameraDevice.getId() == null || cameraDevice.getId() <= 0) {
            return false;
        }
        if (Integer.valueOf(1).equals(cameraDevice.getIs_delete())) {
            return false;
        }
        return Integer.valueOf(1).equals(cameraDevice.getEnabled());
    }

    @Scheduled(initialDelay = 10_000L, fixedDelay = 60_000L)
    public void recoverTasksInBackground() {
        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("task_status", "RUNNING", "STARTING");
        queryWrapper.orderByDesc("updated_at");
        List<AiStreamTask> runningTasks = aiStreamTaskService.list(queryWrapper);
        if (runningTasks == null || runningTasks.isEmpty()) {
            return;
        }

        Map<Long, AiStreamTask> latestTaskByCamera = new LinkedHashMap<>();
        for (AiStreamTask task : runningTasks) {
            if (task == null || task.getCamera_id() == null || task.getCamera_id() <= 0) {
                continue;
            }
            latestTaskByCamera.putIfAbsent(task.getCamera_id(), task);
        }
        for (Map.Entry<Long, AiStreamTask> entry : latestTaskByCamera.entrySet()) {
            try {
                CameraDevice cameraDevice = cameraDeviceService.getById(entry.getKey());
                if (!isCameraActive(cameraDevice)) {
                    try {
                        aiStreamTaskService.stopTask(entry.getValue().getTask_code());
                    } catch (Exception ignored) {
                    }
                    continue;
                }
                monitorRealtimeQueryService.buildRealtimeDataByCamera(
                        entry.getKey(), entry.getValue());
            } catch (Exception ignored) {
            }
        }
    }

    @Scheduled(initialDelay = 8_000L, fixedDelay = 15_000L)
    public void ensureTasksForEnabledCameras() {
        QueryWrapper<CameraDevice> cameraQuery = new QueryWrapper<>();
        cameraQuery.eq("enabled", 1);
        cameraQuery.eq("is_delete", 0);
        List<CameraDevice> enabledCameras = cameraDeviceService.list(cameraQuery);
        if (enabledCameras == null || enabledCameras.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (CameraDevice cameraDevice : enabledCameras) {
            if (cameraDevice == null || cameraDevice.getId() == null || cameraDevice.getId() <= 0) {
                continue;
            }
            if (StringUtils.isBlank(cameraDevice.getStream_url())) {
                continue;
            }
            Long cooldownUntil = cameraAutoStartCooldownUntilMap.get(cameraDevice.getId());
            if (cooldownUntil != null && cooldownUntil > now) {
                continue;
            }
            if (hasRunningTaskByCamera(cameraDevice.getId())) {
                cameraAutoStartCooldownUntilMap.remove(cameraDevice.getId());
                continue;
            }

            try {
                StartMonitorTaskRequest request = new StartMonitorTaskRequest();
                request.setCameraId(cameraDevice.getId());
                aiStreamTaskService.startTask(request);
                cameraAutoStartCooldownUntilMap.remove(cameraDevice.getId());
            } catch (Exception e) {
                cameraAutoStartCooldownUntilMap.put(
                        cameraDevice.getId(), now + AUTO_START_COOLDOWN_MS);
                log.warn("auto start monitor task failed, cameraId={}", cameraDevice.getId(), e);
            }
        }
    }

    @PostMapping("/start")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<AiStreamTask> startTask(@RequestBody StartMonitorTaskRequest request) {
        if (request == null || request.getCameraId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraId不能为空");
        }
        return ResultUtils.success(aiStreamTaskService.startTask(request));
    }

    @PostMapping("/stop")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> stopTask(@RequestBody MonitorTaskControlRequest request) {
        if (request == null || StringUtils.isBlank(request.getTaskCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskCode不能为空");
        }
        boolean result = aiStreamTaskService.stopTask(request.getTaskCode());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getTaskByCode(@RequestParam String taskCode) {
        if (StringUtils.isBlank(taskCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskCode不能为空");
        }
        AiStreamTask localTask = aiStreamTaskService.getTaskByCode(taskCode);
        Map<String, Object> engineTask = aiEngineClient.getTask(taskCode);
        engineTask.put("cameraSite", buildCameraSite(localTask.getCamera_id()));
        Map<String, Object> data = new HashMap<>();
        data.put("local", localTask);
        data.put("engine", engineTask);
        return ResultUtils.success(data);
    }

    @GetMapping("/realtime/by-camera")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getTaskRealtimeByCamera(@RequestParam Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraId不能为空");
        }
        return ResultUtils.success(monitorRealtimeQueryService.buildRealtimeDataByCamera(cameraId));
    }

    @GetMapping("/realtime/batch")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getTaskRealtimeBatch(@RequestParam String cameraIds) {
        Set<Long> uniqueCameraIds = monitorRealtimeQueryService.parseCameraIds(cameraIds);
        if (uniqueCameraIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraIds不能为空");
        }
        return ResultUtils.success(monitorRealtimeQueryService.buildRealtimeBatch(uniqueCameraIds));
    }

    @GetMapping("/engine/health")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getEngineHealth() {
        Map<String, Object> data = new HashMap<>();
        try {
            Map<String, Object> engineHealth = aiEngineClient.healthCheck();
            data.put("available", true);
            data.put("engine", engineHealth);
            data.put("message", "AI引擎运行正常");
        } catch (Exception e) {
            data.put("available", false);
            data.put("engine", null);
            data.put("message", "AI引擎不可用，请启动Python服务");
        }
        return ResultUtils.success(data);
    }

    @GetMapping("/{taskCode}")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Map<String, Object>> getTaskByPath(
            @PathVariable("taskCode") String taskCode) {
        return getTaskByCode(taskCode);
    }
}
