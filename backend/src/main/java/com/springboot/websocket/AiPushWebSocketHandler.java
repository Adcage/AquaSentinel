package com.springboot.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * 接收 Python AI 引擎通过 WebSocket 主动推送的检测结果，
 * 并转发给订阅了对应摄像头的前端 WS session。
 * 仅允许本机（127.0.0.1）连接，由 AiPushHandshakeInterceptor 保证。
 * 
 * 支持两种消息格式：
 * 1. 文本消息：JSON 格式的检测结果
 * 2. 二进制消息：已画框的 JPEG 帧（需先发送文本元数据）
 */
@Component
@Slf4j
public class AiPushWebSocketHandler extends AbstractWebSocketHandler {

    private static final String HEADER_KEY_VIDEO_FRAME = "VIDEO_FRAME";

    private final AlertWebSocketHandler alertWebSocketHandler;

    private final ObjectMapper objectMapper;

    private final Map<String, VideoFrameMetadata> pendingFrameMetadata = new ConcurrentHashMap<>();

    public AiPushWebSocketHandler(AlertWebSocketHandler alertWebSocketHandler,
                                  ObjectMapper objectMapper) {
        this.alertWebSocketHandler = alertWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("AI push ws connected, sessionId={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("AI push ws disconnected, sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("AI push ws transport error, sessionId={}", session.getId(), exception);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignore) {
        }
    }

    /**
     * 接收 Python 推送的 JSON 消息，格式：
     * {
     *   "cameraId": 5005,
     *   "taskCode": "TASK_CAM_5005_xxx",
     *   "headCount": 3,
     *   "detections": [...],
     *   "riskPoint": {...}
     * }
     * 
     * 或者视频帧元数据（紧跟二进制帧）：
     * {
     *   "type": "VIDEO_FRAME",
     *   "cameraId": 5005,
     *   "frameTs": 1234567890
     * }
     * 转发给订阅了该摄像头的前端 WS session。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (StringUtils.isBlank(payload)) {
            return;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<>() {});
            String type = StringUtils.trimToEmpty((String) data.get("type"));
            if (HEADER_KEY_VIDEO_FRAME.equals(type)) {
                long cameraId = parseCameraId(data.get("cameraId"));
                long frameTs = parseFrameTs(data.get("frameTs"));
                if (cameraId > 0) {
                    int headCount = parseIntValue(data.get("headCount"));
                    List<Object> detections = parseListValue(data.get("detections"));
                    Object riskPoint = data.get("riskPoint");
                    pendingFrameMetadata.put(session.getId(),
                            new VideoFrameMetadata(cameraId, frameTs, headCount, detections, riskPoint));
                }
                return;
            }
            forwardToFrontend(data);
        } catch (Exception e) {
            log.warn("AI push ws message parse failed, sessionId={}, payload={}", session.getId(), payload, e);
        }
    }

    private long parseCameraId(Object raw) {
        if (raw == null) {
            return 0;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseFrameTs(Object raw) {
        if (raw == null) {
            return System.currentTimeMillis();
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private int parseIntValue(Object raw) {
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseListValue(Object raw) {
        if (raw instanceof List<?>) {
            return (List<Object>) raw;
        }
        return List.of();
    }

    private void forwardToFrontend(Map<String, Object> aiData) {
        Object cameraIdRaw = aiData.get("cameraId");
        if (cameraIdRaw == null) {
            return;
        }
        long cameraId;
        try {
            cameraId = Long.parseLong(String.valueOf(cameraIdRaw));
        } catch (Exception e) {
            return;
        }

        Map<String, Set<Long>> subscriptions = alertWebSocketHandler.snapshotRealtimeSubscriptions();
        if (subscriptions.isEmpty()) {
            return;
        }

        WsPayload wsPayload = new WsPayload();
        wsPayload.setMessageId(UUID.randomUUID().toString());
        wsPayload.setMessageType(WsMessageType.MONITOR_REALTIME_BATCH);
        wsPayload.setOccurredAt(Instant.now().toEpochMilli());

        Map<String, Object> batchData = Map.of(String.valueOf(cameraId), buildEnginePayload(aiData));
        wsPayload.setData(batchData);

        String text;
        try {
            text = objectMapper.writeValueAsString(wsPayload);
        } catch (Exception e) {
            log.error("serialize AI push realtime ws payload failed", e);
            return;
        }

        for (Map.Entry<String, Set<Long>> entry : subscriptions.entrySet()) {
            Set<Long> cameraIds = entry.getValue();
            if (cameraIds == null || !cameraIds.contains(cameraId)) {
                continue;
            }
            WebSocketSession frontendSession = alertWebSocketHandler.getSession(entry.getKey());
            if (frontendSession == null || !frontendSession.isOpen()) {
                continue;
            }
            try {
                synchronized (frontendSession) {
                    frontendSession.sendMessage(new TextMessage(text));
                }
            } catch (IOException e) {
                log.debug("forward AI push to frontend failed, sessionId={}", entry.getKey(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEnginePayload(Map<String, Object> aiData) {
        Object detectionsRaw = aiData.get("detections");
        List<Map<String, Object>> detections = List.of();
        if (detectionsRaw instanceof List<?> list) {
            try {
                detections = (List<Map<String, Object>>) list;
            } catch (ClassCastException ignored) {
            }
        }
        Object headCount = aiData.getOrDefault("headCount", 0);
        Object riskPoint = aiData.getOrDefault("riskPoint", Map.of());
        Object taskCode = aiData.getOrDefault("taskCode", "");

        return Map.of(
                "engine", Map.of(
                        "available", true,
                        "realtime", Map.of(
                                "detections", detections,
                                "head_count", headCount,
                                "risk_point", riskPoint
                        ),
                        "task_code", taskCode
                )
        );
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] payload = message.getPayload().array();
        if (payload == null || payload.length == 0) {
            return;
        }
        VideoFrameMetadata metadata = pendingFrameMetadata.remove(session.getId());
        if (metadata == null) {
            log.debug("no pending frame metadata for session {}", session.getId());
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("received binary video frame, cameraId={}, size={}", metadata.cameraId, payload.length);
        }
        forwardVideoFrameToFrontend(metadata, payload);
    }

    private void forwardVideoFrameToFrontend(VideoFrameMetadata metadata, byte[] jpegBytes) {
        long cameraId = metadata.cameraId;
        if (log.isDebugEnabled()) {
            log.debug("forwarding video frame to frontend, cameraId={}, size={}", cameraId, jpegBytes.length);
        }
        Map<String, Set<Long>> subscriptions = alertWebSocketHandler.snapshotRealtimeSubscriptions();
        if (subscriptions.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Set<Long>> entry : subscriptions.entrySet()) {
            Set<Long> cameraIds = entry.getValue();
            if (cameraIds == null || !cameraIds.contains(cameraId)) {
                continue;
            }
            WebSocketSession frontendSession = alertWebSocketHandler.getSession(entry.getKey());
            if (frontendSession == null || !frontendSession.isOpen()) {
                continue;
            }
            try {
                synchronized (frontendSession) {
                    WsPayload headerPayload = new WsPayload();
                    headerPayload.setMessageId(UUID.randomUUID().toString());
                    headerPayload.setMessageType(WsMessageType.MONITOR_VIDEO_FRAME);
                    headerPayload.setOccurredAt(Instant.now().toEpochMilli());
                    java.util.HashMap<String, Object> frameData = new java.util.HashMap<>();
                    frameData.put("cameraId", cameraId);
                    frameData.put("frameTs", metadata.frameTs);
                    frameData.put("seq", System.nanoTime());
                    frameData.put("headCount", metadata.headCount);
                    frameData.put("detections", metadata.detections);
                    if (metadata.riskPoint != null) {
                        frameData.put("riskPoint", metadata.riskPoint);
                    }
                    headerPayload.setData(frameData);
                    String headerJson = objectMapper.writeValueAsString(headerPayload);
                    frontendSession.sendMessage(new TextMessage(headerJson));
                    frontendSession.sendMessage(new BinaryMessage(jpegBytes));
                }
            } catch (IOException e) {
                log.debug("forward video frame to frontend failed, sessionId={}", entry.getKey(), e);
            }
        }
    }

    public static class VideoFrameMetadata {
        public long cameraId;
        public long frameTs;
        public int headCount;
        public List<Object> detections;
        public Object riskPoint;

        public VideoFrameMetadata(long cameraId, long frameTs, int headCount, List<Object> detections, Object riskPoint) {
            this.cameraId = cameraId;
            this.frameTs = frameTs;
            this.headCount = headCount;
            this.detections = detections != null ? detections : List.of();
            this.riskPoint = riskPoint;
        }
    }
}
