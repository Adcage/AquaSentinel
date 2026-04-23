package com.springboot.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.stream.proxy")
public class AppStreamProxyProperties {

    private boolean enabled = true;

    /** ffmpeg | javacv | rtsp_direct | auto */
    private String mode = "auto";

    private List<String> providerPriority =
            new ArrayList<>(List.of("ffmpeg", "javacv", "rtsp_direct"));

    private String ffmpegPath = "ffmpeg";

    private String ffmpegLogLevel = "error";

    private int jpegQuality = 6;

    private String tokenParamName = "token";

    private List<String> internalAllowedRemoteAddrs =
            new ArrayList<>(List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));
}
