package com.springboot.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.springboot.model.dto.monitor.StartMonitorTaskRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.CameraDevice;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MonitorRealtimeQueryService {

    private static final Logger log = LoggerFactory.getLogger(MonitorRealtimeQueryService.class);

    private static final int REALTIME_ENGINE_TIMEOUT_MS = 8000;

    private static final long ENGINE_COOLDOWN_MS = 2_000L;

    private static final long CAMERA_SITE_CACHE_TTL_MS = 60_000L;

    private final Map<Long, Long> cameraCooldownUntilMap = new ConcurrentHashMap<>();

    private final Map<Long, Object[]> cameraSiteCache = new ConcurrentHashMap<>();

    @Resource private AiStreamTaskService aiStreamTaskService;

    @Resource private AiEngineClient aiEngineClient;

    @Resource private CameraDeviceService cameraDeviceService;

    private Map<String, Object> buildCameraSite(Long cameraId) {
        if (cameraId == null) {
            return new HashMap<>();
        }
        long now = System.currentTimeMillis();
        Object[] cached = cameraSiteCache.get(cameraId);
        if (cached != null && (Long) cached[1] > now) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hit = (Map<String, Object>) cached[0];
            return hit;
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
        cameraSiteCache.put(cameraId, new Object[] {cameraSite, now + CAMERA_SITE_CACHE_TTL_MS});
        return cameraSite;
    }

    private boolean isTaskNotFoundFromEngine(Exception e) {
        if (e == null || StringUtils.isBlank(e.getMessage())) {
            return false;
        }
        return StringUtils.containsIgnoreCase(e.getMessage(), "HTTP 404");
    }

    private StartMonitorTaskRequest buildAutoRestartRequest(AiStreamTask task) {
        StartMonitorTaskRequest restartRequest = new StartMonitorTaskRequest();
        restartRequest.setCameraId(task.getCamera_id());
        restartRequest.setFrameIntervalMs(task.getFrame_interval_ms());
        restartRequest.setCallbackUrl(task.getCallback_url());
        return restartRequest;
    }

    private AiStreamTask pickTaskByCamera(Long cameraId) {
        QueryWrapper<AiStreamTask> runningQuery = new QueryWrapper<>();
        runningQuery.eq("camera_id", cameraId);
        runningQuery.in("task_status", "RUNNING", "STARTING");
        runningQuery.orderByDesc("updated_at");
        runningQuery.last("limit 1");
        AiStreamTask pickedTask = aiStreamTaskService.getOne(runningQuery);
        if (pickedTask != null) {
            return pickedTask;
        }
        QueryWrapper<AiStreamTask> latestQuery = new QueryWrapper<>();
        latestQuery.eq("camera_id", cameraId);
        latestQuery.orderByDesc("updated_at");
        latestQuery.last("limit 1");
        return aiStreamTaskService.getOne(latestQuery);
    }

    private Map<String, Object> buildEngineFallback(Long cameraId, String message) {
        Map<String, Object> engineFallback = new HashMap<>();
        engineFallback.put("available", false);
        engineFallback.put("message", message);
        engineFallback.put("cameraSite", buildCameraSite(cameraId));
        return engineFallback;
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

    public Map<String, Object> buildRealtimeDataByCamera(Long cameraId) {
        return buildRealtimeDataByCamera(cameraId, null);
    }

    public Map<String, Object> buildRealtimeDataByCamera(Long cameraId, AiStreamTask knownTask) {
        Map<String, Object> data = new HashMap<>();
        AiStreamTask pickedTask = (knownTask != null) ? knownTask : pickTaskByCamera(cameraId);
        if (pickedTask == null) {
            data.put("local", null);
            data.put("engine", null);
            return data;
        }

        data.put("local", pickedTask);
        if (StringUtils.isBlank(pickedTask.getTask_code())) {
            data.put("engine", null);
            return data;
        }

        long now = System.currentTimeMillis();
        Long cooldownUntil = cameraCooldownUntilMap.get(cameraId);
        if (cooldownUntil != null && cooldownUntil > now) {
            data.put("engine", buildEngineFallback(cameraId, "AI引擎查询冷却中"));
            return data;
        }

        try {
            Map<String, Object> engineTask =
                    aiEngineClient.getTask(pickedTask.getTask_code(), REALTIME_ENGINE_TIMEOUT_MS);
            engineTask.put("cameraSite", buildCameraSite(cameraId));
            data.put("engine", engineTask);
            cameraCooldownUntilMap.remove(cameraId);
            return data;
        } catch (Exception e) {
            if (isTaskNotFoundFromEngine(e)) {
                try {
                    try {
                        aiStreamTaskService.stop(pickedTask.getTask_code());
                    } catch (Exception ignoreStopError) {
                        log.debug(
                                "mark stale task stopped failed, taskCode={}",
                                pickedTask.getTask_code(),
                                ignoreStopError);
                    }
                    CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
                    if (!isCameraActive(cameraDevice)) {
                        log.info(
                                "skip auto rebuild because camera inactive, cameraId={}, enabled={}, isDelete={}",
                                cameraId,
                                cameraDevice == null ? null : cameraDevice.getEnabled(),
                                cameraDevice == null ? null : cameraDevice.getIs_delete());
                        data.put("engine", buildEngineFallback(cameraId, "摄像头已删除或停用"));
                        return data;
                    }
                    log.info(
                            "auto rebuilding engine task, cameraId={}, staleTaskCode={}",
                            cameraId,
                            pickedTask.getTask_code());
                    AiStreamTask restartedTask =
                            aiStreamTaskService.startTask(buildAutoRestartRequest(pickedTask));
                    data.put("local", restartedTask);
                    Map<String, Object> engineTask =
                            aiEngineClient.getTask(
                                    restartedTask.getTask_code(), REALTIME_ENGINE_TIMEOUT_MS);
                    engineTask.put("cameraSite", buildCameraSite(cameraId));
                    engineTask.put("autoRestarted", true);
                    data.put("engine", engineTask);
                    cameraCooldownUntilMap.remove(cameraId);
                    return data;
                } catch (Exception restartError) {
                    log.warn(
                            "auto rebuild engine task failed, cameraId={}, staleTaskCode={}",
                            cameraId,
                            pickedTask.getTask_code(),
                            restartError);
                    cameraCooldownUntilMap.put(
                            cameraId, System.currentTimeMillis() + ENGINE_COOLDOWN_MS);
                    data.put("engine", buildEngineFallback(cameraId, "AI任务自动重建失败"));
                    return data;
                }
            }
            cameraCooldownUntilMap.put(cameraId, System.currentTimeMillis() + ENGINE_COOLDOWN_MS);
            data.put("engine", buildEngineFallback(cameraId, "AI引擎不可用"));
            return data;
        }
    }

    public Set<Long> parseCameraIds(String cameraIdsText) {
        Set<Long> uniqueCameraIds = new LinkedHashSet<>();
        if (StringUtils.isBlank(cameraIdsText)) {
            return uniqueCameraIds;
        }
        for (String part : cameraIdsText.split(",")) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                long parsed = Long.parseLong(part.trim());
                if (parsed > 0) {
                    uniqueCameraIds.add(parsed);
                }
            } catch (Exception ignored) {
            }
        }
        return uniqueCameraIds;
    }

    public Map<String, Object> buildRealtimeBatch(Set<Long> uniqueCameraIds) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (uniqueCameraIds == null || uniqueCameraIds.isEmpty()) {
            return data;
        }
        for (Long cameraId : uniqueCameraIds) {
            if (cameraId == null || cameraId <= 0) {
                continue;
            }
            data.put(String.valueOf(cameraId), buildRealtimeDataByCamera(cameraId));
        }
        return data;
    }
}
