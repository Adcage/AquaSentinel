from __future__ import annotations

import logging
from threading import Lock

from app.video_hub.source_worker import VideoHubSession

logger = logging.getLogger(__name__)


class VideoHubRegistry:
    def __init__(self, session_factory=None):
        self._session_factory = session_factory or VideoHubSession
        self._sessions: dict[int, VideoHubSession] = {}
        self._lock = Lock()

    def ensure_session(self, camera_id: int, source_url: str) -> VideoHubSession:
        created = False
        with self._lock:
            session = self._sessions.get(camera_id)
            if session is None:
                session = self._session_factory(camera_id, source_url)
                self._sessions[camera_id] = session
                created = True
            elif session.source_url != source_url:
                logger.info(
                    "camera=%s source_url 变更: %s -> %s，更新会话",
                    camera_id, session.source_url, source_url,
                )
                session.source_url = source_url
        if created:
            session.ensure_started()
        return session

    def get_session(self, camera_id: int) -> VideoHubSession | None:
        with self._lock:
            return self._sessions.get(camera_id)

    def remove_session(self, camera_id: int) -> None:
        with self._lock:
            session = self._sessions.pop(camera_id, None)
        if session is not None:
            session.stop()
