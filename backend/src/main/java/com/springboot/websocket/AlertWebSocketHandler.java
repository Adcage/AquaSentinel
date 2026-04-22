package com.springboot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final String ACTION_SUBSCRIBE_MONITOR_REALTIME = "SUBSCRIBE_MONITOR_REALTIME";

    private static final String ACTION_UNSUBSCRIBE_MONITOR_REALTIME = "UNSUBSCRIBE_MONITOR_REALTIME";

    private final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private final Map<String, Set<Long>> sessionRealtimeSubscriptions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public AlertWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionMap.put(session.getId(), session);
        log.info("ws alerts connected, sessionId={}, online={}", session.getId(), sessionMap.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionMap.remove(session.getId());
        sessionRealtimeSubscriptions.remove(session.getId());
        log.info("ws alerts disconnected, sessionId={}, online={}", session.getId(), sessionMap.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionMap.remove(session.getId());
        sessionRealtimeSubscriptions.remove(session.getId());
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignore) {
        }
        log.warn("ws alerts transport error, sessionId={}", session.getId(), exception);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = StringUtils.upperCase(StringUtils.trimToEmpty((String) payload.get("action")));
            Set<Long> cameraIds = parseCameraIds(payload.get("cameraIds"));
            if (StringUtils.equals(action, ACTION_SUBSCRIBE_MONITOR_REALTIME)) {
                sessionRealtimeSubscriptions.put(session.getId(), cameraIds);
                return;
            }
            if (StringUtils.equals(action, ACTION_UNSUBSCRIBE_MONITOR_REALTIME)) {
                sessionRealtimeSubscriptions.remove(session.getId());
            }
        } catch (Exception e) {
            log.debug("ignore ws client message parse failure, sessionId={}", session.getId(), e);
        }
    }

    public Collection<WebSocketSession> allSessions() {
        return sessionMap.values();
    }

    public WebSocketSession getSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        return sessionMap.get(sessionId);
    }

    public Map<String, Set<Long>> snapshotRealtimeSubscriptions() {
        Map<String, Set<Long>> snapshot = new ConcurrentHashMap<>();
        for (Map.Entry<String, Set<Long>> entry : sessionRealtimeSubscriptions.entrySet()) {
            WebSocketSession session = sessionMap.get(entry.getKey());
            if (session == null || !session.isOpen()) {
                continue;
            }
            Set<Long> cameraIds = entry.getValue();
            if (cameraIds == null || cameraIds.isEmpty()) {
                continue;
            }
            snapshot.put(entry.getKey(), new LinkedHashSet<>(cameraIds));
        }
        return snapshot;
    }

    private Set<Long> parseCameraIds(Object raw) {
        if (raw == null) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        if (raw instanceof Iterable<?> items) {
            for (Object item : items) {
                appendCameraId(result, item);
            }
            return result;
        }
        String text = String.valueOf(raw);
        if (StringUtils.isBlank(text)) {
            return result;
        }
        for (String part : text.split(",")) {
            appendCameraId(result, part);
        }
        return result;
    }

    private void appendCameraId(Set<Long> holder, Object value) {
        if (value == null) {
            return;
        }
        try {
            long cameraId = Long.parseLong(String.valueOf(value).trim());
            if (cameraId > 0) {
                holder.add(cameraId);
            }
        } catch (Exception ignored) {
        }
    }
}
