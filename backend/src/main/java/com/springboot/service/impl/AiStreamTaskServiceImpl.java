package com.springboot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.config.AppAiEngineProperties;
import com.springboot.constant.CommonConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.monitor.StartMonitorTaskRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.entity.CameraDevice;
import com.springboot.model.dto.aistreamtask.AiStreamTaskQueryRequest;
import com.springboot.model.vo.AiStreamTaskVO;
import com.springboot.service.AiEngineClient;
import com.springboot.service.AiStreamTaskService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.SystemNoticeConfigService;
import com.springboot.mapper.AiStreamTaskMapper;
import com.springboot.utils.SqlUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
* @description 针对表【ai_stream_task(AI流任务表)】的数据库操作Service实现
*/
@Service
public class AiStreamTaskServiceImpl extends ServiceImpl<AiStreamTaskMapper, AiStreamTask>
    implements AiStreamTaskService{

    private final CameraDeviceService cameraDeviceService;

    private final AiEngineClient aiEngineClient;

    private final AppAiEngineProperties appAiEngineProperties;

    private final SystemNoticeConfigService systemNoticeConfigService;

    public AiStreamTaskServiceImpl(CameraDeviceService cameraDeviceService, AiEngineClient aiEngineClient,
                                   AppAiEngineProperties appAiEngineProperties,
                                   SystemNoticeConfigService systemNoticeConfigService) {
        this.cameraDeviceService = cameraDeviceService;
        this.aiEngineClient = aiEngineClient;
        this.appAiEngineProperties = appAiEngineProperties;
        this.systemNoticeConfigService = systemNoticeConfigService;
    }

    @Override
    public void validAiStreamTask(AiStreamTask aiStreamTask, boolean add) {
        if (aiStreamTask == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不能为空");
        }
        if (add && StringUtils.isBlank(aiStreamTask.getTask_code())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务编码不能为空");
        }
        if (add && aiStreamTask.getCamera_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头ID不能为空");
        }
        if (add && StringUtils.isBlank(aiStreamTask.getStream_url())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "流地址不能为空");
        }
        if (StringUtils.isNotBlank(aiStreamTask.getTask_code()) && aiStreamTask.getTask_code().length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务编码过长");
        }
        if (StringUtils.isNotBlank(aiStreamTask.getTask_code())) {
            QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_code", aiStreamTask.getTask_code());
            queryWrapper.ne(aiStreamTask.getId() != null, "id", aiStreamTask.getId());
            Long count = this.baseMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务编码已存在");
            }
        }
    }

    @Override
    public QueryWrapper<AiStreamTask> getQueryWrapper(AiStreamTaskQueryRequest aiStreamTaskQueryRequest) {
        if (aiStreamTaskQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(aiStreamTaskQueryRequest.getId() != null, "id", aiStreamTaskQueryRequest.getId());
        queryWrapper.eq(StringUtils.isNotBlank(aiStreamTaskQueryRequest.getTaskCode()), "task_code",
                aiStreamTaskQueryRequest.getTaskCode());
        queryWrapper.eq(aiStreamTaskQueryRequest.getCameraId() != null, "camera_id", aiStreamTaskQueryRequest.getCameraId());
        queryWrapper.eq(StringUtils.isNotBlank(aiStreamTaskQueryRequest.getTaskStatus()), "task_status",
                aiStreamTaskQueryRequest.getTaskStatus());
        String sortField = aiStreamTaskQueryRequest.getSortField();
        String sortOrder = aiStreamTaskQueryRequest.getSortOrder();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public AiStreamTaskVO getAiStreamTaskVO(AiStreamTask aiStreamTask) {
        if (aiStreamTask == null) {
            return null;
        }
        AiStreamTaskVO aiStreamTaskVO = new AiStreamTaskVO();
        aiStreamTaskVO.setId(aiStreamTask.getId());
        aiStreamTaskVO.setTaskCode(aiStreamTask.getTask_code());
        aiStreamTaskVO.setCameraId(aiStreamTask.getCamera_id());
        aiStreamTaskVO.setStreamUrl(aiStreamTask.getStream_url());
        aiStreamTaskVO.setFrameIntervalMs(aiStreamTask.getFrame_interval_ms());
        aiStreamTaskVO.setCallbackUrl(aiStreamTask.getCallback_url());
        aiStreamTaskVO.setTaskStatus(aiStreamTask.getTask_status());
        aiStreamTaskVO.setStartedAt(aiStreamTask.getStarted_at());
        aiStreamTaskVO.setStoppedAt(aiStreamTask.getStopped_at());
        aiStreamTaskVO.setLastFrameAt(aiStreamTask.getLast_frame_at());
        aiStreamTaskVO.setCreatedAt(aiStreamTask.getCreated_at());
        aiStreamTaskVO.setUpdatedAt(aiStreamTask.getUpdated_at());
        return aiStreamTaskVO;
    }

    @Override
    public List<AiStreamTaskVO> getAiStreamTaskVO(List<AiStreamTask> aiStreamTaskList) {
        if (CollUtil.isEmpty(aiStreamTaskList)) {
            return new ArrayList<>();
        }
        return aiStreamTaskList.stream().map(this::getAiStreamTaskVO).collect(Collectors.toList());
    }

    @Override
    public AiStreamTask getTaskByCode(String taskCode) {
        if (StringUtils.isBlank(taskCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskCode不能为空");
        }
        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_code", taskCode);
        AiStreamTask aiStreamTask = this.getOne(queryWrapper);
        if (aiStreamTask == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        }
        return aiStreamTask;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiStreamTask startTask(StartMonitorTaskRequest request) {
        if (request == null || request.getCameraId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "cameraId不能为空");
        }
        CameraDevice cameraDevice = cameraDeviceService.getById(request.getCameraId());
        if (cameraDevice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "摄像头不存在");
        }
        if (Integer.valueOf(1).equals(cameraDevice.getIs_delete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "摄像头不存在");
        }
        if (!Integer.valueOf(1).equals(cameraDevice.getEnabled())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "摄像头未启用");
        }
        if (StringUtils.isBlank(cameraDevice.getStream_url())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头流地址为空");
        }

        String taskCode = StringUtils.trimToEmpty(request.getTaskCode());
        if (StringUtils.isBlank(taskCode)) {
            taskCode = String.format("TASK_CAM_%s_%d", request.getCameraId(), System.currentTimeMillis());
        }

        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_code", taskCode);
        AiStreamTask aiStreamTask = this.getOne(queryWrapper);
        if (aiStreamTask == null) {
            aiStreamTask = new AiStreamTask();
            aiStreamTask.setTask_code(taskCode);
            aiStreamTask.setCreated_at(new Date());
        } else if (StringUtils.equals(aiStreamTask.getTask_status(), "RUNNING")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务已在运行中");
        }

        Integer frameIntervalMs = request.getFrameIntervalMs() == null ? 200 : request.getFrameIntervalMs();
        Double drowningAlertThresholdSec = Double.valueOf(systemNoticeConfigService.getDrowningAlertThresholdSec());
        String engineStreamUrl = resolveEngineStreamUrl(cameraDevice);
        String displayStreamUrl = resolveDisplayStreamUrl(request.getCameraId());
        aiStreamTask.setCamera_id(request.getCameraId());
        aiStreamTask.setStream_url(engineStreamUrl);
        aiStreamTask.setFrame_interval_ms(frameIntervalMs);
        aiStreamTask.setCallback_url(StringUtils.defaultIfBlank(request.getCallbackUrl(), aiEngineClient.getDefaultCallbackUrl()));
        aiStreamTask.setTask_status("STARTING");
        aiStreamTask.setUpdated_at(new Date());

        validAiStreamTask(aiStreamTask, aiStreamTask.getId() == null);
        boolean saveOrUpdate = this.saveOrUpdate(aiStreamTask);
        if (!saveOrUpdate) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务保存失败");
        }

        try {
            Map<String, Object> engineResult = aiEngineClient.startTask(
                    taskCode,
                    cameraDevice.getCamera_code(),
                    aiStreamTask.getStream_url(),
                    frameIntervalMs,
                    displayStreamUrl,
                    drowningAlertThresholdSec);
            Object statusValue = engineResult.get("status");
            String statusText = statusValue == null ? null : statusValue.toString();
            Date now = new Date();
            UpdateWrapper<AiStreamTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", aiStreamTask.getId());
            updateWrapper.set("task_status", StringUtils.defaultIfBlank(statusText, "RUNNING"));
            updateWrapper.set("started_at", now);
            updateWrapper.set("stopped_at", null);
            updateWrapper.set("updated_at", now);
            this.update(updateWrapper);
        } catch (Exception e) {
            UpdateWrapper<AiStreamTask> failUpdate = new UpdateWrapper<>();
            failUpdate.eq("id", aiStreamTask.getId());
            failUpdate.set("task_status", "FAILED");
            failUpdate.set("updated_at", new Date());
            this.update(failUpdate);
            throw e;
        }
        return getTaskByCode(taskCode);
    }

    private String resolveEngineStreamUrl(CameraDevice cameraDevice) {
        String sourceStreamUrl = StringUtils.trimToEmpty(cameraDevice.getStream_url());
        String mode = StringUtils.defaultIfBlank(appAiEngineProperties.getInputStreamMode(), "source")
                .trim()
                .toLowerCase();
        if ("proxy".equals(mode)) {
            return buildProxyUrl(cameraDevice.getId(), appAiEngineProperties.getInternalPreviewPathTemplate());
        }
        if ("auto".equals(mode)) {
            String proxyUrl = buildProxyUrl(cameraDevice.getId(), appAiEngineProperties.getInternalPreviewPathTemplate());
            return StringUtils.defaultIfBlank(proxyUrl, sourceStreamUrl);
        }
        return sourceStreamUrl;
    }

    private String resolveDisplayStreamUrl(Long cameraId) {
        return buildProxyUrl(cameraId, appAiEngineProperties.getDisplayPreviewPathTemplate());
    }

    private String buildProxyUrl(Long cameraId, String pathTemplate) {
        if (cameraId == null || cameraId <= 0) {
            return "";
        }
        String baseUrl = StringUtils.removeEnd(StringUtils.trimToEmpty(appAiEngineProperties.getProxyBaseUrl()), "/");
        String path = StringUtils.defaultIfBlank(pathTemplate, "");
        path = StringUtils.replace(path, "{cameraId}", String.valueOf(cameraId));
        if (StringUtils.isBlank(path)) {
            return "";
        }
        path = StringUtils.prependIfMissing(path, "/");
        return baseUrl + path;
    }

    @Override
    public boolean start(String taskCode) {
        AiStreamTask aiStreamTask = getTaskByCode(taskCode);
        UpdateWrapper<AiStreamTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", aiStreamTask.getId());
        updateWrapper.set("task_status", "RUNNING");
        updateWrapper.set("started_at", new Date());
        updateWrapper.set("stopped_at", null);
        return this.update(updateWrapper);
    }

    @Override
    public boolean stop(String taskCode) {
        AiStreamTask aiStreamTask = getTaskByCode(taskCode);
        UpdateWrapper<AiStreamTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", aiStreamTask.getId());
        updateWrapper.set("task_status", "STOPPED");
        updateWrapper.set("stopped_at", new Date());
        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopTask(String taskCode) {
        AiStreamTask aiStreamTask = getTaskByCode(taskCode);
        try {
            aiEngineClient.stopTask(taskCode);
        } catch (BusinessException e) {
            if (!StringUtils.containsIgnoreCase(e.getMessage(), "HTTP 404")) {
                throw e;
            }
        }
        UpdateWrapper<AiStreamTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", aiStreamTask.getId());
        updateWrapper.set("task_status", "STOPPED");
        updateWrapper.set("stopped_at", new Date());
        updateWrapper.set("updated_at", new Date());
        return this.update(updateWrapper);
    }

}
