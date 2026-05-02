package com.springboot.metrics;

import com.springboot.websocket.AlertWebSocketHandler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry, AlertWebSocketHandler alertWebSocketHandler) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(
                        "ws.connections.active",
                        alertWebSocketHandler,
                        handler -> handler.allSessions().size())
                .description("当前WebSocket连接数")
                .register(meterRegistry);
    }

    @Bean
    public Counter aiAnalysisTotal() {
        return Counter.builder("ai.analysis.total")
                .description("AI报警分析总次数")
                .register(meterRegistry);
    }

    @Bean
    public Counter aiAnalysisFailed() {
        return Counter.builder("ai.analysis.failed")
                .description("AI报警分析失败次数")
                .register(meterRegistry);
    }

    @Bean
    public Timer aiAnalysisLatency() {
        return Timer.builder("ai.analysis.latency").description("AI报警分析耗时").register(meterRegistry);
    }

    @Bean
    public Counter aiChatTotal() {
        return Counter.builder("ai.chat.total").description("AI对话总次数").register(meterRegistry);
    }

    @Bean
    public Counter aiFunctionCallTotal() {
        return Counter.builder("ai.function.call.total")
                .description("AI Function调用总次数")
                .register(meterRegistry);
    }

    @Bean
    public Counter aiEmbeddingTotal() {
        return Counter.builder("ai.embedding.total")
                .description("AI向量嵌入生成总次数")
                .register(meterRegistry);
    }

    @Bean
    public Timer aiEmbeddingLatency() {
        return Timer.builder("ai.embedding.latency")
                .description("AI向量嵌入生成耗时")
                .register(meterRegistry);
    }
}
