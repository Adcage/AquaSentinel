package com.springboot.websocket;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class MonitorRealtimeWsPublisher {

    private static final long HEARTBEAT_INTERVAL_MS = 5_000L;

    private final AlertWebSocketHandler alertWebSocketHandler;

    private final ObjectMapper objectMapper;

    private final Map<String, Long> lastHeartbeatMap = new ConcurrentHashMap<>();

    public MonitorRealtimeWsPublisher(
            AlertWebSocketHandler alertWebSocketHandler, ObjectMapper objectMapper) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5_000)
    public void publishHeartbeats() {
        Map<String, Set<Long>> subscriptions =
                alertWebSocketHandler.snapshotRealtimeSubscriptions();
        if (subscriptions.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Set<Long>> entry : subscriptions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = alertWebSocketHandler.getSession(sessionId);
            if (session == null || !session.isOpen()) {
                lastHeartbeatMap.remove(sessionId);
                continue;
            }
            Long lastHb = lastHeartbeatMap.get(sessionId);
            if (lastHb == null || now - lastHb >= HEARTBEAT_INTERVAL_MS) {
                sendHeartbeat(session);
                lastHeartbeatMap.put(sessionId, now);
            }
        }
    }

    private void sendHeartbeat(WebSocketSession session) {
        WsPayload wsPayload = new WsPayload();
        wsPayload.setMessageId(UUID.randomUUID().toString());
        wsPayload.setMessageType(WsMessageType.MONITOR_REALTIME_HEARTBEAT);
        wsPayload.setOccurredAt(Instant.now().toEpochMilli());
        String text;
        try {
            text = objectMapper.writeValueAsString(wsPayload);
        } catch (Exception e) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(text));
            }
        } catch (IOException e) {
            log.debug("send heartbeat ws message failed, sessionId={}", session.getId(), e);
        }
    }
}
