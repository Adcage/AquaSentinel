from __future__ import annotations

import logging
from time import time

from flask import current_app

logger = logging.getLogger(__name__)


def register_push_routes(sock):
    @sock.route("/video-hub/cameras/push")
    def push_frames(ws):
        from app.video_hub import video_hub_registry
        from app.video_hub.source_worker import _parse_jpeg_size

        token = ws.receive(timeout=10)
        if token is None:
            logger.warning("推帧连接未发送认证信息，断开")
            return

        expected_token = current_app.config.get("VIDEO_HUB_PUSH_TOKEN", "")
        if expected_token and token != expected_token:
            logger.warning("推帧认证失败，断开")
            return

        camera_id_str = ws.receive(timeout=10)
        if camera_id_str is None:
            logger.warning("推帧连接未发送 camera_id，断开")
            return

        try:
            camera_id = int(camera_id_str)
        except (ValueError, TypeError):
            logger.warning("推帧连接 camera_id 无效: %s", camera_id_str)
            return

        session = video_hub_registry.get_session(camera_id)
        if session is not None and session.stream_mode != "push":
            logger.warning(
                "推帧连接被拒绝 camera_id=%d: 该摄像头为拉流模式(%s)，不允许推帧接入",
                camera_id,
                session.source_url,
            )
            return

        session = video_hub_registry.get_or_create_session(camera_id)
        logger.info("推帧连接建立 camera_id=%d", camera_id)

        try:
            while True:
                data = ws.receive(timeout=30)
                if data is None:
                    logger.info("推帧连接断开 camera_id=%d", camera_id)
                    break
                if isinstance(data, bytes) and len(data) > 0:
                    width, height = _parse_jpeg_size(data)
                    timestamp = int(time() * 1000)
                    session.frame_cache.update(data, width, height, timestamp)
        except Exception as exc:
            logger.info("推帧连接异常 camera_id=%d: %s", camera_id, exc)
        finally:
            logger.info("推帧连接结束 camera_id=%d", camera_id)
