package com.springboot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.common.ErrorCode;
import com.springboot.config.AppAiEngineProperties;
import com.springboot.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AiEngineClient {

    private final AppAiEngineProperties appAiEngineProperties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public AiEngineClient(AppAiEngineProperties appAiEngineProperties, ObjectMapper objectMapper) {
        this.appAiEngineProperties = appAiEngineProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(appAiEngineProperties.getTimeoutMs()))
                .build();
    }

    public Map<String, Object> startTask(String taskCode, String cameraCode, String streamUrl,
                                         Integer frameIntervalMs, String displayStreamUrl,
                                         Double drowningAlertThresholdSec) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_code", taskCode);
        payload.put("camera_code", cameraCode);
        payload.put("stream_url", streamUrl);
        if (StringUtils.isNotBlank(displayStreamUrl)) {
            payload.put("display_stream_url", displayStreamUrl.trim());
        }
        if (frameIntervalMs != null && frameIntervalMs > 0) {
            payload.put("frame_interval", frameIntervalMs / 1000.0d);
        }
        if (drowningAlertThresholdSec != null && drowningAlertThresholdSec > 0) {
            payload.put("drowning_alert_threshold_sec", drowningAlertThresholdSec);
        }
        return postJson(appAiEngineProperties.getStartPath(), payload);
    }

    public Map<String, Object> startTask(String taskCode, String cameraCode, String streamUrl,
                                         Integer frameIntervalMs) {
        return startTask(taskCode, cameraCode, streamUrl, frameIntervalMs, null, null);
    }

    public Map<String, Object> startTask(String taskCode, String cameraCode, String streamUrl,
                                         Integer frameIntervalMs, String displayStreamUrl) {
        return startTask(taskCode, cameraCode, streamUrl, frameIntervalMs, displayStreamUrl, null);
    }

    public Map<String, Object> stopTask(String taskCode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_code", taskCode);
        return postJson(appAiEngineProperties.getStopPath(), payload);
    }

    public Map<String, Object> getTask(String taskCode) {
        return getTask(taskCode, null);
    }

    public Map<String, Object> getTask(String taskCode, Integer timeoutMs) {
        String url = joinUrl(appAiEngineProperties.getStatusPath() + "/" + taskCode);
        try {
            int requestTimeoutMs = timeoutMs == null || timeoutMs <= 0
                    ? appAiEngineProperties.getTimeoutMs()
                    : timeoutMs;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "AI引擎状态查询失败: HTTP " + response.statusCode());
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            return unwrapData(payload);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI引擎状态查询异常: " + e.getMessage());
        }
    }

    public Map<String, Object> healthCheck() {
        String url = joinUrl(appAiEngineProperties.getHealthPath());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(appAiEngineProperties.getTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "AI引擎健康检查失败: HTTP " + response.statusCode());
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            return unwrapData(payload);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI引擎健康检查异常: " + e.getMessage());
        }
    }

    public String getDefaultCallbackUrl() {
        return appAiEngineProperties.getCallbackUrl();
    }

    public Map<String, Object> updateTaskConfig(String taskCode, Double drowningAlertThresholdSec) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("task_code", taskCode);
        if (drowningAlertThresholdSec != null && drowningAlertThresholdSec > 0) {
            payload.put("drowning_alert_threshold_sec", drowningAlertThresholdSec);
        }
        return postJson(appAiEngineProperties.getUpdateConfigPath(), payload);
    }

    private Map<String, Object> postJson(String path, Map<String, Object> requestPayload) {
        String url = joinUrl(path);
        try {
            String body = objectMapper.writeValueAsString(requestPayload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(appAiEngineProperties.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "AI引擎调用失败: HTTP " + response.statusCode());
            }
            Map<String, Object> responsePayload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            return unwrapData(responsePayload);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI引擎调用异常: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapData(Map<String, Object> payload) {
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> mapData) {
            return (Map<String, Object>) mapData;
        }
        return payload;
    }

    private String joinUrl(String path) {
        String base = StringUtils.removeEnd(appAiEngineProperties.getBaseUrl(), "/");
        String suffix = StringUtils.prependIfMissing(path, "/");
        return base + suffix;
    }
}
