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
        self._first_frame_logged: bool = False

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
            if not self._first_frame_logged:
                self._first_frame_logged = True
                logger.info(
                    "WebRTC 首帧发送 camera_id=%d version=%d size=%d",
                    self._camera_id,
                    self._last_version,
                    len(jpeg_bytes),
                )
            elif self._last_version % 100 == 0:
                logger.info(
                    "WebRTC 发帧 camera_id=%d version=%d size=%d",
                    self._camera_id,
                    self._last_version,
                    len(jpeg_bytes),
                )
            return frame

    def stop(self):
        super().stop()


class WebrtcSessionManager:
    def __init__(self, registry=None, max_sessions_per_camera: int = 10):
        self._registry = registry
        self._sessions: dict[str, RTCPeerConnection] = {}
        self._session_to_camera: dict[str, int] = {}
        self._max_sessions_per_camera = max_sessions_per_camera
        self._current_preferred_ip: str | None = None

    @property
    def registry(self):
        if self._registry is not None:
            return self._registry
        from app.video_hub import video_hub_registry
        return video_hub_registry

    def _count_sessions_for_camera(self, camera_id: int) -> int:
        return sum(1 for cid in self._session_to_camera.values() if cid == camera_id)

    async def _log_pc_stats_later(
        self, pc: RTCPeerConnection, session_id: str, camera_id: int, delay_sec: float = 3.0
    ) -> None:
        await asyncio.sleep(delay_sec)
        if pc.connectionState in ("closed", "failed"):
            return
        try:
            report = await pc.getStats()
        except Exception as exc:
            logger.warning(
                "WebRTC stats 获取失败 session=%s camera_id=%d error=%s",
                session_id[:8],
                camera_id,
                exc,
            )
            return

        candidate_pair = None
        transport = None
        outbound_video = None
        candidate_pairs: list[dict[str, object | None]] = []
        for stat in report.values():
            stat_type = getattr(stat, "type", "")
            if stat_type == "candidate-pair" and (
                getattr(stat, "selected", False) or getattr(stat, "nominated", False)
            ):
                candidate_pair = stat
            if stat_type == "candidate-pair":
                candidate_pairs.append(
                    {
                        "state": getattr(stat, "state", None),
                        "nominated": getattr(stat, "nominated", None),
                        "selected": getattr(stat, "selected", None),
                        "localCandidateId": getattr(stat, "localCandidateId", None),
                        "remoteCandidateId": getattr(stat, "remoteCandidateId", None),
                        "bytesSent": getattr(stat, "bytesSent", None),
                        "bytesReceived": getattr(stat, "bytesReceived", None),
                    }
                )
            elif stat_type == "transport":
                transport = stat
            elif stat_type == "outbound-rtp" and getattr(stat, "kind", "") == "video":
                outbound_video = stat

        logger.info(
            "WebRTC stats session=%s camera_id=%d state=%s ice=%s dtls=%s bytesSent=%s packetsSent=%s pairState=%s rtt=%s",
            session_id[:8],
            camera_id,
            pc.connectionState,
            pc.iceConnectionState,
            getattr(transport, "dtlsState", None),
            getattr(outbound_video, "bytesSent", None),
            getattr(outbound_video, "packetsSent", None),
            getattr(candidate_pair, "state", None),
            getattr(candidate_pair, "currentRoundTripTime", None),
        )
        logger.info(
            "WebRTC candidate pairs session=%s camera_id=%d pairs=%s",
            session_id[:8],
            camera_id,
            candidate_pairs,
        )

    async def create_whip_session(
        self, camera_id: int, sdp_offer: str, session=None, preferred_ip: str | None = None
    ) -> tuple[str, str]:
        self._current_preferred_ip = preferred_ip
        if session is None:
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

        if pc.iceGatheringState != "complete":
            gather_event = asyncio.Event()

            @pc.on("icegatheringstatechange")
            async def _on_gather():
                if pc.iceGatheringState == "complete":
                    gather_event.set()

            await asyncio.wait_for(gather_event.wait(), timeout=5.0)
            logger.info(
                "ICE gathering 完成 camera_id=%d candidates=%d",
                camera_id,
                pc.localDescription.sdp.count("a=candidate"),
            )

        session_id = str(uuid.uuid4())

        @pc.on("connectionstatechange")
        async def _on_state_change():
            state = pc.connectionState
            logger.info(
                "WebRTC 连接状态变更 session=%s camera_id=%d state=%s",
                session_id[:8],
                camera_id,
                state,
            )
            if state in ("closed", "failed"):
                self._sessions.pop(session_id, None)
                self._session_to_camera.pop(session_id, None)

        @pc.on("iceconnectionstatechange")
        async def _on_ice_state_change():
            logger.info(
                "WebRTC ICE 状态变更 session=%s camera_id=%d state=%s",
                session_id[:8],
                camera_id,
                pc.iceConnectionState,
            )

        @pc.on("signalingstatechange")
        async def _on_signaling_state_change():
            logger.info(
                "WebRTC signaling 状态变更 session=%s camera_id=%d state=%s",
                session_id[:8],
                camera_id,
                pc.signalingState,
            )

        @pc.on("datachannel")
        async def _on_datachannel(channel):
            logger.info("WebRTC 数据通道 session=%s label=%s", session_id[:8], channel.label)

        self._sessions[session_id] = pc
        self._session_to_camera[session_id] = camera_id
        asyncio.create_task(self._log_pc_stats_later(pc, session_id, camera_id))

        return pc.localDescription.sdp, session_id

    async def delete_whip_session(self, session_id: str) -> None:
        pc = self._sessions.pop(session_id, None)
        self._session_to_camera.pop(session_id, None)
        if pc is not None:
            await pc.close()
