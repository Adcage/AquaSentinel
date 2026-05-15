package com.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.video-hub")
public class AppVideoHubProperties {

    private String baseUrl = "http://127.0.0.1:5100";

    private long timeoutMs = 5000;

    private String preferredIp = "";
}
