package com.springboot.websocket;

import java.io.Serializable;

import lombok.Data;

@Data
public class WsPayload implements Serializable {

    private String messageId;

    private WsMessageType messageType;

    private String eventUid;

    private String alertUid;

    private long occurredAt;

    private Object data;
}
