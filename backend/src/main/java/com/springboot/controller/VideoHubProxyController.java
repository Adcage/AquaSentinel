package com.springboot.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.config.AppVideoHubProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.security.StreamTokenAuthService;
import com.springboot.service.CameraDeviceService;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video-hub")
public class VideoHubProxyController {

    @Resource private AppVideoHubProperties videoHubProperties;

    @Resource private StreamTokenAuthService streamTokenAuthService;

    @Resource private CameraDeviceService cameraDeviceService;

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @PostMapping("/cameras/{cameraId}/whip")
    public ResponseEntity<byte[]> whipOffer(
            @PathVariable Long cameraId,
            @RequestParam Map<String, String> params,
            @RequestBody byte[] sdpOffer) {
        String token = params.get(streamTokenAuthService.resolveTokenParamName());
        streamTokenAuthService.verifyPreviewToken(token);

        StringBuilder urlBuilder =
                new StringBuilder()
                        .append(videoHubProperties.getBaseUrl())
                        .append("/video-hub/cameras/")
                        .append(cameraId)
                        .append("/whip");

        boolean hasParam = false;

        CameraDevice camera = cameraDeviceService.getById(cameraId);
        if (camera != null && StringUtils.isNotBlank(camera.getStream_url())) {
            urlBuilder
                    .append("?source_url=")
                    .append(URLEncoder.encode(camera.getStream_url(), StandardCharsets.UTF_8));
            hasParam = true;
        }

        if (StringUtils.isNotBlank(videoHubProperties.getPreferredIp())) {
            urlBuilder.append(hasParam ? "&" : "?");
            urlBuilder.append("preferred_ip=").append(videoHubProperties.getPreferredIp());
        }

        try {
            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(urlBuilder.toString()))
                            .header("Content-Type", "application/sdp")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(sdpOffer))
                            .timeout(Duration.ofSeconds(30))
                            .build();

            HttpResponse<byte[]> targetResponse =
                    HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.valueOf("application/sdp"));

            String location = targetResponse.headers().firstValue("Location").orElse(null);
            if (location != null && !location.startsWith("/api")) {
                responseHeaders.set(HttpHeaders.LOCATION, "/api" + location);
            } else if (location != null) {
                responseHeaders.set(HttpHeaders.LOCATION, location);
            }

            return ResponseEntity.status(targetResponse.statusCode())
                    .headers(responseHeaders)
                    .body(targetResponse.body());
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR, "WHIP 代理请求失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Boolean> deleteWhipSession(@PathVariable String sessionId) {
        String targetUrl = videoHubProperties.getBaseUrl() + "/video-hub/sessions/" + sessionId;

        try {
            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .DELETE()
                            .timeout(Duration.ofSeconds(10))
                            .build();

            HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR, "删除 WHEP 会话失败: " + e.getMessage());
        }
        return ResultUtils.success(true);
    }
}
