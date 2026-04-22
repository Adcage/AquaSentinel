from __future__ import annotations

from dataclasses import dataclass
import time
from typing import Any

from app.services.tracker_service import TrackedObject


@dataclass
class DrowningDecision:
    track_id: str
    triggered: bool
    posture_score: float
    thermal_score: float
    duration_sec: float
    posture_abnormal: bool
    thermal_abnormal: bool
    duration_abnormal: bool


def _as_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except Exception:
        return default


def _as_bool(value: Any, default: bool = False) -> bool:
    if isinstance(value, bool):
        return value
    if value is None:
        return default
    text = str(value).strip().lower()
    if text in {"1", "true", "yes", "on"}:
        return True
    if text in {"0", "false", "no", "off"}:
        return False
    return default


class DrowningRuleEvaluator:
    def __init__(
        self,
        min_duration_sec: float = 3.0,
        posture_threshold: float = 0.7,
        thermal_threshold: float = 0.85,
        cooldown_sec: float = 15.0,
        max_idle_sec: float = 10.0,
        decay_window_sec: float = 1.0,
        clear_threshold_ratio: float = 0.4,
    ):
        self._min_duration_sec = max(0.5, min_duration_sec)
        self._posture_threshold = min(max(posture_threshold, 0.0), 1.0)
        self._thermal_threshold = min(max(thermal_threshold, 0.0), 1.0)
        self._cooldown_sec = max(1.0, cooldown_sec)
        self._max_idle_sec = max(1.0, max_idle_sec)
        self._decay_window_sec = max(0.1, decay_window_sec)
        self._clear_threshold_ratio = min(max(clear_threshold_ratio, 0.1), 0.9)
        self._states: dict[str, dict[str, float | None]] = {}

    def evaluate(
        self,
        track: TrackedObject,
        timestamp: float | None = None,
    ) -> DrowningDecision:
        now = time.monotonic() if timestamp is None else timestamp
        state = self._states.setdefault(
            track.track_id,
            {
                "last_seen": now,
                "abnormal_duration": 0.0,
                "miss_duration": 0.0,
                "last_triggered_at": None,
                "active": 0.0,
            },
        )

        last_seen = state.get("last_seen")
        delta = 0.0
        if last_seen is not None:
            delta = max(0.0, min(now - float(last_seen), self._max_idle_sec))

        posture_score = self._resolve_posture_score(track)
        thermal_score = self._resolve_thermal_score(track)
        posture_abnormal = posture_score >= self._posture_threshold
        thermal_abnormal = thermal_score >= self._thermal_threshold

        if posture_abnormal and thermal_abnormal:
            state["abnormal_duration"] = (
                float(state.get("abnormal_duration") or 0.0) + delta
            )
            state["miss_duration"] = 0.0
        else:
            miss_duration = float(state.get("miss_duration") or 0.0) + delta
            if miss_duration > self._decay_window_sec:
                decay = miss_duration - self._decay_window_sec
                state["abnormal_duration"] = max(
                    0.0,
                    float(state.get("abnormal_duration") or 0.0) - decay,
                )
                miss_duration = self._decay_window_sec
            state["miss_duration"] = miss_duration

        duration_sec = float(state.get("abnormal_duration") or 0.0)

        duration_abnormal = duration_sec >= self._min_duration_sec
        active = bool(float(state.get("active") or 0.0) >= 1.0)
        clear_threshold = self._min_duration_sec * self._clear_threshold_ratio
        if active and duration_sec <= clear_threshold:
            active = False

        trigger_allowed = True
        last_triggered_at = state.get("last_triggered_at")
        if last_triggered_at is not None:
            trigger_allowed = (
                now - _as_float(last_triggered_at, 0.0) >= self._cooldown_sec
            )

        triggered = (
            posture_abnormal
            and thermal_abnormal
            and duration_abnormal
            and trigger_allowed
        )
        if triggered:
            state["last_triggered_at"] = now
            active = True

        state["active"] = 1.0 if active else 0.0

        state["last_seen"] = now
        self._gc(now)
        return DrowningDecision(
            track_id=track.track_id,
            triggered=triggered,
            posture_score=posture_score,
            thermal_score=thermal_score,
            duration_sec=duration_sec,
            posture_abnormal=posture_abnormal,
            thermal_abnormal=thermal_abnormal,
            duration_abnormal=duration_abnormal,
        )

    def _resolve_posture_score(self, track: TrackedObject) -> float:
        extra_json = track.extra_json or {}
        posture_score = extra_json.get("posture_score")
        if posture_score is None:
            posture_score = extra_json.get("pose_score")
        if posture_score is not None:
            return min(max(_as_float(posture_score, 0.0), 0.0), 1.0)

        if _as_bool(extra_json.get("posture_abnormal"), False) or _as_bool(
            extra_json.get("pose_abnormal"),
            False,
        ):
            return 1.0

        width = max(0.0, track.x_max - track.x_min)
        height = max(1.0, track.y_max - track.y_min)
        ratio = width / height
        if ratio >= 1.4:
            return 0.95
        if ratio >= 1.1:
            return 0.75
        return 0.3

    def _resolve_thermal_score(self, track: TrackedObject) -> float:
        extra_json = track.extra_json or {}
        thermal_score = extra_json.get("thermal_score")
        if thermal_score is None:
            thermal_score = extra_json.get("temperature_score")
        if thermal_score is not None:
            return min(max(_as_float(thermal_score, 0.0), 0.0), 1.0)

        if _as_bool(extra_json.get("thermal_abnormal"), False) or _as_bool(
            extra_json.get("heat_abnormal"),
            False,
        ):
            return 1.0

        return min(max(track.confidence, 0.0), 1.0)

    def _gc(self, now: float):
        stale_track_ids: list[str] = []
        for track_id, state in self._states.items():
            last_seen = state.get("last_seen")
            if last_seen is None:
                stale_track_ids.append(track_id)
                continue
            if now - float(last_seen) > self._max_idle_sec:
                stale_track_ids.append(track_id)
        for track_id in stale_track_ids:
            self._states.pop(track_id, None)
