package com.springboot.ai.analysis;

import org.springframework.context.ApplicationEvent;

/** 报警分析事件 用于异步触发AI报警分析 */
public class AlertAnalysisEvent extends ApplicationEvent {

    private final Long alertId;
    private final String alertType;

    public AlertAnalysisEvent(Object source, Long alertId, String alertType) {
        super(source);
        this.alertId = alertId;
        this.alertType = alertType;
    }

    public Long getAlertId() {
        return alertId;
    }

    public String getAlertType() {
        return alertType;
    }
}
