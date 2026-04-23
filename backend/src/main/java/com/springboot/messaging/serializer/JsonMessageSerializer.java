package com.springboot.messaging.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JsonMessageSerializer implements MessageSerializer {

    private final ObjectMapper objectMapper;

    public JsonMessageSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败", e);
            throw new RuntimeException("消息序列化失败", e);
        }
    }

    @Override
    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("消息反序列化失败", e);
            throw new RuntimeException("消息反序列化失败", e);
        }
    }
}
