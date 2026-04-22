from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path

from flask import current_app

from app.core.errors import BusinessError

TASK_STATUSES = ["PENDING", "PROCESSING", "SUCCESS", "FAILED"]
TASK_STATUS_SET = set(TASK_STATUSES)


def _backend_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _resolve_labels_path() -> Path:
    configured_path = str(
        current_app.config.get("MODEL_LABELS_PATH", "model/labels.json")
    )
    labels_path = Path(configured_path)
    if not labels_path.is_absolute():
        labels_path = _backend_root() / labels_path
    return labels_path


@lru_cache(maxsize=4)
def _load_labels(labels_path_text: str) -> tuple[str, ...]:
    labels_path = Path(labels_path_text)
    if not labels_path.exists():
        raise BusinessError(
            f"labels file not found: {labels_path_text}", status_code=500
        )
    payload = json.loads(labels_path.read_text(encoding="utf-8"))
    labels = payload.get("labels", [])
    if not isinstance(labels, list):
        raise BusinessError("labels format invalid", status_code=500)
    result: list[str] = []
    for item in labels:
        if not isinstance(item, dict):
            continue
        label = str(item.get("en", "")).strip()
        if label and label not in result:
            result.append(label)
    if not result:
        raise BusinessError("labels list is empty", status_code=500)
    return tuple(result)


def get_label_list() -> list[str]:
    labels_path = _resolve_labels_path()
    return list(_load_labels(labels_path.as_posix()))


def get_label_set() -> set[str]:
    return set(get_label_list())


def ensure_detection_label(label: str):
    if label not in get_label_set():
        raise BusinessError("invalid detection label", status_code=400)


def ensure_task_status(status: str):
    if status not in TASK_STATUS_SET:
        raise BusinessError("invalid task status", status_code=400)
