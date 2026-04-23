package com.springboot.metrics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertProcessingCompletedEvent extends ApplicationEvent {

    private final boolean success;
    private final long latencyMs;
    private final String eventUid;

    public AlertProcessingCompletedEvent(boolean success, long latencyMs, String eventUid) {
        super(System.currentTimeMillis());
        this.success = success;
        this.latencyMs = latencyMs;
        this.eventUid = eventUid;
    }
}
