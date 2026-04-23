package com.springboot.service;

import java.util.List;

import com.springboot.model.dto.aistreamtask.AiStreamTaskQueryRequest;
import com.springboot.model.dto.monitor.StartMonitorTaskRequest;
import com.springboot.model.entity.AiStreamTask;
import com.springboot.model.vo.AiStreamTaskVO;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description 针对表【ai_stream_task(AI流任务表)】的数据库操作Service
 */
public interface AiStreamTaskService extends IService<AiStreamTask> {

    void validAiStreamTask(AiStreamTask aiStreamTask, boolean add);

    QueryWrapper<AiStreamTask> getQueryWrapper(AiStreamTaskQueryRequest aiStreamTaskQueryRequest);

    AiStreamTaskVO getAiStreamTaskVO(AiStreamTask aiStreamTask);

    List<AiStreamTaskVO> getAiStreamTaskVO(List<AiStreamTask> aiStreamTaskList);

    AiStreamTask getTaskByCode(String taskCode);

    AiStreamTask startTask(StartMonitorTaskRequest request);

    boolean start(String taskCode);

    boolean stop(String taskCode);

    boolean stopTask(String taskCode);
}
