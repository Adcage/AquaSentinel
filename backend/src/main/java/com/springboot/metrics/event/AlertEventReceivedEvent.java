package com.springboot.metrics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AlertEventReceivedEvent extends ApplicationEvent {

    private final boolean success;
    private final String eventType;

    public AlertEventReceivedEvent(boolean success, String eventType) {
        super(System.currentTimeMillis());
        this.success = success;
        this.eventType = eventType;
    }
}
