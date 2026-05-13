package com.springboot.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.config.AppStreamProxyProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.security.StreamTokenAuthService;
import com.springboot.service.CameraDeviceService;
import com.springboot.service.CameraPreviewRouteService;
import com.springboot.service.stream.StreamOpenRequest;
import com.springboot.service.stream.StreamProviderRouter;
import com.springboot.service.stream.StreamSession;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CameraStreamController {

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private StreamProviderRouter streamProviderRouter;

    @Resource private CameraPreviewRouteService cameraPreviewRouteService;

    @Resource private StreamTokenAuthService streamTokenAuthService;

    @Resource private AppStreamProxyProperties appStreamProxyProperties;

    @GetMapping("/streams/capabilities")
    public BaseResponse<Map<String, Object>> streamCapabilities() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", appStreamProxyProperties.isEnabled());
        data.put("mode", appStreamProxyProperties.getMode());
        data.put("providerPriority", appStreamProxyProperties.getProviderPriority());
        data.put("providers", streamProviderRouter.listProviderNames());
        data.put("tokenParamName", streamTokenAuthService.resolveTokenParamName());
        return ResultUtils.success(data);
    }

    @GetMapping("/streams/cameras/{cameraId}/preview")
    public void previewExternal(
            @PathVariable Long cameraId,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam Map<String, String> params,
            HttpServletResponse response)
            throws IOException {
        ensureEnabled();
        String token = params.get(streamTokenAuthService.resolveTokenParamName());
        streamTokenAuthService.verifyPreviewToken(token);
        pipePreview(cameraId, provider, false, response);
    }

    @GetMapping("/internal/streams/cameras/{cameraId}/preview")
    public void previewInternal(
            @PathVariable Long cameraId,
            @RequestParam(value = "provider", required = false) String provider,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        ensureEnabled();
        ensureInternalCaller(request.getRemoteAddr());
        pipePreview(cameraId, provider, true, response);
    }

    private void pipePreview(
            Long cameraId, String provider, boolean internalRequest, HttpServletResponse response)
            throws IOException {
        CameraDevice cameraDevice = cameraDeviceService.getById(cameraId);
        if (cameraDevice == null
                || cameraDevice.getIs_delete() != null && cameraDevice.getIs_delete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "摄像头不存在");
        }
        if (cameraDevice.getEnabled() != null && cameraDevice.getEnabled() == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "摄像头未启用");
        }
        StreamOpenRequest request =
                internalRequest
                        ? StreamOpenRequest.internal(provider)
                        : StreamOpenRequest.external(provider);
        if (cameraPreviewRouteService.supportsVideoHub(cameraDevice)) {
            try {
                response.setHeader(
                        "Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setContentType("multipart/x-mixed-replace; boundary=frame");
                cameraPreviewRouteService.proxyVideoHubPreview(
                        cameraDevice, response.getOutputStream());
                return;
            } catch (BusinessException e) {
                throw e;
            } catch (IOException e) {
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "video_hub 预览被中断");
            }
        }
        try (StreamSession streamSession = streamProviderRouter.open(cameraDevice, request)) {
            if (!streamSession.supportsPipe()) {
                if (StringUtils.isBlank(streamSession.getSourceUrl())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前provider不支持预览输出");
                }
                response.sendRedirect(streamSession.getSourceUrl());
                return;
            }
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setContentType(streamSession.getContentType());
            streamSession.pipeTo(response.getOutputStream());
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            // 浏览器主动断开流连接时会触发异常，静默返回
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频流预览失败: " + e.getMessage());
        }
    }

    private void ensureEnabled() {
        if (!appStreamProxyProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "视频流代理未启用");
        }
    }

    private void ensureInternalCaller(String remoteAddr) {
        List<String> allowed = appStreamProxyProperties.getInternalAllowedRemoteAddrs();
        if (allowed == null || allowed.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "内部流访问来源未配置");
        }
        boolean matched = allowed.stream().anyMatch(item -> StringUtils.equals(item, remoteAddr));
        if (!matched) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅允许内部服务访问");
        }
    }
}
