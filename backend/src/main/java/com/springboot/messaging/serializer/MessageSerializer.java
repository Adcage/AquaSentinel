package com.springboot.messaging.serializer;

public interface MessageSerializer {

    String serialize(Object object);

    <T> T deserialize(String json, Class<T> clazz);
}
