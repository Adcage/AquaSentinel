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
import com.springboot.model.dto.aistreamtask.AiStreamTaskAddRequest;
import com.springboot.model.dto.aistreamtask.AiStreamTaskEditRequest;
import com.springboot.model.dto.aistreamtask.AiStreamTaskQueryRequest;
import com.springboot.model.dto.aistreamtask.AiStreamTaskUpdateRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.vo.AiStreamTaskVO;
import com.springboot.service.AiStreamTaskService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-stream-tasks")
public class AiStreamTaskController {

    @Resource private AiStreamTaskService aiStreamTaskService;

    @PostMapping("/add")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Long> addAiStreamTask(
            @RequestBody AiStreamTaskAddRequest aiStreamTaskAddRequest) {
        if (aiStreamTaskAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiStreamTask aiStreamTask = toAiStreamTask(aiStreamTaskAddRequest);
        aiStreamTask.setTask_status(
                aiStreamTask.getTask_status() == null ? "PENDING" : aiStreamTask.getTask_status());
        aiStreamTask.setFrame_interval_ms(
                aiStreamTask.getFrame_interval_ms() == null
                        ? 200
                        : aiStreamTask.getFrame_interval_ms());
        aiStreamTask.setCreated_at(new Date());
        aiStreamTask.setUpdated_at(new Date());
        aiStreamTaskService.validAiStreamTask(aiStreamTask, true);
        boolean result = aiStreamTaskService.save(aiStreamTask);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(aiStreamTask.getId());
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> deleteAiStreamTask(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = aiStreamTaskService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> updateAiStreamTask(
            @RequestBody AiStreamTaskUpdateRequest aiStreamTaskUpdateRequest) {
        if (aiStreamTaskUpdateRequest == null
                || aiStreamTaskUpdateRequest.getId() == null
                || aiStreamTaskUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiStreamTask aiStreamTask = toAiStreamTask(aiStreamTaskUpdateRequest);
        aiStreamTaskService.validAiStreamTask(aiStreamTask, false);
        boolean result = aiStreamTaskService.updateById(aiStreamTask);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/edit")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Boolean> editAiStreamTask(
            @RequestBody AiStreamTaskEditRequest aiStreamTaskEditRequest) {
        if (aiStreamTaskEditRequest == null
                || aiStreamTaskEditRequest.getId() == null
                || aiStreamTaskEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiStreamTask aiStreamTask = new AiStreamTask();
        aiStreamTask.setId(aiStreamTaskEditRequest.getId());
        aiStreamTask.setStream_url(aiStreamTaskEditRequest.getStreamUrl());
        aiStreamTask.setFrame_interval_ms(aiStreamTaskEditRequest.getFrameIntervalMs());
        aiStreamTask.setCallback_url(aiStreamTaskEditRequest.getCallbackUrl());
        aiStreamTask.setTask_status(aiStreamTaskEditRequest.getTaskStatus());
        aiStreamTaskService.validAiStreamTask(aiStreamTask, false);
        boolean result = aiStreamTaskService.updateById(aiStreamTask);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<AiStreamTask> getAiStreamTaskById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiStreamTask aiStreamTask = aiStreamTaskService.getById(id);
        ThrowUtils.throwIf(aiStreamTask == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(aiStreamTask);
    }

    @GetMapping("/get/vo")
    public BaseResponse<AiStreamTaskVO> getAiStreamTaskVOById(long id) {
        BaseResponse<AiStreamTask> response = getAiStreamTaskById(id);
        return ResultUtils.success(aiStreamTaskService.getAiStreamTaskVO(response.getData()));
    }

    @GetMapping("/code/get")
    public BaseResponse<AiStreamTask> getAiStreamTaskByCode(@RequestParam String taskCode) {
        return ResultUtils.success(aiStreamTaskService.getTaskByCode(taskCode));
    }

    @PostMapping("/list")
    public BaseResponse<List<AiStreamTask>> listAiStreamTask(
            @RequestBody AiStreamTaskQueryRequest aiStreamTaskQueryRequest) {
        if (aiStreamTaskQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(
                aiStreamTaskService.list(
                        aiStreamTaskService.getQueryWrapper(aiStreamTaskQueryRequest)));
    }

    @PostMapping("/list/vo")
    public BaseResponse<List<AiStreamTaskVO>> listAiStreamTaskVO(
            @RequestBody AiStreamTaskQueryRequest aiStreamTaskQueryRequest) {
        BaseResponse<List<AiStreamTask>> response = listAiStreamTask(aiStreamTaskQueryRequest);
        return ResultUtils.success(aiStreamTaskService.getAiStreamTaskVO(response.getData()));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<AiStreamTask>> listAiStreamTaskByPage(
            @RequestBody AiStreamTaskQueryRequest aiStreamTaskQueryRequest) {
        if (aiStreamTaskQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = aiStreamTaskQueryRequest.getCurrent();
        long size = aiStreamTaskQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR, "分页大小不能超过100");
        Page<AiStreamTask> aiStreamTaskPage =
                aiStreamTaskService.page(
                        new Page<>(current, size),
                        aiStreamTaskService.getQueryWrapper(aiStreamTaskQueryRequest));
        return ResultUtils.success(aiStreamTaskPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AiStreamTaskVO>> listAiStreamTaskVOByPage(
            @RequestBody AiStreamTaskQueryRequest aiStreamTaskQueryRequest) {
        BaseResponse<Page<AiStreamTask>> response =
                listAiStreamTaskByPage(aiStreamTaskQueryRequest);
        Page<AiStreamTask> aiStreamTaskPage = response.getData();
        Page<AiStreamTaskVO> aiStreamTaskVOPage =
                new Page<>(
                        aiStreamTaskPage.getCurrent(),
                        aiStreamTaskPage.getSize(),
                        aiStreamTaskPage.getTotal());
        aiStreamTaskVOPage.setRecords(
                aiStreamTaskService.getAiStreamTaskVO(aiStreamTaskPage.getRecords()));
        return ResultUtils.success(aiStreamTaskVOPage);
    }

    private AiStreamTask toAiStreamTask(AiStreamTaskAddRequest request) {
        AiStreamTask aiStreamTask = new AiStreamTask();
        aiStreamTask.setTask_code(request.getTaskCode());
        aiStreamTask.setCamera_id(request.getCameraId());
        aiStreamTask.setStream_url(request.getStreamUrl());
        aiStreamTask.setFrame_interval_ms(request.getFrameIntervalMs());
        aiStreamTask.setCallback_url(request.getCallbackUrl());
        aiStreamTask.setTask_status(request.getTaskStatus());
        return aiStreamTask;
    }

    private AiStreamTask toAiStreamTask(AiStreamTaskUpdateRequest request) {
        AiStreamTask aiStreamTask = new AiStreamTask();
        aiStreamTask.setId(request.getId());
        aiStreamTask.setTask_code(request.getTaskCode());
        aiStreamTask.setCamera_id(request.getCameraId());
        aiStreamTask.setStream_url(request.getStreamUrl());
        aiStreamTask.setFrame_interval_ms(request.getFrameIntervalMs());
        aiStreamTask.setCallback_url(request.getCallbackUrl());
        aiStreamTask.setTask_status(request.getTaskStatus());
        return aiStreamTask;
    }
}
