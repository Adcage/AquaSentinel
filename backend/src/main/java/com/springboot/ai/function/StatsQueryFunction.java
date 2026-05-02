package com.springboot.ai.function;

import java.util.List;
import java.util.function.Function;

import com.springboot.model.entity.StatsSnapshot;
import com.springboot.service.StatsSnapshotService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

/** 查询统计数据Function */
@Component("getStatsSnapshot")
@Description("查询报警统计数据。返回按日期汇总的统计快照。")
public class StatsQueryFunction
        implements Function<StatsQueryFunction.Request, StatsQueryFunction.Response> {

    @Resource private StatsSnapshotService statsSnapshotService;

    public record Request(String venueId, String dateRange, int days, int page, int pageSize) {}

    public record Response(int total, List<StatsSummary> stats) {}

    public record StatsSummary(
            String date,
            int totalAlertCount,
            int drowningCount,
            int resolvedCount,
            int pendingCount) {}

    @Override
    public Response apply(Request request) {
        QueryWrapper<StatsSnapshot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);

        if (request.venueId() != null && !request.venueId().isEmpty()) {
            try {
                queryWrapper.eq("venue_id", Long.parseLong(request.venueId()));
            } catch (NumberFormatException ignored) {
            }
        }

        int days = Math.min(30, Math.max(1, request.days()));
        if (request.days() > 0) {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -days);
            queryWrapper.ge("snapshot_date", calendar.getTime());
        }

        queryWrapper.orderByDesc("snapshot_date");

        int effectivePageSize = Math.min(30, Math.max(1, request.pageSize()));
        queryWrapper.last("LIMIT " + effectivePageSize);

        List<StatsSnapshot> snapshots = statsSnapshotService.list(queryWrapper);

        List<StatsSummary> summaries =
                snapshots.stream()
                        .map(
                                s ->
                                        new StatsSummary(
                                                s.getSnapshot_date() != null
                                                        ? s.getSnapshot_date().toString()
                                                        : null,
                                                s.getMetric_value() != null
                                                        ? s.getMetric_value().intValue()
                                                        : 0,
                                                0,
                                                0,
                                                0))
                        .toList();

        return new Response(summaries.size(), summaries);
    }
}
