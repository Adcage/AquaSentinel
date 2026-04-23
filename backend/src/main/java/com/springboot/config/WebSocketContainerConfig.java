package com.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@ConditionalOnProperty(
        prefix = "app.websocket",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WebSocketContainerConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.websocket",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);
        container.setMaxBinaryMessageBufferSize(1024 * 1024 * 10);
        return container;
    }
}
