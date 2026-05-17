from __future__ import annotations

import json
import logging
from threading import Thread
from time import sleep

import redis

from app.video_hub.registry import VideoHubRegistry

logger = logging.getLogger(__name__)

STREAM_HASH_KEY = "aqua:camera:streams"
EVENT_CHANNEL = "aqua:camera:events"


class RedisStreamSync:
    def __init__(self, registry: VideoHubRegistry):
        self._registry = registry
        self._redis_url = ""
        self._thread: Thread | None = None
        self._stopped = False

    def start(self, redis_url: str):
        if not redis_url:
            logger.info("未配置 VIDEO_HUB_REDIS_URL，跳过 Redis 同步")
            return
        self._redis_url = redis_url
        try:
            r = redis.from_url(redis_url, decode_responses=True)
            self._sync_all_from_redis(r)
            self._thread = Thread(target=self._listen_loop, daemon=True)
            self._thread.start()
            logger.info("Redis 同步服务已启动")
        except Exception as exc:
            logger.warning("Redis 同步服务启动失败: %s", exc)

    def stop(self):
        self._stopped = True

    def _sync_all_from_redis(self, r: redis.Redis):
        try:
            all_entries = r.hgetall(STREAM_HASH_KEY)
            for camera_id_str, value_json in all_entries.items():
                camera_id = int(camera_id_str)
                data = json.loads(value_json)
                stream_url = data.get("stream_url", "")
                stream_mode = data.get("stream_mode", "pull")
                if stream_url:
                    self._registry.ensure_session(camera_id, stream_url, stream_mode=stream_mode)
            logger.info("Redis 初始同步完成，共 %d 个摄像头", len(all_entries))
        except Exception as exc:
            logger.warning("Redis 初始同步失败: %s", exc)

    def _listen_loop(self):
        r = redis.from_url(self._redis_url, decode_responses=True)
        pubsub = r.pubsub()
        pubsub.subscribe(EVENT_CHANNEL)
        while not self._stopped:
            try:
                message = pubsub.get_message(timeout=1.0)
                if message and message["type"] == "message":
                    self._handle_event(json.loads(message["data"]))
            except redis.ConnectionError:
                logger.warning("Redis 连接断开，5s 后重连")
                sleep(5.0)
            except Exception as exc:
                logger.warning("Redis Pub/Sub 异常: %s", exc)
                sleep(1.0)
        pubsub.unsubscribe()
        pubsub.close()

    def _handle_event(self, event: dict):
        action = event.get("action")
        camera_id = event.get("camera_id")
        if not camera_id:
            return
        if action == "upsert":
            source_url = event.get("stream_url", "")
            stream_mode = event.get("stream_mode", "pull")
            if source_url:
                self._registry.ensure_session(camera_id, source_url, stream_mode=stream_mode)
                logger.info("Redis 事件: camera=%s upsert", camera_id)
        elif action == "delete":
            self._registry.remove_session(camera_id)
            logger.info("Redis 事件: camera=%s delete", camera_id)
