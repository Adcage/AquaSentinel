package com.springboot.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.cameradevice.CameraPtzControlRequest;
import com.springboot.model.entity.CameraDevice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Esp32PtzControlService {

    private final ConcurrentMap<Long, Semaphore> controlSlots = new ConcurrentHashMap<>();

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(2000)).build();

    @Resource private ObjectMapper objectMapper;

    public Map<String, Object> control(CameraDevice cameraDevice, CameraPtzControlRequest request) {
        Long cameraId = cameraDevice.getId();
        if (!tryAcquireControlSlot(cameraId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备正在控制中，请稍后再试");
        }
        String baseUrl = resolveDeviceBaseUrl(cameraDevice);
        String action =
                StringUtils.defaultString(request.getAction()).trim().toUpperCase(Locale.ROOT);

        String url;
        String method = "GET";
        if ("HOME".equals(action)) {
            url = baseUrl + "/api/ptz/home";
            method = "POST";
        } else if ("STATUS".equals(action)) {
            url = baseUrl + "/api/ptz/status";
        } else if ("CALIB_DATA".equals(action)) {
            url = baseUrl + "/api/ptz/calib/data";
        } else if ("CALIB_START".equals(action)) {
            url = baseUrl + "/api/ptz/calib/start";
        } else if ("CALIB_SAVE".equals(action)) {
            url = baseUrl + "/api/ptz/calib/save";
        } else if ("CALIB_EXIT".equals(action)) {
            url = baseUrl + "/api/ptz/calib/exit";
        } else if ("CALIB_PAN".equals(action) || "CALIB_TILT".equals(action)) {
            int pulse = request.getPulse() == null ? 1500 : request.getPulse();
            if (pulse < 500) {
                pulse = 500;
            }
            if (pulse > 2500) {
                pulse = 2500;
            }
            String axis = "CALIB_PAN".equals(action) ? "pan" : "tilt";
            url = baseUrl + "/api/ptz/calib/" + axis + "?pulse=" + pulse;
        } else if ("NUDGE".equals(action)) {
            String direction =
                    StringUtils.defaultString(request.getDirection())
                            .trim()
                            .toUpperCase(Locale.ROOT);
            if (!("LEFT".equals(direction)
                    || "RIGHT".equals(direction)
                    || "UP".equals(direction)
                    || "DOWN".equals(direction))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "direction 参数非法");
            }
            int step = request.getStep() == null ? 5 : request.getStep();
            if (step <= 0) {
                step = 5;
            }
            if (step > 10) {
                step = 10;
            }
            url = baseUrl + "/api/ptz/nudge?dir=" + direction + "&step=" + step;
            method = "POST";
        } else if ("MOVE".equals(action)) {
            int pan = request.getPan() == null ? 90 : request.getPan();
            int tilt = request.getTilt() == null ? 90 : request.getTilt();
            if (pan < 0) {
                pan = 0;
            }
            if (pan > 180) {
                pan = 180;
            }
            if (tilt < 0) {
                tilt = 0;
            }
            if (tilt > 180) {
                tilt = 180;
            }
            url = baseUrl + "/api/ptz/move?pan=" + pan + "&tilt=" + tilt;
            method = "POST";
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "action 参数非法");
        }

        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(3000));
            HttpRequest httpRequest =
                    "POST".equals(method)
                            ? builder.POST(HttpRequest.BodyPublishers.noBody()).build()
                            : builder.GET().build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.OPERATION_ERROR, "设备控制失败，HTTP=" + response.statusCode());
            }

            Map<String, Object> payload =
                    objectMapper.readValue(response.body(), new TypeReference<>() {});
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("cameraId", cameraId);
            result.put("action", action);
            result.put("deviceUrl", baseUrl);
            result.put("deviceResponse", payload);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ESP32 控制请求失败, cameraId={}", cameraId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备控制请求失败: " + e.getMessage());
        } finally {
            releaseControlSlot(cameraId);
        }
    }

    private boolean tryAcquireControlSlot(Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            return false;
        }
        Semaphore slot = controlSlots.computeIfAbsent(cameraId, key -> new Semaphore(1));
        return slot.tryAcquire();
    }

    private void releaseControlSlot(Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            return;
        }
        Semaphore slot = controlSlots.get(cameraId);
        if (slot == null || slot.availablePermits() > 0) {
            return;
        }
        slot.release();
    }

    private String resolveDeviceBaseUrl(CameraDevice cameraDevice) {
        // 阶段一仍由 stream_url 保存 ESP32 原始 HTTP 地址，PTZ 控制必须始终指向设备本体，
        // 不能误用 backend 平台预览地址或 yolo-service video_hub 地址。
        String streamUrl = StringUtils.defaultString(cameraDevice.getStream_url()).trim();
        if (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备 streamUrl 需为 ESP32 HTTP 地址");
        }
        URI uri = URI.create(streamUrl);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if (StringUtils.isBlank(scheme) || StringUtils.isBlank(host)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备 streamUrl 无法解析地址");
        }
        String base = scheme + "://" + host;
        if (port > 0) {
            base += ":" + port;
        }
        return base;
    }
}
