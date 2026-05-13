from __future__ import annotations

import logging
from collections.abc import Iterator
from threading import Lock, Thread
from time import sleep, time

import requests

from app.video_hub.frame_cache import FrameCache

logger = logging.getLogger(__name__)


def _extract_boundary(content_type: str) -> bytes:
    for part in content_type.split(";"):
        key, _, value = part.strip().partition("=")
        if key.lower() == "boundary" and value:
            return value.strip().strip('"').encode("utf-8")
    return b"frame"


def _parse_jpeg_size(jpeg_bytes: bytes) -> tuple[int, int]:
    if len(jpeg_bytes) < 24 or jpeg_bytes[0:2] != b"\xff\xd8":
        return (0, 0)
    index = 2
    while index + 9 < len(jpeg_bytes):
        if jpeg_bytes[index] != 0xFF:
            index += 1
            continue
        marker = jpeg_bytes[index + 1]
        if marker in {0xC0, 0xC2}:
            height = int.from_bytes(jpeg_bytes[index + 5 : index + 7], "big")
            width = int.from_bytes(jpeg_bytes[index + 7 : index + 9], "big")
            return (width, height)
        if marker in {0xD8, 0xD9}:
            index += 2
            continue
        segment_length = int.from_bytes(jpeg_bytes[index + 2 : index + 4], "big")
        if segment_length <= 0:
            break
        index += 2 + segment_length
    return (0, 0)


class VideoHubSession:
    def __init__(
        self,
        camera_id: int,
        source_url: str,
        connect_timeout_sec: float = 3.0,
        read_timeout_sec: float = 10.0,
        retry_delay_sec: float = 1.5,
    ):
        self.camera_id = camera_id
        self.source_url = source_url
        self.connect_timeout_sec = connect_timeout_sec
        self.read_timeout_sec = read_timeout_sec
        self.retry_delay_sec = retry_delay_sec
        self.http_session = requests.Session()
        self.http_session.trust_env = False
        self.frame_cache = FrameCache()
        self._start_lock = Lock()
        self._viewer_lock = Lock()
        self._started = False
        self._connected = False
        self._viewer_count = 0
        self._thread: Thread | None = None
        self._last_frame_at: int | None = None

    def ensure_started(self):
        with self._start_lock:
            if self._started:
                return
            self._thread = Thread(target=self._run_loop, daemon=True)
            self._thread.start()
            self._started = True

    def get_latest_frame(self) -> dict | None:
        return self.frame_cache.latest()

    def status_dict(self) -> dict:
        latest = self.frame_cache.latest()
        return {
            "camera_id": self.camera_id,
            "connected": self._connected,
            "last_frame_at": self._last_frame_at,
            "source_width": latest["frame_width"] if latest else 0,
            "source_height": latest["frame_height"] if latest else 0,
            "last_error": self.frame_cache.last_error(),
            "viewer_count": self._viewer_count,
            "source_url": self.source_url,
        }

    def open_stream(self) -> Iterator[bytes]:
        self.ensure_started()
        with self._viewer_lock:
            self._viewer_count += 1

        def generator():
            version = 0
            try:
                while True:
                    frame = self.frame_cache.wait_for_new_frame(5.0, after_version=version)
                    if frame is not None:
                        version = frame["_version"]
                        yield self._multipart_frame(frame["jpeg_bytes"], "image/jpeg")
                        continue
                    current = self.frame_cache.latest()
                    if current is not None:
                        yield self._multipart_frame(current["jpeg_bytes"], "image/jpeg")
                        sleep(0.05)
                    else:
                        sleep(0.2)
            finally:
                with self._viewer_lock:
                    self._viewer_count = max(0, self._viewer_count - 1)

        return generator()

    def _multipart_frame(self, payload: bytes, content_type: str) -> bytes:
        header = (
            b"--frame\r\n"
            + f"Content-Type: {content_type}\r\n".encode("utf-8")
            + f"Content-Length: {len(payload)}\r\n\r\n".encode("utf-8")
        )
        return header + payload + b"\r\n"

    def _run_loop(self):
        while True:
            try:
                self._consume_stream()
            except Exception as exc:
                self._connected = False
                self.frame_cache.set_error(f"上游视频流异常: {exc}")
                logger.warning("camera=%s 拉流异常，%.1fs 后重试: %s", self.camera_id, self.retry_delay_sec, exc)
                sleep(self.retry_delay_sec)

    def _consume_stream(self):
        response = None
        try:
            response = self.http_session.get(
                self.source_url,
                stream=True,
                timeout=(self.connect_timeout_sec, self.read_timeout_sec),
            )
            response.raise_for_status()
            content_type = response.headers.get("Content-Type", "")
            boundary = _extract_boundary(content_type)
            buffer = bytearray()
            self._connected = True
            self.frame_cache.set_error("")
            logger.info("camera=%s 已连接上游: %s (boundary=%s)", self.camera_id, self.source_url, boundary)

            for chunk in response.iter_content(chunk_size=4096):
                if not chunk:
                    continue
                buffer.extend(chunk)
                while True:
                    frame = self._pop_frame(buffer, boundary)
                    if frame is None:
                        break
                    width, height = _parse_jpeg_size(frame)
                    timestamp = int(time() * 1000)
                    self.frame_cache.update(frame, width, height, timestamp)
                    self._last_frame_at = timestamp
        finally:
            self._connected = False
            if response is not None:
                try:
                    response.close()
                except Exception:
                    pass

    def _pop_frame(self, buffer: bytearray, boundary: bytes) -> bytes | None:
        boundary_token = b"--" + boundary
        start = buffer.find(boundary_token)
        if start < 0:
            return None
        header_start = buffer.find(b"\r\n\r\n", start)
        if header_start < 0:
            return None
        payload_start = header_start + 4
        next_boundary = buffer.find(boundary_token, payload_start)
        if next_boundary < 0:
            return None
        payload = bytes(buffer[payload_start : next_boundary - 2])
        del buffer[:next_boundary]
        return payload
