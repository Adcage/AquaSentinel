package com.springboot.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private Jwt jwt = new Jwt();

    private Cors cors = new Cors();

    private AiCallback aiCallback = new AiCallback();

    @Data
    public static class Jwt {

        private String issuer = "aqua-sentinel-backend";

        private String secret = "change-this-jwt-secret-change-this-jwt-secret";

        private long accessTokenExpireSeconds = 1800;

        private long refreshTokenExpireSeconds = 604800;
    }

    @Data
    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Data
    public static class AiCallback {

        private String key = "ai-service";

        private String secret = "change-this-ai-callback-secret";

        private long allowedSkewSeconds = 300;
    }
}
