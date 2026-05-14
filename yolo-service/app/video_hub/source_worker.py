from __future__ import annotations

import logging
from collections.abc import Iterator
from enum import Enum
from threading import Lock, Thread
from time import sleep, time

import requests

from app.video_hub.frame_cache import FrameCache

logger = logging.getLogger(__name__)


class SessionState(str, Enum):
    CONNECTING = "CONNECTING"
    CONNECTED = "CONNECTED"
    STALE = "STALE"
    CIRCUIT_OPEN = "CIRCUIT_OPEN"


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
        stale_frame_timeout_sec: float = 5.0,
    ):
        self.camera_id = camera_id
        self.source_url = source_url
        self.connect_timeout_sec = connect_timeout_sec
        self.read_timeout_sec = read_timeout_sec
        self.stale_frame_timeout_sec = stale_frame_timeout_sec
        self.http_session = requests.Session()
        self.http_session.trust_env = False
        self.frame_cache = FrameCache()
        self._start_lock = Lock()
        self._viewer_lock = Lock()
        self._state_lock = Lock()
        self._started = False
        self._viewer_count = 0
        self._thread: Thread | None = None
        self._last_frame_at: int | None = None
        self._state = SessionState.CONNECTING
        self._consecutive_failures = 0
        self._last_failure_at: int | None = None
        self._last_failure_detail: str | None = None
        self._circuit_open_reason: str | None = None
        self._stopped = False

    @property
    def state(self) -> str:
        with self._state_lock:
            return self._state.value

    @property
    def consecutive_failures(self) -> int:
        with self._state_lock:
            return self._consecutive_failures

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
        with self._state_lock:
            current_state = self._state
            circuit_reason = self._circuit_open_reason
            failures = self._consecutive_failures
            last_fail_at = self._last_failure_at
            last_fail_detail = self._last_failure_detail
        return {
            "camera_id": self.camera_id,
            "state": current_state.value,
            "connected": current_state == SessionState.CONNECTED,
            "circuit_open_reason": circuit_reason,
            "consecutive_failures": failures,
            "last_failure_at": last_fail_at,
            "last_failure_detail": last_fail_detail,
            "stale_frame_timeout_sec": self.stale_frame_timeout_sec,
            "last_frame_at": self._last_frame_at,
            "source_width": latest["frame_width"] if latest else 0,
            "source_height": latest["frame_height"] if latest else 0,
            "last_error": self.frame_cache.last_error(),
            "viewer_count": self._viewer_count,
            "source_url": self.source_url,
            "retry_delay_sec": self._calc_retry_delay(failures),
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

    def stop(self):
        self._stopped = True

    def activate_from_circuit_open(self):
        with self._state_lock:
            if self._state == SessionState.CIRCUIT_OPEN:
                self._consecutive_failures = 0
                self._circuit_open_reason = None
                self._state = SessionState.CONNECTING

    def _transition_to_connected(self):
        with self._state_lock:
            self._state = SessionState.CONNECTED

    def _transition_to_connecting(self):
        with self._state_lock:
            self._state = SessionState.CONNECTING

    def _transition_to_stale(self):
        with self._state_lock:
            self._state = SessionState.STALE

    def _transition_to_circuit_open(self, reason: str):
        with self._state_lock:
            self._state = SessionState.CIRCUIT_OPEN
            self._circuit_open_reason = reason

    def _record_failure(self, detail: str):
        with self._state_lock:
            self._consecutive_failures += 1
            self._last_failure_at = int(time() * 1000)
            self._last_failure_detail = detail
            if self._consecutive_failures >= 10:
                self._state = SessionState.CIRCUIT_OPEN
                self._circuit_open_reason = (
                    f"连续连接失败{self._consecutive_failures}次: {detail}"
                )
        self.frame_cache.set_error(f"上游视频流异常: {detail}")

    def _record_success(self):
        with self._state_lock:
            self._consecutive_failures = 0
            self._circuit_open_reason = None
        self.frame_cache.set_error("")

    def _calc_retry_delay(self, consecutive_failures: int) -> float:
        if consecutive_failures <= 2:
            return 1.5
        elif consecutive_failures <= 4:
            return 3.0
        elif consecutive_failures <= 6:
            return 5.0
        elif consecutive_failures <= 9:
            return 10.0
        else:
            return 60.0

    def _check_stale_frame(self) -> bool:
        with self._state_lock:
            if self._state != SessionState.CONNECTED:
                return False
        last_at = self.frame_cache.last_frame_at()
        if last_at is None or time() - last_at > self.stale_frame_timeout_sec:
            self._transition_to_stale()
            return True
        return False

    def _multipart_frame(self, payload: bytes, content_type: str) -> bytes:
        header = (
            b"--frame\r\n"
            + f"Content-Type: {content_type}\r\n".encode("utf-8")
            + f"Content-Length: {len(payload)}\r\n\r\n".encode("utf-8")
        )
        return header + payload + b"\r\n"

    def _run_loop(self):
        while not self._stopped:
            with self._state_lock:
                current_state = self._state
            if current_state == SessionState.CIRCUIT_OPEN:
                sleep(60.0)
                if self._stopped:
                    break
                continue

            try:
                self._consume_stream()
            except Exception as exc:
                with self._state_lock:
                    is_stale = self._state == SessionState.STALE
                if is_stale:
                    logger.info("camera=%s 无帧超时，立即重连", self.camera_id)
                    self._transition_to_connecting()
                    continue
                self._record_failure(str(exc))
                with self._state_lock:
                    is_circuit_open = self._state == SessionState.CIRCUIT_OPEN
                if not is_circuit_open:
                    self._transition_to_connecting()
                delay = self._calc_retry_delay(self._consecutive_failures)
                logger.warning(
                    "camera=%s 拉流异常，%.1fs 后重试: %s",
                    self.camera_id,
                    delay,
                    exc,
                )
                sleep(delay)
                continue

            if self._stopped:
                break

            with self._state_lock:
                post_state = self._state
            if post_state == SessionState.STALE:
                logger.info("camera=%s 无帧超时，立即重连", self.camera_id)
                self._transition_to_connecting()
                continue

            self._record_failure("上游连接正常关闭")
            with self._state_lock:
                is_circuit_open = self._state == SessionState.CIRCUIT_OPEN
            if not is_circuit_open:
                self._transition_to_connecting()
            delay = self._calc_retry_delay(self._consecutive_failures)
            logger.warning(
                "camera=%s 上游连接关闭，%.1fs 后重连",
                self.camera_id,
                delay,
            )
            sleep(delay)

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
            self._transition_to_connected()
            self._record_success()
            logger.info(
                "camera=%s 已连接上游: %s (boundary=%s)",
                self.camera_id,
                self.source_url,
                boundary,
            )

            for chunk in response.iter_content(chunk_size=4096):
                if self._stopped:
                    break
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
                if self._check_stale_frame():
                    break
        finally:
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
