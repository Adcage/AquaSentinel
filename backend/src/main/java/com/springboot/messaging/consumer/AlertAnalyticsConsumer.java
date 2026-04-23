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
        name = "app.messaging.rabbitmq.analytics-consumer-enabled",
        havingValue = "true",
        matchIfMissing = false)
@Slf4j
public class AlertAnalyticsConsumer {

    @Value("${app.messaging.rabbitmq.alert-analytics-queue:alert.analytics.queue}")
    private String queueName;

    private final MessageSerializer messageSerializer;

    public AlertAnalyticsConsumer(MessageSerializer messageSerializer) {
        this.messageSerializer = messageSerializer;
    }

    @RabbitListener(
            queues = "${app.messaging.rabbitmq.alert-analytics-queue:alert.analytics.queue}")
    public void onMessage(String message) {
        AlertEventMessage eventMsg;
        try {
            eventMsg = messageSerializer.deserialize(message, AlertEventMessage.class);
        } catch (Exception e) {
            log.error("分析消息反序列化失败", e);
            return;
        }
        log.info("收到报警分析消息, eventUid={}, 暂由占位消费者接收，后续阶段接入统计分析", eventMsg.getEventUid());
    }
}
