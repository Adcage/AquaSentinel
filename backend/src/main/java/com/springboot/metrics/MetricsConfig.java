package com.springboot.metrics;

import com.springboot.websocket.AlertWebSocketHandler;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry meterRegistry, AlertWebSocketHandler alertWebSocketHandler) {
        Gauge.builder(
                        "ws.connections.active",
                        alertWebSocketHandler,
                        handler -> handler.allSessions().size())
                .description("当前WebSocket连接数")
                .register(meterRegistry);
    }
}
