package com.springboot.ai.function;

import java.util.List;
import java.util.function.Function;

import com.springboot.model.entity.AiStreamTask;
import com.springboot.service.AiStreamTaskService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

/** 查询AI推理任务状态Function */
@Component("getMonitorTasks")
@Description("查询AI推理任务状态。返回任务摘要列表。")
public class MonitorTaskQueryFunction
        implements Function<MonitorTaskQueryFunction.Request, MonitorTaskQueryFunction.Response> {

    @Resource private AiStreamTaskService aiStreamTaskService;

    public record Request(String venueId, String taskCode, String status, int page, int pageSize) {}

    public record Response(int total, List<TaskSummary> tasks) {}

    public record TaskSummary(
            long id,
            String taskCode,
            String cameraCode,
            String status,
            String startedAt,
            String stoppedAt,
            String lastFrameAt) {}

    @Override
    public Response apply(Request request) {
        QueryWrapper<AiStreamTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);

        if (request.taskCode() != null && !request.taskCode().isEmpty()) {
            queryWrapper.eq("task_code", request.taskCode());
        }

        if (request.status() != null && !request.status().isEmpty()) {
            queryWrapper.eq("task_status", request.status());
        }

        queryWrapper.orderByDesc("created_at");

        int effectivePageSize = Math.min(20, Math.max(1, request.pageSize()));
        queryWrapper.last("LIMIT " + effectivePageSize);

        List<AiStreamTask> tasks = aiStreamTaskService.list(queryWrapper);

        List<TaskSummary> summaries =
                tasks.stream()
                        .map(
                                t ->
                                        new TaskSummary(
                                                t.getId(),
                                                t.getTask_code(),
                                                null,
                                                t.getTask_status(),
                                                t.getStarted_at() != null
                                                        ? t.getStarted_at().toString()
                                                        : null,
                                                t.getStopped_at() != null
                                                        ? t.getStopped_at().toString()
                                                        : null,
                                                t.getLast_frame_at() != null
                                                        ? t.getLast_frame_at().toString()
                                                        : null))
                        .toList();

        return new Response(summaries.size(), summaries);
    }
}
