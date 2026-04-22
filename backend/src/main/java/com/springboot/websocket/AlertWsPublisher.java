package com.springboot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class AlertWsPublisher {

    private static final long DEDUP_TTL_MILLIS = 60 * 60 * 1000L;

    private final AlertWebSocketHandler alertWebSocketHandler;

    private final ObjectMapper objectMapper;

    private final Map<String, Long> eventDedupMap = new ConcurrentHashMap<>();

    public AlertWsPublisher(AlertWebSocketHandler alertWebSocketHandler, ObjectMapper objectMapper) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    public boolean publish(WsMessageType messageType, String eventUid, String alertUid, Object data) {
        return publish(messageType, eventUid, alertUid, data, null, null);
    }

    public boolean publish(WsMessageType messageType,
                           String eventUid,
                           String alertUid,
                           Object data,
                           Set<Long> targetUserIds,
                           Set<String> targetRoleCodes) {
        if (StringUtils.isNotBlank(eventUid)) {
            long now = System.currentTimeMillis();
            Long old = eventDedupMap.putIfAbsent(eventUid, now);
            if (old != null) {
                return false;
            }
        }
        WsPayload wsPayload = new WsPayload();
        wsPayload.setMessageId(UUID.randomUUID().toString());
        wsPayload.setMessageType(messageType);
        wsPayload.setEventUid(eventUid);
        wsPayload.setAlertUid(alertUid);
        wsPayload.setOccurredAt(Instant.now().toEpochMilli());
        wsPayload.setData(data);
        broadcast(wsPayload, targetUserIds, targetRoleCodes);
        return true;
    }

    public void publishAlertCreated(String eventUid, String alertUid, Object data) {
        publish(WsMessageType.ALERT_CREATED, eventUid, alertUid, data);
    }

    public void publishAlertCreated(String eventUid,
                                    String alertUid,
                                    Object data,
                                    Set<Long> targetUserIds,
                                    Set<String> targetRoleCodes) {
        publish(WsMessageType.ALERT_CREATED, eventUid, alertUid, data, targetUserIds, targetRoleCodes);
    }

    public void publishAlertUpdated(String eventUid, String alertUid, Object data) {
        publish(WsMessageType.ALERT_UPDATED, eventUid, alertUid, data);
    }

    public void publishCameraStatusChanged(String eventUid, Object data) {
        publish(WsMessageType.CAMERA_STATUS_CHANGED, eventUid, null, data);
    }

    public void publishLifeguardStatusChanged(String eventUid, Object data) {
        publish(WsMessageType.LIFEGUARD_STATUS_CHANGED, eventUid, null, data);
    }

    @Scheduled(fixedDelay = 30000)
    public void publishHeartbeat() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("kind", "HEARTBEAT");
        payload.put("serverTime", Instant.now().toEpochMilli());
        publish(WsMessageType.SYSTEM_NOTICE, null, null, payload);
    }

    @Scheduled(fixedDelay = 600000)
    public void cleanupDedupCache() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = eventDedupMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() >= DEDUP_TTL_MILLIS) {
                iterator.remove();
            }
        }
    }

    private void broadcast(WsPayload wsPayload, Set<Long> targetUserIds, Set<String> targetRoleCodes) {
        String messageText;
        try {
            messageText = objectMapper.writeValueAsString(wsPayload);
        } catch (Exception e) {
            log.error("serialize ws payload failed", e);
            return;
        }
        Set<Long> normalizedUserIds = normalizeUserIds(targetUserIds);
        Set<String> normalizedRoleCodes = normalizeRoleCodes(targetRoleCodes);
        TextMessage textMessage = new TextMessage(messageText);
        for (WebSocketSession session : alertWebSocketHandler.allSessions()) {
            if (!session.isOpen()) {
                continue;
            }
            if (!shouldSendToSession(session, normalizedUserIds, normalizedRoleCodes)) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.warn("send ws message failed, sessionId={}", session.getId(), e);
            }
        }
    }

    private boolean shouldSendToSession(WebSocketSession session,
                                        Set<Long> targetUserIds,
                                        Set<String> targetRoleCodes) {
        boolean hasUserFilter = targetUserIds != null && !targetUserIds.isEmpty();
        boolean hasRoleFilter = targetRoleCodes != null && !targetRoleCodes.isEmpty();
        if (!hasUserFilter && !hasRoleFilter) {
            return true;
        }
        Long userId = toLongValue(session.getAttributes().get("userId"));
        if (hasUserFilter && userId != null && targetUserIds.contains(userId)) {
            return true;
        }
        if (!hasRoleFilter) {
            return false;
        }
        Set<String> roleCodes = parseRoleCodes(session.getAttributes().get("roleCodes"));
        for (String roleCode : roleCodes) {
            if (targetRoleCodes.contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    private Set<Long> normalizeUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long item : userIds) {
            if (item != null && item > 0) {
                normalized.add(item);
            }
        }
        return normalized;
    }

    private Set<String> normalizeRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            String value = StringUtils.upperCase(StringUtils.trimToEmpty(roleCode));
            if (StringUtils.isNotBlank(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private Set<String> parseRoleCodes(Object rawRoles) {
        if (rawRoles == null) {
            return Set.of();
        }
        if (rawRoles instanceof Collection<?> collection) {
            Set<String> roleCodes = new LinkedHashSet<>();
            for (Object item : collection) {
                String roleCode = StringUtils.upperCase(StringUtils.trimToEmpty(Objects.toString(item, "")));
                if (StringUtils.isNotBlank(roleCode)) {
                    roleCodes.add(roleCode);
                }
            }
            return roleCodes;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(rawRoles));
        if (StringUtils.isBlank(text)) {
            return Set.of();
        }
        Set<String> roleCodes = new LinkedHashSet<>();
        for (String item : text.replace("[", "").replace("]", "").split(",")) {
            String roleCode = StringUtils.upperCase(StringUtils.trimToEmpty(item));
            if (StringUtils.isNotBlank(roleCode)) {
                roleCodes.add(roleCode);
            }
        }
        return roleCodes;
    }

    private Long toLongValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
