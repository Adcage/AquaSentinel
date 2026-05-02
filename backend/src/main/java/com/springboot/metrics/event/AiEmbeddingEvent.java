package com.springboot.metrics.event;

import org.springframework.context.ApplicationEvent;

/** AI向量嵌入事件 */
public class AiEmbeddingEvent extends ApplicationEvent {

    private final boolean success;
    private final long alertId;
    private final long durationMs;

    public AiEmbeddingEvent(Object source, boolean success, long alertId, long durationMs) {
        super(source);
        this.success = success;
        this.alertId = alertId;
        this.durationMs = durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getAlertId() {
        return alertId;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
