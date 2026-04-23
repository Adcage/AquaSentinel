package com.springboot.messaging.errorHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.messaging.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class DeadLetterHandler {

    @RabbitListener(queues = "${app.messaging.rabbitmq.alert-record-queue:alert.record.queue}.dlq")
    public void handleAlertRecordDlq(Message message) {
        log.error("报警记录死信消息: {}", new String(message.getBody()));
    }

    @RabbitListener(
            queues =
                    "${app.messaging.rabbitmq.alert-notification-queue:alert.notification.queue}.dlq")
    public void handleAlertNotificationDlq(Message message) {
        log.error("报警通知死信消息: {}", new String(message.getBody()));
    }

    @RabbitListener(
            queues = "${app.messaging.rabbitmq.alert-analytics-queue:alert.analytics.queue}.dlq")
    public void handleAlertAnalyticsDlq(Message message) {
        log.error("报警分析死信消息: {}", new String(message.getBody()));
    }
}
