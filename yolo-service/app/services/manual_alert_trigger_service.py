from __future__ import annotations

import time
from typing import Any

import requests


def build_trigger_payload(
    camera_id: int,
    posture_score: float,
    thermal_score: float,
    duration_sec: float,
) -> dict[str, Any]:
    return {
        "camera_id": camera_id,
        "posture_score": posture_score,
        "thermal_score": thermal_score,
        "duration_sec": duration_sec,
    }


def parse_command(raw: str) -> tuple[str, dict[str, Any]]:
    text = (raw or "").strip()
    if not text:
        raise ValueError("empty command")

    lower_text = text.lower()
    if lower_text in {"q", "quit", "exit"}:
        return "exit", {}
    if lower_text in {"h", "help", "?"}:
        return "help", {}

    parts = text.split()
    if len(parts) == 1 and parts[0].isdigit():
        return "trigger", {"camera_id": int(parts[0]), "times": 1, "interval_sec": 1.0}

    if parts[0].lower() != "trigger":
        raise ValueError("unsupported command")

    if len(parts) < 2:
        raise ValueError("missing camera id")

    try:
        camera_id = int(parts[1])
    except Exception as exc:
        raise ValueError("camera id must be integer") from exc
    if camera_id <= 0:
        raise ValueError("camera id must be positive")

    times = 1
    interval_sec = 1.0
    if len(parts) >= 3:
        times = int(parts[2])
    if len(parts) >= 4:
        interval_sec = float(parts[3])
    if times <= 0:
        raise ValueError("times must be positive")
    if interval_sec < 0:
        raise ValueError("interval must be >= 0")

    return "trigger", {
        "camera_id": camera_id,
        "times": times,
        "interval_sec": interval_sec,
    }


def trigger_once(
    *,
    base_url: str,
    payload: dict[str, Any],
    timeout_sec: float = 10.0,
) -> tuple[bool, dict[str, Any]]:
    url = f"{base_url.rstrip('/')}/engine/test/trigger-alert"
    response = requests.post(url, json=payload, timeout=timeout_sec)
    data: dict[str, Any]
    try:
        data = response.json()
    except Exception:
        data = {"raw": response.text}

    if response.status_code != 200:
        return False, data

    payload_data = data.get("data") if isinstance(data, dict) else {}
    triggered = bool(isinstance(payload_data, dict) and payload_data.get("triggered"))
    return triggered, data


def trigger_many(
    *,
    base_url: str,
    payload: dict[str, Any],
    times: int,
    interval_sec: float,
    timeout_sec: float = 10.0,
) -> list[tuple[bool, dict[str, Any]]]:
    results: list[tuple[bool, dict[str, Any]]] = []
    for index in range(times):
        ok, data = trigger_once(
            base_url=base_url, payload=payload, timeout_sec=timeout_sec
        )
        results.append((ok, data))
        if index < times - 1 and interval_sec > 0:
            time.sleep(interval_sec)
    return results
