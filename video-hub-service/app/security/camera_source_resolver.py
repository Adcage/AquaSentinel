from __future__ import annotations

import logging

import requests
from flask import current_app

from app.common.errors import BusinessError

logger = logging.getLogger(__name__)

_resolve_session = requests.Session()


def resolve_camera_source(camera_id: int, token: str) -> str:
    if not token:
        raise BusinessError(
            "缺少视频流访问令牌",
            status_code=401,
            code="TOKEN_MISSING",
        )
    backend_url = str(current_app.config.get("VIDEO_HUB_BACKEND_BASE_URL") or "").strip()
    if not backend_url:
        raise BusinessError(
            "未配置后端地址，无法解析摄像头视频源",
            status_code=503,
            code="CAMERA_SOURCE_RESOLVE_ERROR",
        )
    url = f"{backend_url.rstrip('/')}/api/video-hub/auth/camera-source"
    try:
        response = _resolve_session.get(
            url,
            params={"cameraId": camera_id, "token": token},
            timeout=5,
        )
    except requests.RequestException as exc:
        logger.error("摄像头视频源解析请求失败: %s", exc)
        raise BusinessError(
            "摄像头视频源解析服务不可用",
            status_code=503,
            code="CAMERA_SOURCE_RESOLVE_ERROR",
        )
    if response.status_code == 401:
        raise BusinessError(
            "视频流访问令牌无效或已过期",
            status_code=401,
            code="TOKEN_INVALID",
        )
    if response.status_code != 200:
        raise BusinessError(
            "摄像头视频源解析失败",
            status_code=503,
            code="CAMERA_SOURCE_RESOLVE_ERROR",
        )
    payload = response.json()
    data = payload.get("data") if isinstance(payload, dict) else None
    source_url = str((data or {}).get("sourceUrl") or "").strip()
    if not source_url:
        raise BusinessError(
            "摄像头未配置可用的视频源地址",
            status_code=503,
            code="CAMERA_SOURCE_EMPTY",
        )
    return source_url
