package com.springboot.ai.function;

import java.util.List;
import java.util.function.Function;

import com.springboot.model.entity.CameraDevice;
import com.springboot.service.CameraDeviceService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

/** 查询设备状态Function */
@Component("getDeviceStatus")
@Description("查询摄像头设备在线状态。可按场馆ID筛选。返回设备状态摘要列表。")
public class DeviceStatusFunction
        implements Function<DeviceStatusFunction.Request, DeviceStatusFunction.Response> {

    @Resource private CameraDeviceService cameraDeviceService;

    public record Request(String venueId, String cameraCode, int page, int pageSize) {}

    public record Response(int total, List<DeviceSummary> devices) {}

    public record DeviceSummary(
            long id,
            String cameraCode,
            String cameraName,
            long venueId,
            String status,
            String lastHeartbeatAt) {}

    @Override
    public Response apply(Request request) {
        QueryWrapper<CameraDevice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0);

        if (request.venueId() != null && !request.venueId().isEmpty()) {
            try {
                queryWrapper.eq("venue_id", Long.parseLong(request.venueId()));
            } catch (NumberFormatException ignored) {
            }
        }

        if (request.cameraCode() != null && !request.cameraCode().isEmpty()) {
            queryWrapper.eq("camera_code", request.cameraCode());
        }

        int effectivePageSize = Math.min(20, Math.max(1, request.pageSize()));
        queryWrapper.last("LIMIT " + effectivePageSize);

        List<CameraDevice> devices = cameraDeviceService.list(queryWrapper);

        List<DeviceSummary> summaries =
                devices.stream()
                        .map(
                                d ->
                                        new DeviceSummary(
                                                d.getId(),
                                                d.getCamera_code(),
                                                d.getCamera_name(),
                                                d.getVenue_id(),
                                                d.getDevice_status() != null
                                                        ? d.getDevice_status()
                                                        : "UNKNOWN",
                                                d.getLast_heartbeat_at() != null
                                                        ? d.getLast_heartbeat_at().toString()
                                                        : null))
                        .toList();

        return new Response(summaries.size(), summaries);
    }
}
