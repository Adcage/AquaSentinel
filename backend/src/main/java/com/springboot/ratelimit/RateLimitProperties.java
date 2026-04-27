package com.springboot.ratelimit;

import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 限流配置属性
 *
 * <p>从 application.yml 的 app.rate-limit 节加载配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private boolean distributed = true;

    private int globalCapacity = 100;

    private int globalRefillRate = 100;

    private int globalRefillPeriodSeconds = 1;

    private Map<String, EndpointRateLimit> endpoints = Map.of();

    @Data
    public static class EndpointRateLimit {
        private int capacity = 10;
        private int refillRate = 10;
        private int refillPeriodSeconds = 1;
        private String keyType = "USER";
    }
}
