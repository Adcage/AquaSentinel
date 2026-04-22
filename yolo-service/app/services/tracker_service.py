from __future__ import annotations

from dataclasses import dataclass
import math
import time
from typing import Any


@dataclass
class TrackedObject:
    track_id: str
    x_min: float
    y_min: float
    x_max: float
    y_max: float
    confidence: float
    label: str
    extra_json: dict[str, Any]


def _as_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except Exception:
        return default


def _bbox_from_detection(
    detection: dict[str, Any],
) -> tuple[float, float, float, float]:
    return (
        _as_float(detection.get("x_min")),
        _as_float(detection.get("y_min")),
        _as_float(detection.get("x_max")),
        _as_float(detection.get("y_max")),
    )


def _bbox_iou(
    first: tuple[float, float, float, float],
    second: tuple[float, float, float, float],
) -> float:
    ax1, ay1, ax2, ay2 = first
    bx1, by1, bx2, by2 = second
    inter_x1 = max(ax1, bx1)
    inter_y1 = max(ay1, by1)
    inter_x2 = min(ax2, bx2)
    inter_y2 = min(ay2, by2)

    inter_w = max(0.0, inter_x2 - inter_x1)
    inter_h = max(0.0, inter_y2 - inter_y1)
    inter_area = inter_w * inter_h
    if inter_area <= 0:
        return 0.0

    first_area = max(0.0, ax2 - ax1) * max(0.0, ay2 - ay1)
    second_area = max(0.0, bx2 - bx1) * max(0.0, by2 - by1)
    union = first_area + second_area - inter_area
    if union <= 0:
        return 0.0
    return inter_area / union


def _distance(
    first: tuple[float, float, float, float],
    second: tuple[float, float, float, float],
) -> float:
    ax1, ay1, ax2, ay2 = first
    bx1, by1, bx2, by2 = second
    acx = (ax1 + ax2) / 2.0
    acy = (ay1 + ay2) / 2.0
    bcx = (bx1 + bx2) / 2.0
    bcy = (by1 + by2) / 2.0
    return math.hypot(acx - bcx, acy - bcy)


class _SimpleIouTracker:
    def __init__(self, iou_threshold: float = 0.3, max_age_sec: float = 1.5):
        self._iou_threshold = iou_threshold
        self._max_age_sec = max_age_sec
        self._next_track_id = 1
        self._tracks: dict[int, dict[str, Any]] = {}

    def update(
        self,
        detections: list[dict[str, Any]],
        timestamp: float | None = None,
    ) -> list[TrackedObject]:
        now = time.monotonic() if timestamp is None else timestamp
        self._gc(now)

        tracked_objects: list[TrackedObject] = []
        used_track_ids: set[int] = set()

        for detection in detections:
            bbox = _bbox_from_detection(detection)
            matched_track_id = self._match_track(bbox, used_track_ids)
            if matched_track_id is None:
                matched_track_id = self._new_track_id()

            used_track_ids.add(matched_track_id)
            self._tracks[matched_track_id] = {
                "bbox": bbox,
                "last_seen": now,
                "label": str(detection.get("label") or "person"),
                "confidence": _as_float(detection.get("confidence"), 0.0),
                "extra_json": detection.get("extra_json") or {},
            }
            tracked_objects.append(
                TrackedObject(
                    track_id=f"track_{matched_track_id}",
                    x_min=bbox[0],
                    y_min=bbox[1],
                    x_max=bbox[2],
                    y_max=bbox[3],
                    confidence=_as_float(detection.get("confidence"), 0.0),
                    label=str(detection.get("label") or "person"),
                    extra_json=detection.get("extra_json") or {},
                )
            )
        return tracked_objects

    def _match_track(
        self,
        bbox: tuple[float, float, float, float],
        used_track_ids: set[int],
    ) -> int | None:
        best_id = None
        best_iou = 0.0
        for track_id, track_info in self._tracks.items():
            if track_id in used_track_ids:
                continue
            iou = _bbox_iou(bbox, track_info["bbox"])
            if iou >= self._iou_threshold and iou > best_iou:
                best_iou = iou
                best_id = track_id
        return best_id

    def _new_track_id(self) -> int:
        track_id = self._next_track_id
        self._next_track_id += 1
        return track_id

    def _gc(self, now: float):
        stale_ids: list[int] = []
        for track_id, track_info in self._tracks.items():
            if now - float(track_info["last_seen"]) > self._max_age_sec:
                stale_ids.append(track_id)
        for track_id in stale_ids:
            self._tracks.pop(track_id, None)


class DeepSortTracker:
    def __init__(
        self,
        iou_threshold: float = 0.3,
        max_age_sec: float = 1.5,
        force_simple: bool = False,
    ):
        self._simple = _SimpleIouTracker(
            iou_threshold=iou_threshold,
            max_age_sec=max_age_sec,
        )
        self._backend = "simple"
        self._deepsort = None

        if force_simple:
            return

        try:
            from deep_sort_realtime.deepsort_tracker import DeepSort  # type: ignore

            self._deepsort = DeepSort(
                max_age=max(1, int(round(max_age_sec * 25))),
                n_init=2,
                max_iou_distance=1.0 - min(max(iou_threshold, 0.05), 0.95),
            )
            self._backend = "deepsort"
        except Exception:
            self._deepsort = None
            self._backend = "simple"

    @property
    def backend(self) -> str:
        return self._backend

    def update(
        self,
        detections: list[dict[str, Any]],
        frame: Any = None,
        timestamp: float | None = None,
    ) -> list[TrackedObject]:
        if self._deepsort is None:
            return self._simple.update(detections, timestamp=timestamp)

        ds_inputs: list[tuple[list[float], float, str]] = []
        for detection in detections:
            x_min, y_min, x_max, y_max = _bbox_from_detection(detection)
            ds_inputs.append(
                (
                    [x_min, y_min, max(0.0, x_max - x_min), max(0.0, y_max - y_min)],
                    _as_float(detection.get("confidence"), 0.0),
                    str(detection.get("label") or "person"),
                )
            )

        try:
            tracks = self._deepsort.update_tracks(ds_inputs, frame=frame)
        except Exception:
            return self._simple.update(detections, timestamp=timestamp)

        tracked_objects: list[TrackedObject] = []
        detection_bboxes = [_bbox_from_detection(item) for item in detections]
        for track in tracks:
            if not track.is_confirmed():
                continue
            ltrb = track.to_ltrb()
            bbox = (
                _as_float(ltrb[0]),
                _as_float(ltrb[1]),
                _as_float(ltrb[2]),
                _as_float(ltrb[3]),
            )
            matched_detection = _closest_detection(detections, detection_bboxes, bbox)
            tracked_objects.append(
                TrackedObject(
                    track_id=f"track_{track.track_id}",
                    x_min=bbox[0],
                    y_min=bbox[1],
                    x_max=bbox[2],
                    y_max=bbox[3],
                    confidence=_as_float(
                        (matched_detection or {}).get("confidence"), 0.0
                    ),
                    label=str((matched_detection or {}).get("label") or "person"),
                    extra_json=(matched_detection or {}).get("extra_json") or {},
                )
            )
        return tracked_objects


def _closest_detection(
    detections: list[dict[str, Any]],
    bboxes: list[tuple[float, float, float, float]],
    target_bbox: tuple[float, float, float, float],
) -> dict[str, Any] | None:
    if not detections:
        return None

    best_index = 0
    best_distance = float("inf")
    for index, bbox in enumerate(bboxes):
        distance = _distance(bbox, target_bbox)
        if distance < best_distance:
            best_distance = distance
            best_index = index
    return detections[best_index]
