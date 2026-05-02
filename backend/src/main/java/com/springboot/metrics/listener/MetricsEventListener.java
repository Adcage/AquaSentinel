package com.springboot.metrics.listener;

import com.springboot.metrics.event.AiAnalysisEvent;
import com.springboot.metrics.event.AiChatEvent;
import com.springboot.metrics.event.AiEmbeddingEvent;
import com.springboot.metrics.event.AlertEventReceivedEvent;
import com.springboot.metrics.event.AlertProcessingCompletedEvent;
import com.springboot.metrics.event.DeviceStatusChangedEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MetricsEventListener {

    private final Counter alertReceivedCounter;
    private final Counter alertFailedCounter;
    private final Counter alertDroppedCounter;
    private final Timer alertProcessingTimer;
    private final Counter deviceStatusChangedCounter;
    private final Counter aiAnalysisTotalCounter;
    private final Counter aiAnalysisFailedCounter;
    private final Timer aiAnalysisLatencyTimer;
    private final Counter aiChatTotalCounter;
    private final Counter aiFunctionCallCounter;
    private final Counter aiEmbeddingTotalCounter;
    private final Timer aiEmbeddingLatencyTimer;

    public MetricsEventListener(MeterRegistry meterRegistry) {
        this.alertReceivedCounter =
                Counter.builder("alert.events.received")
                        .description("收到的报警事件总数")
                        .tag("source", "rabbitmq")
                        .register(meterRegistry);

        this.alertFailedCounter =
                Counter.builder("alert.events.failed")
                        .description("处理失败的报警事件数")
                        .register(meterRegistry);

        this.alertDroppedCounter =
                Counter.builder("alert.events.dropped")
                        .description("被丢弃的无效报警数")
                        .register(meterRegistry);

        this.alertProcessingTimer =
                Timer.builder("alert.events.processing.latency")
                        .description("报警从接收到处理完成的耗时")
                        .register(meterRegistry);

        this.deviceStatusChangedCounter =
                Counter.builder("device.status.changed")
                        .description("设备状态变更次数")
                        .register(meterRegistry);

        this.aiAnalysisTotalCounter =
                Counter.builder("ai.analysis.total")
                        .description("AI报警分析总次数")
                        .register(meterRegistry);

        this.aiAnalysisFailedCounter =
                Counter.builder("ai.analysis.failed")
                        .description("AI报警分析失败次数")
                        .register(meterRegistry);

        this.aiAnalysisLatencyTimer =
                Timer.builder("ai.analysis.latency")
                        .description("AI报警分析耗时")
                        .register(meterRegistry);

        this.aiChatTotalCounter =
                Counter.builder("ai.chat.total").description("AI对话总次数").register(meterRegistry);

        this.aiFunctionCallCounter =
                Counter.builder("ai.function.call.total")
                        .description("AI Function调用总次数")
                        .register(meterRegistry);

        this.aiEmbeddingTotalCounter =
                Counter.builder("ai.embedding.total")
                        .description("AI向量嵌入生成总次数")
                        .register(meterRegistry);

        this.aiEmbeddingLatencyTimer =
                Timer.builder("ai.embedding.latency")
                        .description("AI向量嵌入生成耗时")
                        .register(meterRegistry);
    }

    @EventListener
    public void onAlertEventReceived(AlertEventReceivedEvent event) {
        if (event.isSuccess()) {
            alertReceivedCounter.increment();
        } else {
            alertDroppedCounter.increment();
        }
        log.debug("指标更新: alert.events.received success={}", event.isSuccess());
    }

    @EventListener
    public void onAlertProcessingCompleted(AlertProcessingCompletedEvent event) {
        if (event.isSuccess()) {
            alertProcessingTimer.record(
                    event.getLatencyMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            alertFailedCounter.increment();
        }
        log.debug(
                "指标更新: alert.events.processing.latency success={}, latencyMs={}",
                event.isSuccess(),
                event.getLatencyMs());
    }

    @EventListener
    public void onDeviceStatusChanged(DeviceStatusChangedEvent event) {
        deviceStatusChangedCounter.increment();
        log.debug(
                "指标更新: device.status.changed deviceId={}, {}->{}",
                event.getDeviceId(),
                event.getOldStatus(),
                event.getNewStatus());
    }

    @Async
    @EventListener
    public void onAiAnalysis(AiAnalysisEvent event) {
        aiAnalysisTotalCounter.increment();
        if (!event.isSuccess()) {
            aiAnalysisFailedCounter.increment();
        }
        aiAnalysisLatencyTimer.record(
                event.getDurationMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug(
                "指标更新: ai.analysis success={}, durationMs={}",
                event.isSuccess(),
                event.getDurationMs());
    }

    @Async
    @EventListener
    public void onAiChat(AiChatEvent event) {
        aiChatTotalCounter.increment();
        if (event.getFunctionName() != null) {
            aiFunctionCallCounter.increment();
        }
        log.debug(
                "指标更新: ai.chat success={}, functionName={}",
                event.isSuccess(),
                event.getFunctionName());
    }

    @Async
    @EventListener
    public void onAiEmbedding(AiEmbeddingEvent event) {
        aiEmbeddingTotalCounter.increment();
        aiEmbeddingLatencyTimer.record(
                event.getDurationMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug(
                "指标更新: ai.embedding success={}, alertId={}", event.isSuccess(), event.getAlertId());
    }
}
