from __future__ import annotations

import asyncio
import io
import logging
import time
import uuid
from typing import TYPE_CHECKING

import av
import numpy as np
from aiortc import MediaStreamTrack, RTCPeerConnection, RTCSessionDescription
from PIL import Image

if TYPE_CHECKING:
    from app.video_hub.frame_cache import FrameCache

logger = logging.getLogger(__name__)


class VideoStreamTrack(MediaStreamTrack):
    kind = "video"

    def __init__(
        self,
        camera_id: int,
        frame_cache: FrameCache,
        target_fps: float = 10.0,
    ):
        super().__init__()
        self._camera_id = camera_id
        self._frame_cache = frame_cache
        self._target_fps = target_fps
        self._frame_interval = 1.0 / target_fps
        self._last_send_time: float = 0.0
        self._last_frame_bytes: bytes | None = None
        self._last_av_frame: av.VideoFrame | None = None
        self._last_version: int = 0

    async def recv(self) -> av.VideoFrame:
        loop = asyncio.get_event_loop()

        while True:
            now = time.time()
            elapsed = now - self._last_send_time
            wait = self._frame_interval - elapsed
            if wait > 0:
                await asyncio.sleep(wait)

            try:
                snapshot = await loop.run_in_executor(
                    None,
                    self._frame_cache.wait_for_new_frame,
                    2.0,
                    self._last_version,
                )
            except Exception:
                logger.debug("取帧异常，重发上一帧 camera_id=%d", self._camera_id)
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            if snapshot is None:
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            jpeg_bytes = snapshot.get("jpeg_bytes")
            if jpeg_bytes is None:
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            try:
                img = Image.open(io.BytesIO(jpeg_bytes))
                arr = np.asarray(img)
                frame = av.VideoFrame.from_ndarray(arr, format="rgb24")
            except Exception:
                logger.debug("JPEG 解码异常，重发上一帧 camera_id=%d", self._camera_id)
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            self._last_frame_bytes = jpeg_bytes
            self._last_av_frame = frame
            self._last_version = snapshot.get("_version", self._last_version)
            self._last_send_time = time.time()
            return frame

    def stop(self):
        super().stop()


class WebrtcSessionManager:
    def __init__(self, registry=None, max_sessions_per_camera: int = 10):
        self._registry = registry
        self._sessions: dict[str, RTCPeerConnection] = {}
        self._session_to_camera: dict[str, int] = {}
        self._max_sessions_per_camera = max_sessions_per_camera

    @property
    def registry(self):
        if self._registry is not None:
            return self._registry
        from app.video_hub import video_hub_registry
        return video_hub_registry

    def _count_sessions_for_camera(self, camera_id: int) -> int:
        return sum(1 for cid in self._session_to_camera.values() if cid == camera_id)

    async def create_whip_session(
        self, camera_id: int, sdp_offer: str
    ) -> tuple[str, str]:
        session = self.registry.get_session(camera_id)
        if session is None:
            raise ValueError(f"camera_id={camera_id} 视频会话尚未建立")

        if session.state == "CIRCUIT_OPEN":
            session.activate_from_circuit_open()

        count = self._count_sessions_for_camera(camera_id)
        if count >= self._max_sessions_per_camera:
            raise ValueError(
                f"camera_id={camera_id} WebRTC 会话数已达上限 {self._max_sessions_per_camera}"
            )

        pc = RTCPeerConnection()
        video_track = VideoStreamTrack(camera_id, session.frame_cache)
        pc.addTrack(video_track)

        await pc.setRemoteDescription(RTCSessionDescription(sdp_offer, "offer"))
        answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)

        session_id = str(uuid.uuid4())

        @pc.on("connectionstatechange")
        async def _on_state_change():
            if pc.connectionState in ("closed", "failed"):
                self._sessions.pop(session_id, None)
                self._session_to_camera.pop(session_id, None)

        self._sessions[session_id] = pc
        self._session_to_camera[session_id] = camera_id

        return pc.localDescription.sdp, session_id

    async def delete_whip_session(self, session_id: str) -> None:
        pc = self._sessions.pop(session_id, None)
        self._session_to_camera.pop(session_id, None)
        if pc is not None:
            await pc.close()
