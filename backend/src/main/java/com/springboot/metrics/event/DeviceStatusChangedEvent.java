package com.springboot.metrics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeviceStatusChangedEvent extends ApplicationEvent {

    private final Long deviceId;
    private final String oldStatus;
    private final String newStatus;

    public DeviceStatusChangedEvent(Long deviceId, String oldStatus, String newStatus) {
        super(System.currentTimeMillis());
        this.deviceId = deviceId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
