package com.springboot.ai.function;

import java.util.List;
import java.util.function.Function;

import com.springboot.model.entity.AlertRecord;
import com.springboot.service.AlertRecordService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

/** 查询报警记录Function */
@Component("getAlertRecords")
@Description("查询报警记录。可按场馆ID、日期范围、报警类型筛选。返回报警摘要列表。")
public class AlertQueryFunction
        implements Function<AlertQueryFunction.Request, AlertQueryFunction.Response> {

    @Resource private AlertRecordService alertRecordService;

    public record Request(
            String venueId,
            String dateRange,
            String alertType,
            String alertStatus,
            int page,
            int pageSize) {}

    public record Response(int total, List<AlertSummary> records) {}

    public record AlertSummary(
            long id,
            String alertUid,
            String alertType,
            String alertStatus,
            String incidentLocation,
            String detectionResult,
            String createdAt) {}

    @Override
    public Response apply(Request request) {
        QueryWrapper<AlertRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);

        if (request.venueId() != null && !request.venueId().isEmpty()) {
            try {
                queryWrapper.eq("venue_id", Long.parseLong(request.venueId()));
            } catch (NumberFormatException ignored) {
            }
        }

        if (request.alertType() != null && !request.alertType().isEmpty()) {
            queryWrapper.eq("alert_type", request.alertType());
        }

        if (request.alertStatus() != null && !request.alertStatus().isEmpty()) {
            queryWrapper.eq("alert_status", request.alertStatus());
        }

        queryWrapper.orderByDesc("created_at");

        int effectivePage = Math.max(1, request.page());
        int effectivePageSize = Math.min(20, Math.max(1, request.pageSize()));
        queryWrapper.last(
                "LIMIT "
                        + effectivePageSize
                        + " OFFSET "
                        + ((effectivePage - 1) * effectivePageSize));

        List<AlertRecord> records = alertRecordService.list(queryWrapper);

        List<AlertSummary> summaries =
                records.stream()
                        .map(
                                r ->
                                        new AlertSummary(
                                                r.getId(),
                                                r.getAlert_uid(),
                                                r.getAlert_type(),
                                                r.getAlert_status(),
                                                r.getIncident_location(),
                                                truncate(r.getDetection_result(), 100),
                                                r.getCreated_at() != null
                                                        ? r.getCreated_at().toString()
                                                        : null))
                        .toList();

        return new Response(summaries.size(), summaries);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
