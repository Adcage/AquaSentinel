package com.springboot.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.springboot.common.ErrorCode;
import com.springboot.config.AppAiEngineProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class CameraPreviewRouteService {

    private final AppAiEngineProperties appAiEngineProperties;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(2000)).build();

    public CameraPreviewRouteService(AppAiEngineProperties appAiEngineProperties) {
        this.appAiEngineProperties = appAiEngineProperties;
    }

    public boolean supportsVideoHub(CameraDevice cameraDevice) {
        if (cameraDevice == null) {
            return false;
        }
        String streamUrl = StringUtils.defaultString(cameraDevice.getStream_url()).trim();
        return streamUrl.startsWith("http://") || streamUrl.startsWith("https://");
    }

    public URI buildVideoHubStreamUri(CameraDevice cameraDevice) {
        if (cameraDevice == null || cameraDevice.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头不能为空");
        }
        String streamUrl = StringUtils.defaultString(cameraDevice.getStream_url()).trim();
        if (!supportsVideoHub(cameraDevice)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前摄像头不支持 video_hub 预览链路");
        }
        String baseUrl =
                StringUtils.removeEnd(
                        StringUtils.defaultString(appAiEngineProperties.getBaseUrl()).trim(), "/");
        String encodedSource = URLEncoder.encode(streamUrl, StandardCharsets.UTF_8);
        return URI.create(
                baseUrl
                        + "/video-hub/cameras/"
                        + cameraDevice.getId()
                        + "/stream?source_url="
                        + encodedSource);
    }

    public String proxyVideoHubPreview(CameraDevice cameraDevice, OutputStream outputStream)
            throws IOException, InterruptedException {
        URI uri = buildVideoHubStreamUri(cameraDevice);
        HttpRequest request =
                HttpRequest.newBuilder(uri)
                        .timeout(
                                Duration.ofMillis(
                                        Math.max(3000, appAiEngineProperties.getTimeoutMs())))
                        .GET()
                        .build();
        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR, "video_hub 预览请求失败，HTTP=" + response.statusCode());
        }
        try (InputStream inputStream = response.body()) {
            inputStream.transferTo(outputStream);
        }
        return StringUtils.defaultIfBlank(
                response.headers().firstValue("Content-Type").orElse(""),
                "multipart/x-mixed-replace; boundary=frame");
    }
}
