package com.springboot.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(2000)).build();

    @Resource private ObjectMapper objectMapper;

    public Map<String, Object> control(CameraDevice cameraDevice, CameraPtzControlRequest request) {
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
            result.put("cameraId", cameraDevice.getId());
            result.put("action", action);
            result.put("deviceUrl", baseUrl);
            result.put("deviceResponse", payload);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ESP32 控制请求失败, cameraId={}", cameraDevice.getId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "设备控制请求失败: " + e.getMessage());
        }
    }

    private String resolveDeviceBaseUrl(CameraDevice cameraDevice) {
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
