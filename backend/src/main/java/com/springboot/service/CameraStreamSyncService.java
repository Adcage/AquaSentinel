package com.springboot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.springboot.model.entity.CameraDevice;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CameraStreamSyncService {

    private static final String STREAM_HASH_KEY = "aqua:camera:streams";
    private static final String EVENT_CHANNEL = "aqua:camera:events";

    @Resource private StringRedisTemplate stringRedisTemplate;

    @Resource private CameraDeviceService cameraDeviceService;

    @Resource private ObjectMapper objectMapper;

    public void upsertCameraStream(Long cameraId, String streamUrl, Integer enabled) {
        if (cameraId == null || cameraId <= 0) {
            return;
        }
        boolean shouldSync =
                enabled != null
                        && enabled == 1
                        && StringUtils.isNotBlank(streamUrl)
                        && isValidStreamUrl(streamUrl.trim());
        if (!shouldSync) {
            removeCameraStream(cameraId);
            return;
        }
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("stream_url", streamUrl.trim());
            data.put("enabled", true);
            String value = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForHash().put(STREAM_HASH_KEY, String.valueOf(cameraId), value);
            publishEvent("upsert", cameraId, streamUrl.trim());
        } catch (Exception e) {
            log.warn("同步摄像头视频流到 Redis 失败, cameraId={}: {}", cameraId, e.getMessage());
        }
    }

    public void removeCameraStream(Long cameraId) {
        if (cameraId == null || cameraId <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForHash().delete(STREAM_HASH_KEY, String.valueOf(cameraId));
            publishEvent("delete", cameraId, null);
        } catch (Exception e) {
            log.warn("从 Redis 删除摄像头视频流失败, cameraId={}: {}", cameraId, e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialSync() {
        try {
            stringRedisTemplate.delete(STREAM_HASH_KEY);
            QueryWrapper<CameraDevice> qw = new QueryWrapper<>();
            qw.eq("enabled", 1);
            qw.eq("is_delete", 0);
            List<CameraDevice> cameras = cameraDeviceService.list(qw);
            int count = 0;
            for (CameraDevice camera : cameras) {
                String streamUrl = camera.getStream_url();
                if (StringUtils.isNotBlank(streamUrl) && isValidStreamUrl(streamUrl.trim())) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("stream_url", streamUrl.trim());
                    data.put("enabled", true);
                    String value = objectMapper.writeValueAsString(data);
                    stringRedisTemplate
                            .opsForHash()
                            .put(STREAM_HASH_KEY, String.valueOf(camera.getId()), value);
                    count++;
                }
            }
            log.info("摄像头视频流初始同步完成，共 {} 个摄像头写入 Redis", count);
        } catch (Exception e) {
            log.warn("摄像头视频流初始同步失败: {}", e.getMessage());
        }
    }

    private boolean isValidStreamUrl(String url) {
        return url.startsWith("rtsp://") || url.startsWith("http://") || url.startsWith("https://");
    }

    private void publishEvent(String action, Long cameraId, String streamUrl) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("action", action);
            event.put("camera_id", cameraId);
            if (streamUrl != null) {
                event.put("stream_url", streamUrl);
            }
            String message = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(EVENT_CHANNEL, message);
        } catch (Exception e) {
            log.warn("发布摄像头事件到 Redis 失败, cameraId={}: {}", cameraId, e.getMessage());
        }
    }
}
