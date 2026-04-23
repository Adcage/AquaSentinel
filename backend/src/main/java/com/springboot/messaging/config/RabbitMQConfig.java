package com.springboot.messaging.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.messaging.rabbitmq.alert-exchange:alert.topic}")
    private String alertExchange;

    @Value("${app.messaging.rabbitmq.alert-record-queue:alert.record.queue}")
    private String alertRecordQueue;

    @Value("${app.messaging.rabbitmq.alert-notification-queue:alert.notification.queue}")
    private String alertNotificationQueue;

    @Value("${app.messaging.rabbitmq.alert-analytics-queue:alert.analytics.queue}")
    private String alertAnalyticsQueue;

    @Value("${app.messaging.rabbitmq.device-control-exchange:device.control.direct}")
    private String deviceControlExchange;

    @Value("${app.messaging.rabbitmq.dlq-prefix:}")
    private String dlqPrefix;

    @Value("${app.messaging.rabbitmq.message-ttl:86400000}")
    private Integer messageTtl;

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Declarables alertBindings() {
        TopicExchange alertTopic =
                ExchangeBuilder.topicExchange(alertExchange).durable(true).build();

        Queue recordQueue =
                QueueBuilder.durable(alertRecordQueue)
                        .withArguments(queueArguments(alertRecordQueue))
                        .build();

        Queue notificationQueue =
                QueueBuilder.durable(alertNotificationQueue)
                        .withArguments(queueArguments(alertNotificationQueue))
                        .build();

        Queue analyticsQueue =
                QueueBuilder.durable(alertAnalyticsQueue)
                        .withArguments(queueArguments(alertAnalyticsQueue))
                        .build();

        return new Declarables(
                alertTopic,
                recordQueue,
                notificationQueue,
                analyticsQueue,
                BindingBuilder.bind(recordQueue).to(alertTopic).with("alert.record"),
                BindingBuilder.bind(notificationQueue).to(alertTopic).with("alert.notification"),
                BindingBuilder.bind(analyticsQueue).to(alertTopic).with("alert.analytics"));
    }

    @Bean
    public Declarables deadLetterQueues() {
        String recordDlq = dlqPrefix + alertRecordQueue + ".dlq";
        String notificationDlq = dlqPrefix + alertNotificationQueue + ".dlq";
        String analyticsDlq = dlqPrefix + alertAnalyticsQueue + ".dlq";

        return new Declarables(
                QueueBuilder.durable(recordDlq).build(),
                QueueBuilder.durable(notificationDlq).build(),
                QueueBuilder.durable(analyticsDlq).build());
    }

    @Bean
    public Declarables deviceControlBindings() {
        DirectExchange deviceExchange =
                ExchangeBuilder.directExchange(deviceControlExchange).durable(true).build();

        return new Declarables(deviceExchange);
    }

    private Map<String, Object> queueArguments(String queueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", dlqPrefix + queueName + ".dlq");
        args.put("x-message-ttl", messageTtl);
        return args;
    }
}
