package com.springboot.metrics.event;

import org.springframework.context.ApplicationEvent;

/** AI分析事件 */
public class AiAnalysisEvent extends ApplicationEvent {

    private final boolean success;
    private final String alertType;
    private final long durationMs;

    public AiAnalysisEvent(Object source, boolean success, String alertType, long durationMs) {
        super(source);
        this.success = success;
        this.alertType = alertType;
        this.durationMs = durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getAlertType() {
        return alertType;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
