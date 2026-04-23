from __future__ import annotations

import logging

from prometheus_client import Counter, Gauge

logger = logging.getLogger(__name__)

TASK_COUNT = Gauge(
    "ai_task_count",
    "当前运行中的推理任务数",
)

TASK_STARTED = Counter(
    "ai_task_started_total",
    "任务启动总次数",
)

TASK_STOPPED = Counter(
    "ai_task_stopped_total",
    "任务停止总次数",
    ["reason"],
)

ALERT_PUBLISHED = Counter(
    "ai_alert_published_total",
    "报警事件发布总次数",
    ["channel", "status"],
)


def increment_task_count() -> None:
    TASK_COUNT.inc()


def decrement_task_count() -> None:
    TASK_COUNT.dec()


def record_task_started() -> None:
    TASK_STARTED.inc()
    increment_task_count()


def record_task_stopped(reason: str = "normal") -> None:
    TASK_STOPPED.labels(reason=reason).inc()
    decrement_task_count()


def record_alert_published(channel: str, status: str = "success") -> None:
    ALERT_PUBLISHED.labels(channel=channel, status=status).inc()
    logger.debug("recorded alert published: channel=%s status=%s", channel, status)
