from __future__ import annotations

import os


class BaseConfig:
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
