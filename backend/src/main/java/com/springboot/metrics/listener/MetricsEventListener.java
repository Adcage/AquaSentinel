package com.springboot.metrics.listener;

import com.springboot.metrics.event.AlertEventReceivedEvent;
import com.springboot.metrics.event.AlertProcessingCompletedEvent;
import com.springboot.metrics.event.DeviceStatusChangedEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MetricsEventListener {

    private final Counter alertReceivedCounter;
    private final Counter alertFailedCounter;
    private final Counter alertDroppedCounter;
    private final Timer alertProcessingTimer;
    private final Counter deviceStatusChangedCounter;

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
}
