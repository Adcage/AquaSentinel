from __future__ import annotations

import logging
import time

from prometheus_client import Counter, Gauge, Histogram

logger = logging.getLogger(__name__)

INFERENCE_COUNT = Counter(
    "ai_inference_total",
    "AI 推理总次数",
    ["model_version", "status"],
)

INFERENCE_LATENCY = Histogram(
    "ai_inference_latency_seconds",
    "AI 推理延迟（秒）",
    ["model_version"],
    buckets=[0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0],
)

MODEL_LOAD_TIME = Histogram(
    "ai_model_load_seconds",
    "AI 模型加载时间（秒）",
    ["model_version"],
)

INFERENCE_FPS = Gauge(
    "ai_inference_fps",
    "AI 推理帧率（每秒处理帧数）",
    ["task_code"],
)


def record_inference(
    model_version: str, latency_sec: float, status: str = "success"
) -> None:
    INFERENCE_COUNT.labels(model_version=model_version, status=status).inc()
    INFERENCE_LATENCY.labels(model_version=model_version).observe(latency_sec)
    logger.debug(
        "recorded inference: model=%s latency=%.3fs status=%s",
        model_version,
        latency_sec,
        status,
    )


def record_model_load(model_version: str, load_sec: float) -> None:
    MODEL_LOAD_TIME.labels(model_version=model_version).observe(load_sec)
    logger.info(
        "recorded model load: model=%s load_time=%.3fs", model_version, load_sec
    )


def set_inference_fps(task_code: str, fps: float) -> None:
    INFERENCE_FPS.labels(task_code=task_code).set(fps)
