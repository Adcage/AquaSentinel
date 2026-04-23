package com.springboot.messaging.publisher;

import com.springboot.messaging.model.AlertEventMessage;
import com.springboot.messaging.serializer.MessageSerializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.messaging.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class AlertEventPublisher {

    @Value("${app.messaging.rabbitmq.alert-exchange:alert.topic}")
    private String alertExchange;

    private final RabbitTemplate rabbitTemplate;

    private final MessageSerializer messageSerializer;

    public AlertEventPublisher(RabbitTemplate rabbitTemplate, MessageSerializer messageSerializer) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageSerializer = messageSerializer;
    }

    public void publishAlertEvent(AlertEventMessage message) {
        try {
            String json = messageSerializer.serialize(message);
            String routingKey = "alert." + deriveRoutingKey(message);
            rabbitTemplate.convertAndSend(alertExchange, routingKey, json);
            log.debug(
                    "报警事件已发布到RabbitMQ, eventUid={}, routingKey={}",
                    message.getEventUid(),
                    routingKey);
        } catch (Exception e) {
            log.error("报警事件发布到RabbitMQ失败, eventUid={}", message.getEventUid(), e);
        }
    }

    private String deriveRoutingKey(AlertEventMessage message) {
        String eventType = message.getEventType();
        if (eventType != null) {
            String lower = eventType.toLowerCase();
            if (lower.contains("record")
                    || lower.contains("drowning")
                    || lower.contains("drowing")) {
                return "record";
            }
            if (lower.contains("notification") || lower.contains("notify")) {
                return "notification";
            }
            if (lower.contains("analytics") || lower.contains("stat")) {
                return "analytics";
            }
        }
        return "record";
    }
}
