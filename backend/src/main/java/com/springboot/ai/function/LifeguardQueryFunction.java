package com.springboot.ai.function;

import java.util.List;
import java.util.function.Function;

import com.springboot.model.entity.Lifeguard;
import com.springboot.service.LifeguardService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

/** 查询值班救生员Function */
@Component("getLifeguardOnDuty")
@Description("查询指定场馆的值班救生员。返回救生员摘要列表。")
public class LifeguardQueryFunction
        implements Function<LifeguardQueryFunction.Request, LifeguardQueryFunction.Response> {

    @Resource private LifeguardService lifeguardService;

    public record Request(String venueId, String dutyStatus, int page, int pageSize) {}

    public record Response(int total, List<LifeguardSummary> lifeguards) {}

    public record LifeguardSummary(
            long id, String name, String dutyStatus, String zoneName, String reportTime) {}

    @Override
    public Response apply(Request request) {
        QueryWrapper<Lifeguard> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);
        queryWrapper.eq("audit_status", "APPROVED");

        if (request.venueId() != null && !request.venueId().isEmpty()) {
            try {
                queryWrapper.eq("venue_id", Long.parseLong(request.venueId()));
            } catch (NumberFormatException ignored) {
            }
        }

        String dutyStatus = request.dutyStatus();
        if (dutyStatus == null || dutyStatus.isEmpty()) {
            dutyStatus = "ON_DUTY";
        }
        queryWrapper.eq("duty_status", dutyStatus);

        int effectivePageSize = Math.min(20, Math.max(1, request.pageSize()));
        queryWrapper.last("LIMIT " + effectivePageSize);

        List<Lifeguard> lifeguards = lifeguardService.list(queryWrapper);

        List<LifeguardSummary> summaries =
                lifeguards.stream()
                        .map(
                                l ->
                                        new LifeguardSummary(
                                                l.getId(),
                                                l.getFull_name(),
                                                l.getDuty_status(),
                                                l.getVenue_id() != null
                                                        ? String.valueOf(l.getVenue_id())
                                                        : null,
                                                l.getLast_login_at() != null
                                                        ? l.getLast_login_at().toString()
                                                        : null))
                        .toList();

        return new Response(summaries.size(), summaries);
    }
}
