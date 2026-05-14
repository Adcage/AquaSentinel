package com.springboot.controller;

import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.config.AppAiEngineProperties;
import com.springboot.exception.BusinessException;
import com.springboot.security.StreamTokenAuthService;

import jakarta.annotation.Resource;
import org.springframework.http.HttpEntity;
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
import org.springframework.web.client.RestTemplate;

/**
 * WHEP 信令反代控制器
 *
 * <p>将 WHEP 信令请求转发到 yolo-service，保持"前端只跟 Java 后端交互"约定。
 */
@RestController
@RequestMapping("/video-hub")
public class VideoHubProxyController {

    @Resource private AppAiEngineProperties aiEngineProperties;

    @Resource private StreamTokenAuthService streamTokenAuthService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 转发 WHEP SDP offer 到 yolo-service
     *
     * @param cameraId 摄像头 ID
     * @param params 查询参数（含 token）
     * @param sdpOffer SDP offer 请求体
     * @return 201 + SDP answer + Location header
     */
    @PostMapping("/cameras/{cameraId}/whip")
    public ResponseEntity<byte[]> whipOffer(
            @PathVariable Long cameraId,
            @RequestParam Map<String, String> params,
            @RequestBody byte[] sdpOffer) {
        String token = params.get(streamTokenAuthService.resolveTokenParamName());
        streamTokenAuthService.verifyPreviewToken(token);

        String yoloUrl =
                aiEngineProperties.getBaseUrl() + "/video-hub/cameras/" + cameraId + "/whip";

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.valueOf("application/sdp"));
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(sdpOffer, requestHeaders);

        ResponseEntity<byte[]> yoloResponse =
                restTemplate.exchange(
                        yoloUrl,
                        org.springframework.http.HttpMethod.POST,
                        requestEntity,
                        byte[].class);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.valueOf("application/sdp"));

        String location = yoloResponse.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location != null && !location.startsWith("/api")) {
            responseHeaders.set(HttpHeaders.LOCATION, "/api" + location);
        } else if (location != null) {
            responseHeaders.set(HttpHeaders.LOCATION, location);
        }

        return ResponseEntity.status(yoloResponse.getStatusCode())
                .headers(responseHeaders)
                .body(yoloResponse.getBody());
    }

    /**
     * 转发 WHEP 会话删除请求到 yolo-service
     *
     * @param sessionId WHEP 会话 ID
     * @return 成功响应
     */
    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Boolean> deleteWhipSession(@PathVariable String sessionId) {
        String yoloUrl = aiEngineProperties.getBaseUrl() + "/video-hub/sessions/" + sessionId;

        try {
            restTemplate.delete(yoloUrl);
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR, "删除 WHEP 会话失败: " + e.getMessage());
        }
        return ResultUtils.success(true);
    }
}
