package com.springboot.controller;

import com.springboot.common.BaseResponse;
import com.springboot.common.ResultUtils;
import com.springboot.model.entity.CameraDevice;
import com.springboot.security.StreamTokenAuthService;
import com.springboot.service.CameraDeviceService;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video-hub")
public class VideoHubProxyController {

    @Resource private StreamTokenAuthService streamTokenAuthService;

    @Resource private CameraDeviceService cameraDeviceService;

    @GetMapping("/auth/verify-preview-token")
    public BaseResponse<Boolean> verifyPreviewToken(@RequestParam String token) {
        streamTokenAuthService.verifyPreviewToken(token);
        return ResultUtils.success(true);
    }

    @GetMapping("/auth/camera-source")
    public BaseResponse<java.util.Map<String, String>> resolveCameraSource(
            @RequestParam Long cameraId, @RequestParam String token) {
        streamTokenAuthService.verifyPreviewToken(token);
        CameraDevice camera = cameraDeviceService.getById(cameraId);
        if (camera == null || StringUtils.isBlank(camera.getStream_url())) {
            throw new com.springboot.exception.BusinessException(
                    com.springboot.common.ErrorCode.NOT_FOUND_ERROR, "摄像头视频源不存在");
        }
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("sourceUrl", camera.getStream_url().trim());
        data.put(
                "rotation",
                String.valueOf(camera.getRotation() != null ? camera.getRotation() : 0));
        return ResultUtils.success(data);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Boolean> deleteWhipSession(@PathVariable String sessionId) {
        return ResultUtils.success(true);
    }
}
