from __future__ import annotations

import logging

from prometheus_client import Counter, Gauge

logger = logging.getLogger(__name__)

STREAM_CONNECTION_COUNT = Gauge(
    "ai_stream_connections",
    "当前活跃的流连接数",
)

STREAM_FRAME_DROPPED = Counter(
    "ai_stream_frames_dropped_total",
    "被丢弃的帧总数",
    ["task_code", "reason"],
)

STREAM_RECONNECT_COUNT = Counter(
    "ai_stream_reconnect_total",
    "流重连次数",
    ["task_code"],
)


def increment_stream_connections() -> None:
    STREAM_CONNECTION_COUNT.inc()


def decrement_stream_connections() -> None:
    STREAM_CONNECTION_COUNT.dec()


def record_frame_dropped(task_code: str, reason: str = "unknown") -> None:
    STREAM_FRAME_DROPPED.labels(task_code=task_code, reason=reason).inc()


def record_stream_reconnect(task_code: str) -> None:
    STREAM_RECONNECT_COUNT.labels(task_code=task_code).inc()
