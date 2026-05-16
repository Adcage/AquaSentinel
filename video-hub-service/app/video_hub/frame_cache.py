from __future__ import annotations

from dataclasses import dataclass
from threading import Condition
from time import time


@dataclass
class FrameSnapshot:
    jpeg_bytes: bytes
    frame_width: int
    frame_height: int
    timestamp: int


class FrameCache:
    def __init__(self):
        self._condition = Condition()
        self._snapshot: FrameSnapshot | None = None
        self._version: int = 0
        self._last_error = "尚未开始拉流"
        self._last_frame_at: float | None = None

    def update(
        self,
        jpeg_bytes: bytes,
        frame_width: int,
        frame_height: int,
        timestamp: int | None = None,
    ):
        with self._condition:
            self._snapshot = FrameSnapshot(
                jpeg_bytes=jpeg_bytes,
                frame_width=frame_width,
                frame_height=frame_height,
                timestamp=timestamp or int(time() * 1000),
            )
            self._version += 1
            self._last_error = ""
            self._last_frame_at = time()
            self._condition.notify_all()

    def set_error(self, message: str):
        with self._condition:
            self._last_error = message
            self._condition.notify_all()

    def latest(self) -> dict | None:
        with self._condition:
            if self._snapshot is None:
                return None
            return {
                "jpeg_bytes": self._snapshot.jpeg_bytes,
                "frame_width": self._snapshot.frame_width,
                "frame_height": self._snapshot.frame_height,
                "timestamp": self._snapshot.timestamp,
            }

    def wait_for_frame(self, timeout_sec: float) -> dict | None:
        with self._condition:
            if self._snapshot is None:
                self._condition.wait(timeout_sec)
            if self._snapshot is None:
                return None
            return {
                "jpeg_bytes": self._snapshot.jpeg_bytes,
                "frame_width": self._snapshot.frame_width,
                "frame_height": self._snapshot.frame_height,
                "timestamp": self._snapshot.timestamp,
            }

    def wait_for_new_frame(self, timeout_sec: float, after_version: int = 0) -> dict | None:
        with self._condition:
            deadline = time() + timeout_sec
            while self._version <= after_version:
                remaining = deadline - time()
                if remaining <= 0:
                    break
                self._condition.wait(remaining)
            if self._snapshot is None or self._version <= after_version:
                return None
            return {
                "jpeg_bytes": self._snapshot.jpeg_bytes,
                "frame_width": self._snapshot.frame_width,
                "frame_height": self._snapshot.frame_height,
                "timestamp": self._snapshot.timestamp,
                "_version": self._version,
            }

    def last_error(self) -> str:
        with self._condition:
            return self._last_error

    def reset_frame_timestamp(self):
        with self._condition:
            self._last_frame_at = None

    def last_frame_at(self) -> float | None:
        return self._last_frame_at
