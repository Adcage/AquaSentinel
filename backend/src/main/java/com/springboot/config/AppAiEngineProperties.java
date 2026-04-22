package com.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.engine")
public class AppAiEngineProperties {

    private String baseUrl = "http://127.0.0.1:5000";

    private String startPath = "/engine/tasks/start";

    private String stopPath = "/engine/tasks/stop";

    private String statusPath = "/engine/tasks";

    private String healthPath = "/health";

    private String updateConfigPath = "/engine/tasks/config/update";

    private String callbackUrl = "http://127.0.0.1:8101/api/internal/ai/events";

    /**
     * source | proxy | auto
     */
    private String inputStreamMode = "source";

    private String proxyBaseUrl = "http://127.0.0.1:8300/api";

    private String internalPreviewPathTemplate = "/internal/streams/cameras/{cameraId}/preview";

    private String displayPreviewPathTemplate = "/streams/cameras/{cameraId}/preview";

    private int timeoutMs = 5000;
}
