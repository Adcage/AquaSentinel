package com.springboot.service.stream;

import com.springboot.common.ErrorCode;
import com.springboot.config.AppStreamProxyProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class StreamProviderRouter {

    private final AppStreamProxyProperties appStreamProxyProperties;

    private final Map<String, StreamProvider> providerMap = new HashMap<>();

    public StreamProviderRouter(AppStreamProxyProperties appStreamProxyProperties, List<StreamProvider> providers) {
        this.appStreamProxyProperties = appStreamProxyProperties;
        for (StreamProvider provider : providers) {
            providerMap.put(normalize(provider.name()), provider);
        }
    }

    public StreamSession open(CameraDevice cameraDevice, StreamOpenRequest request) {
        if (cameraDevice == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "摄像头不能为空");
        }
        List<String> candidates = resolveCandidates(request);
        List<String> errors = new ArrayList<>();
        String protocol = cameraDevice.getProtocol();
        for (String candidateName : candidates) {
            StreamProvider provider = providerMap.get(candidateName);
            if (provider == null) {
                continue;
            }
            if (!provider.supports(protocol)) {
                continue;
            }
            try {
                return provider.open(cameraDevice, request);
            } catch (Exception e) {
                errors.add(candidateName + ": " + e.getMessage());
                if (!request.isAllowFallback()) {
                    break;
                }
            }
        }
        String message = errors.isEmpty()
                ? "无可用流提供器"
                : "流提供器均不可用: " + String.join("; ", errors);
        throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
    }

    public List<String> listProviderNames() {
        return providerMap.keySet().stream().sorted().toList();
    }

    private List<String> resolveCandidates(StreamOpenRequest request) {
        String preferred = normalize(request.getPreferredProvider());
        if (StringUtils.isNotBlank(preferred) && !"auto".equals(preferred)) {
            return List.of(preferred);
        }
        String configuredMode = normalize(appStreamProxyProperties.getMode());
        if (StringUtils.isNotBlank(configuredMode) && !"auto".equals(configuredMode)) {
            return List.of(configuredMode);
        }
        List<String> priorities = appStreamProxyProperties.getProviderPriority();
        if (priorities == null || priorities.isEmpty()) {
            if (request.isInternalRequest()) {
                return List.of("ffmpeg", "javacv", "rtsp_direct");
            }
            return List.of("ffmpeg", "javacv");
        }
        return priorities.stream()
                .map(this::normalize)
                .filter(StringUtils::isNotBlank)
                .filter(item -> request.isInternalRequest() || !"rtsp_direct".equals(item))
                .toList();
    }

    private String normalize(String value) {
        return StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
    }
}
