from __future__ import annotations

import os


class BaseConfig:
    VIDEO_HUB_BACKEND_BASE_URL = os.environ.get(
        "VIDEO_HUB_BACKEND_BASE_URL", "http://127.0.0.1:8300"
    )
    VIDEO_HUB_CONNECT_TIMEOUT_SEC = float(
        os.environ.get("VIDEO_HUB_CONNECT_TIMEOUT_SEC", "3.0")
    )
    VIDEO_HUB_READ_TIMEOUT_SEC = float(
        os.environ.get("VIDEO_HUB_READ_TIMEOUT_SEC", "10.0")
    )
    VIDEO_HUB_STALE_FRAME_TIMEOUT_SEC = float(
        os.environ.get("VIDEO_HUB_STALE_FRAME_TIMEOUT_SEC", "5.0")
    )
    VIDEO_HUB_MAX_WEBRTC_SESSIONS_PER_CAMERA = int(
        os.environ.get("VIDEO_HUB_MAX_WEBRTC_SESSIONS_PER_CAMERA", "10")
    )
    VIDEO_HUB_DEFAULT_TARGET_FPS = float(
        os.environ.get("VIDEO_HUB_DEFAULT_TARGET_FPS", "10.0")
    )
    VIDEO_HUB_PREFERRED_IP = os.environ.get("VIDEO_HUB_PREFERRED_IP", "")
    VIDEO_HUB_REDIS_URL = os.environ.get(
        "VIDEO_HUB_REDIS_URL", "redis://:123456@127.0.0.1:6379/1"
    )
    WEBRTC_MEDIA_PORT_RANGE = os.environ.get("WEBRTC_MEDIA_PORT_RANGE", "")
    VIDEO_HUB_PUSH_TOKEN = os.environ.get("VIDEO_HUB_PUSH_TOKEN", "")
