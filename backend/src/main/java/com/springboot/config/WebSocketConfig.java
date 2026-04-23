package com.springboot.config;

import java.util.List;

import com.springboot.websocket.AiPushHandshakeInterceptor;
import com.springboot.websocket.AiPushWebSocketHandler;
import com.springboot.websocket.AlertWebSocketHandler;
import com.springboot.websocket.AuthHandshakeInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AlertWebSocketHandler alertWebSocketHandler;

    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    private final AiPushWebSocketHandler aiPushWebSocketHandler;

    private final AiPushHandshakeInterceptor aiPushHandshakeInterceptor;

    private final AppSecurityProperties appSecurityProperties;

    public WebSocketConfig(
            AlertWebSocketHandler alertWebSocketHandler,
            AuthHandshakeInterceptor authHandshakeInterceptor,
            AiPushWebSocketHandler aiPushWebSocketHandler,
            AiPushHandshakeInterceptor aiPushHandshakeInterceptor,
            AppSecurityProperties appSecurityProperties) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
        this.aiPushWebSocketHandler = aiPushWebSocketHandler;
        this.aiPushHandshakeInterceptor = aiPushHandshakeInterceptor;
        this.appSecurityProperties = appSecurityProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        List<String> allowedOrigins = appSecurityProperties.getCors().getAllowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
        }
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
        registry.addHandler(aiPushWebSocketHandler, "/ws/ai-push")
                .addInterceptors(aiPushHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
