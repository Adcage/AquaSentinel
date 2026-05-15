from __future__ import annotations

import logging

import requests

from app.core.config import BaseConfig

logger = logging.getLogger(__name__)


class VideoHubClient:
    def __init__(self, base_url: str | None = None, timeout_ms: int | None = None):
        self.base_url = (base_url or BaseConfig.VIDEO_HUB_BASE_URL).rstrip("/")
        self.timeout_ms = timeout_ms or BaseConfig.VIDEO_HUB_TIMEOUT_MS
        self._session = requests.Session()
        self._session.trust_env = False

    def _ensure_url(self, camera_id: int) -> str:
        return f"{self.base_url}/video-hub/cameras/{camera_id}/ensure"

    def _snapshot_url(self, camera_id: int) -> str:
        return f"{self.base_url}/video-hub/cameras/{camera_id}/snapshot"

    def _status_url(self, camera_id: int) -> str:
        return f"{self.base_url}/video-hub/cameras/{camera_id}/status"

    def _reconnect_url(self, camera_id: int) -> str:
        return f"{self.base_url}/video-hub/cameras/{camera_id}/reconnect"

    def ensure_session(self, camera_id: int, source_url: str) -> dict:
        try:
            resp = self._session.post(
                self._ensure_url(camera_id),
                json={"source_url": source_url},
                timeout=self.timeout_ms / 1000.0,
            )
            resp.raise_for_status()
            return resp.json()
        except Exception as exc:
            logger.warning("video_hub ensure_session 失败 camera_id=%s: %s", camera_id, exc)
            raise

    def fetch_snapshot(self, camera_id: int) -> bytes | None:
        try:
            resp = self._session.get(
                self._snapshot_url(camera_id),
                timeout=self.timeout_ms / 1000.0,
            )
            if resp.status_code == 503:
                logger.debug("video_hub 暂无帧 camera_id=%s", camera_id)
                return None
            resp.raise_for_status()
            return resp.content
        except Exception as exc:
            logger.warning("video_hub fetch_snapshot 失败 camera_id=%s: %s", camera_id, exc)
            return None

    def get_status(self, camera_id: int) -> dict | None:
        try:
            resp = self._session.get(
                self._status_url(camera_id),
                timeout=self.timeout_ms / 1000.0,
            )
            if resp.status_code == 404:
                return None
            resp.raise_for_status()
            return resp.json()
        except Exception as exc:
            logger.warning("video_hub get_status 失败 camera_id=%s: %s", camera_id, exc)
            return None

    def reconnect(self, camera_id: int) -> dict | None:
        try:
            resp = self._session.post(
                self._reconnect_url(camera_id),
                timeout=self.timeout_ms / 1000.0,
            )
            resp.raise_for_status()
            return resp.json()
        except Exception as exc:
            logger.warning("video_hub reconnect 失败 camera_id=%s: %s", camera_id, exc)
            return None


video_hub_client = VideoHubClient()
