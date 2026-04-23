package com.springboot.messaging.consumer;

import com.springboot.messaging.model.AlertEventMessage;
import com.springboot.messaging.serializer.MessageSerializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.messaging.rabbitmq.notification-consumer-enabled",
        havingValue = "true",
        matchIfMissing = false)
@Slf4j
public class AlertNotificationConsumer {

    @Value("${app.messaging.rabbitmq.alert-notification-queue:alert.notification.queue}")
    private String queueName;

    private final MessageSerializer messageSerializer;

    public AlertNotificationConsumer(MessageSerializer messageSerializer) {
        this.messageSerializer = messageSerializer;
    }

    @RabbitListener(
            queues = "${app.messaging.rabbitmq.alert-notification-queue:alert.notification.queue}")
    public void onMessage(String message) {
        AlertEventMessage eventMsg;
        try {
            eventMsg = messageSerializer.deserialize(message, AlertEventMessage.class);
        } catch (Exception e) {
            log.error("通知消息反序列化失败", e);
            return;
        }
        log.info("收到报警通知消息, eventUid={}, 暂由占位消费者接收，后续阶段接入多渠道通知", eventMsg.getEventUid());
    }
}
