from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
import hashlib
import re
import threading
import time
from typing import Any
import uuid

from flask import current_app

from app.core.errors import BusinessError
from app.services.ai_ws_push_service import ai_ws_push_service
from app.services.callback_client_service import post_task_callback
from app.services.drowning_rule_service import DrowningDecision, DrowningRuleEvaluator
from app.metrics.inference_metrics import record_inference
from app.metrics.task_metrics import (
    record_alert_published,
    record_task_started,
    record_task_stopped,
)
from app.services.model_inference_service import infer_stream_frame, warmup_model
from app.services.rabbitmq_publisher_service import rabbitmq_publisher_service
from app.services.tracker_service import DeepSortTracker, TrackedObject
from app.services.video_overlay_service import video_frame_push_service
from app.services.video_hub_client import video_hub_client


@dataclass
class EngineTaskState:
    task_code: str
    camera_code: str
    stream_url: str
    display_stream_url: str
    frame_interval: float
    model_version: str
    status: str = "RUNNING"
    error_message: str = ""
    frames_processed: int = 0
    callback_interval_sec: float = 3.0
    drowning_alert_threshold_sec: float = 3.0
    last_callback_at: float = 0.0
    latest_frame_ts: float = 0.0
    latest_frame_width: int = 0
    latest_frame_height: int = 0
    latest_detections: list[dict[str, Any]] = field(default_factory=list)
    latest_risk_point: dict[str, Any] = field(default_factory=dict)
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)
    stop_event: threading.Event = field(default_factory=threading.Event, repr=False)
    thread: threading.Thread | None = field(default=None, repr=False)


_TASKS: dict[str, EngineTaskState] = {}
_TASKS_LOCK = threading.Lock()


def _utc_now() -> datetime:
    return datetime.utcnow()


def _serialize_task(task: EngineTaskState) -> dict[str, Any]:
    return {
        "task_code": task.task_code,
        "camera_code": task.camera_code,
        "status": task.status,
        "stream_url": task.stream_url,
        "display_stream_url": task.display_stream_url,
        "frame_interval": task.frame_interval,
        "model_version": task.model_version,
        "frames_processed": task.frames_processed,
        "drowning_alert_threshold_sec": task.drowning_alert_threshold_sec,
        "error_message": task.error_message,
        "realtime": {
            "frame_ts": task.latest_frame_ts,
            "frame_width": task.latest_frame_width,
            "frame_height": task.latest_frame_height,
            "detections": task.latest_detections,
            "risk_point": task.latest_risk_point,
        },
        "created_at": task.created_at.isoformat() + "Z",
        "updated_at": task.updated_at.isoformat() + "Z",
    }


def _set_task_status(task_code: str, status: str, error_message: str = ""):
    with _TASKS_LOCK:
        task = _TASKS.get(task_code)
        if task is None:
            return
        task.status = status
        task.error_message = error_message
        task.updated_at = _utc_now()


def _touch_task_progress(task_code: str) -> int:
    with _TASKS_LOCK:
        task = _TASKS.get(task_code)
        if task is None:
            return 0
        task.frames_processed += 1
        task.updated_at = _utc_now()
        return task.frames_processed


def _extract_camera_id_from_task_code(task_code: str) -> int | None:
    match = re.search(r"TASK_CAM_(\d+)_", task_code)
    if not match:
        return None
    try:
        return int(match.group(1))
    except Exception:
        return None


def _is_drowning_label(label: str) -> bool:
    normalized = str(label or "").strip().lower()
    return (
        normalized == "drowning"
        or normalized == "drown"
        or "drown" in normalized
        or "溺" in normalized
    )


def _build_rule_hits(decision: DrowningDecision) -> list[str]:
    hits: list[str] = []
    if decision.posture_abnormal:
        hits.append("posture_abnormal")
    if decision.thermal_abnormal:
        hits.append("thermal_abnormal")
    if decision.duration_abnormal:
        hits.append("duration_abnormal")
    return hits


def _calc_risk_score(decision: DrowningDecision) -> float:
    score = (
        decision.posture_score * 0.35
        + decision.thermal_score * 0.35
        + min(max(decision.duration_sec / 5.0, 0.0), 1.0) * 0.3
    )
    return min(max(score, 0.0), 1.0)


def _resolve_risk_level(decision: DrowningDecision, risk_score: float) -> str:
    if decision.triggered or risk_score >= 0.85:
        return "HIGH"
    if risk_score >= 0.6:
        return "MEDIUM"
    return "LOW"


def _build_event_uid(task_code: str, track_id: str, frame_count: int) -> str:
    now_ms = int(time.time() * 1000)
    raw_text = f"{task_code}|{track_id}|{frame_count}|{now_ms}"
    digest = hashlib.sha1(raw_text.encode("utf-8")).hexdigest()[:20]
    return f"evt_{now_ms}_{digest}"


def _build_position_desc(
    tracked_object: TrackedObject,
    risk_point: dict[str, Any],
    rule_hits: list[str],
) -> str:
    """构建位置描述信息。"""
    parts: list[str] = []

    # 添加边界框位置信息
    center = risk_point.get("bboxCenter", {})
    if center:
        x = center.get("x", 0)
        y = center.get("y", 0)
        parts.append(f"目标位置: ({x:.1f}, {y:.1f})")

    # 添加置信度信息
    if tracked_object.confidence > 0:
        parts.append(f"置信度: {tracked_object.confidence:.1%}")

    # 添加触发的规则
    if rule_hits:
        parts.append(f"触发: {', '.join(rule_hits)}")

    return "; ".join(parts) if parts else "检测到疑似溺水行为"


def _build_event_payload(
    task: EngineTaskState,
    tracked_object: TrackedObject,
    decision: DrowningDecision,
    head_count: int,
    frame_count: int,
    tracker_backend: str,
    frame_width: int,
    frame_height: int,
) -> dict[str, Any]:
    detect_time = datetime.utcnow().isoformat() + "Z"
    event_uid = _build_event_uid(task.task_code, tracked_object.track_id, frame_count)
    center_x = (tracked_object.x_min + tracked_object.x_max) / 2.0
    center_y = (tracked_object.y_min + tracked_object.y_max) / 2.0
    safe_width = max(1.0, float(frame_width))
    safe_height = max(1.0, float(frame_height))
    risk_point = {
        "cameraId": _extract_camera_id_from_task_code(task.task_code),
        "cameraCode": task.camera_code,
        "bboxCenter": {
            "x": center_x,
            "y": center_y,
        },
        "bboxCenterNorm": {
            "x": center_x / safe_width,
            "y": center_y / safe_height,
        },
    }
    rule_hits = _build_rule_hits(decision)
    risk_score = _calc_risk_score(decision)
    risk_level = _resolve_risk_level(decision, risk_score)
    camera_id = _extract_camera_id_from_task_code(task.task_code)
    position_desc = _build_position_desc(tracked_object, risk_point, rule_hits)
    incident_location = (
        f"摄像头{camera_id} - 深水区检测区域" if camera_id else "检测区域"
    )

    return {
        "eventUid": event_uid,
        "cameraId": camera_id,
        "cameraCode": task.camera_code,
        "taskCode": task.task_code,
        "eventType": "DROWNING",
        "riskType": "DROWNING",
        "riskLevel": risk_level,
        "detectTime": detect_time,
        "confidence": tracked_object.confidence,
        "targetId": tracked_object.track_id,
        "poolHeadCount": head_count,
        "modelVersion": task.model_version,
        "videoStreamUrl": task.display_stream_url or task.stream_url,
        "bbox": {
            "xMin": tracked_object.x_min,
            "yMin": tracked_object.y_min,
            "xMax": tracked_object.x_max,
            "yMax": tracked_object.y_max,
        },
        "riskPoint": risk_point,
        "positionDesc": position_desc,
        "incidentLocation": incident_location,
        "emergencyContactName": "",
        "emergencyContactPhone": "",
        "extJson": {
            "trackerBackend": tracker_backend,
            "triggered": decision.triggered,
            "postureScore": decision.posture_score,
            "thermalScore": decision.thermal_score,
            "durationSec": decision.duration_sec,
            "postureAbnormal": decision.posture_abnormal,
            "thermalAbnormal": decision.thermal_abnormal,
            "durationAbnormal": decision.duration_abnormal,
            "riskScore": risk_score,
            "ruleHits": rule_hits,
            "riskLevel": risk_level,
            "riskPoint": risk_point,
        },
    }


def _convert_to_realtime_detection(
    tracked_object: TrackedObject,
    frame_width: int,
    frame_height: int,
    decision: DrowningDecision | None = None,
) -> dict[str, Any]:
    safe_width = max(1.0, float(frame_width))
    safe_height = max(1.0, float(frame_height))
    merged_extra_json = dict(tracked_object.extra_json or {})
    if decision is not None:
        merged_extra_json.update(
            {
                "triggered": decision.triggered,
                "posture_score": decision.posture_score,
                "thermal_score": decision.thermal_score,
                "duration_sec": decision.duration_sec,
                "posture_abnormal": decision.posture_abnormal,
                "thermal_abnormal": decision.thermal_abnormal,
                "duration_abnormal": decision.duration_abnormal,
                "risk_score": _calc_risk_score(decision),
                "risk_level": _resolve_risk_level(decision, _calc_risk_score(decision)),
                "rule_hits": _build_rule_hits(decision),
            }
        )
    return {
        "track_id": tracked_object.track_id,
        "label": tracked_object.label,
        "confidence": tracked_object.confidence,
        "bbox": {
            "x_min": tracked_object.x_min,
            "y_min": tracked_object.y_min,
            "x_max": tracked_object.x_max,
            "y_max": tracked_object.y_max,
        },
        "bbox_norm": {
            "x_min": tracked_object.x_min / safe_width,
            "y_min": tracked_object.y_min / safe_height,
            "x_max": tracked_object.x_max / safe_width,
            "y_max": tracked_object.y_max / safe_height,
        },
        "extra_json": merged_extra_json,
    }


def _sync_task_realtime(
    task_code: str,
    tracked_objects: list[TrackedObject],
    frame_width: int,
    frame_height: int,
    decisions: list[DrowningDecision] | None = None,
    frame_timestamp: float | None = None,
):
    decision_map: dict[str, DrowningDecision] = {}
    if decisions:
        decision_map = {item.track_id: item for item in decisions}
    detections = [
        _convert_to_realtime_detection(
            item,
            frame_width=frame_width,
            frame_height=frame_height,
            decision=decision_map.get(item.track_id),
        )
        for item in tracked_objects
    ]
    risk_point: dict[str, Any] = {}
    drowning_candidates = [
        item for item in tracked_objects if _is_drowning_label(item.label)
    ]
    if drowning_candidates:
        picked = max(drowning_candidates, key=lambda item: item.confidence)
        center_x = (picked.x_min + picked.x_max) / 2.0
        center_y = (picked.y_min + picked.y_max) / 2.0
        safe_width = max(1.0, float(frame_width))
        safe_height = max(1.0, float(frame_height))
        risk_point = {
            "cameraId": _extract_camera_id_from_task_code(task_code),
            "trackId": picked.track_id,
            "bboxCenter": {
                "x": center_x,
                "y": center_y,
            },
            "bboxCenterNorm": {
                "x": center_x / safe_width,
                "y": center_y / safe_height,
            },
        }
        matched_decision = decision_map.get(picked.track_id)
        if matched_decision is not None:
            risk_score = _calc_risk_score(matched_decision)
            risk_point.update(
                {
                    "triggered": matched_decision.triggered,
                    "durationSec": matched_decision.duration_sec,
                    "riskScore": risk_score,
                    "riskLevel": _resolve_risk_level(matched_decision, risk_score),
                    "ruleHits": _build_rule_hits(matched_decision),
                }
            )

    with _TASKS_LOCK:
        task = _TASKS.get(task_code)
        if task is None:
            return
        task.latest_frame_ts = time.time() if frame_timestamp is None else frame_timestamp
        task.latest_frame_width = frame_width
        task.latest_frame_height = frame_height
        task.latest_detections = detections
        task.latest_risk_point = risk_point
        task.updated_at = _utc_now()


def _pick_triggered_candidate(
    tracked_objects: list[TrackedObject],
    decisions: list[DrowningDecision],
) -> tuple[TrackedObject, DrowningDecision] | None:
    decision_map = {item.track_id: item for item in decisions if item.triggered}
    candidates: list[tuple[TrackedObject, DrowningDecision]] = []
    for tracked_object in tracked_objects:
        matched_decision = decision_map.get(tracked_object.track_id)
        if matched_decision is None:
            continue
        candidates.append((tracked_object, matched_decision))
    if not candidates:
        return None

    return max(
        candidates,
        key=lambda item: (
            item[0].confidence + item[1].posture_score + item[1].thermal_score
        ),
    )


def _post_detection_event_if_needed(
    task_code: str,
    tracked_objects: list[TrackedObject],
    decisions: list[DrowningDecision],
    tracker_backend: str,
    frame_count: int,
    head_count: int = 0,
    frame_width: int = 0,
    frame_height: int = 0,
):
    picked = _pick_triggered_candidate(tracked_objects, decisions)
    if picked is None:
        return

    tracked_object, decision = picked
    event_payload = None
    with _TASKS_LOCK:
        task = _TASKS.get(task_code)
        if task is None:
            return
        now = time.monotonic()
        if now - task.last_callback_at < task.callback_interval_sec:
            return
        task.last_callback_at = now
        event_payload = _build_event_payload(
            task=task,
            tracked_object=tracked_object,
            decision=decision,
            head_count=head_count,
            frame_count=frame_count,
            tracker_backend=tracker_backend,
            frame_width=frame_width,
            frame_height=frame_height,
        )
    if event_payload is not None:
        post_task_callback(event_payload)
        if rabbitmq_publisher_service.is_connected():
            msg_payload = {
                "messageId": str(uuid.uuid4()),
                "version": 1,
                "source": "yolo-service",
                "publishedAt": time.time(),
            }
            msg_payload.update(event_payload)
            rabbitmq_publisher_service.publish_alert(
                msg_payload, routing_key="alert.record"
            )
            record_alert_published(channel="rabbitmq", status="success")


def _push_realtime_ws(
    task_code: str,
    tracked_objects: list[TrackedObject],
    decisions: list[DrowningDecision],
    frame_width: int,
    frame_height: int,
    frame_timestamp: float | None = None,
):
    camera_id = _extract_camera_id_from_task_code(task_code)
    if camera_id is None:
        return
    if not ai_ws_push_service.is_connected():
        return
    decision_map: dict[str, DrowningDecision] = {
        d.track_id: d for d in (decisions or [])
    }
    detections = [
        _convert_to_realtime_detection(
            obj,
            frame_width=frame_width,
            frame_height=frame_height,
            decision=decision_map.get(obj.track_id),
        )
        for obj in tracked_objects
    ]
    risk_point: dict[str, Any] = {}
    drowning_candidates = [
        obj for obj in tracked_objects if _is_drowning_label(obj.label)
    ]
    if drowning_candidates:
        picked = max(drowning_candidates, key=lambda o: o.confidence)
        center_x = (picked.x_min + picked.x_max) / 2.0
        center_y = (picked.y_min + picked.y_max) / 2.0
        risk_point = {
            "cameraId": camera_id,
            "trackId": picked.track_id,
            "bboxCenter": {"x": center_x, "y": center_y},
        }
    payload = {
        "cameraId": camera_id,
        "taskCode": task_code,
        "frameWidth": frame_width,
        "frameHeight": frame_height,
        "frameTs": time.time() if frame_timestamp is None else frame_timestamp,
        "headCount": len(tracked_objects),
        "detections": detections,
        "riskPoint": risk_point,
    }
    ai_ws_push_service.push(payload)


def _run_loop_without_stream(task_code: str, frame_interval: float):
    while True:
        with _TASKS_LOCK:
            task = _TASKS.get(task_code)
            if task is None:
                return
            stop_event = task.stop_event
        should_stop = stop_event.wait(timeout=frame_interval)
        if should_stop:
            return
        _touch_task_progress(task_code)


def _should_use_video_hub_for_stream(task_code: str, stream_url: str) -> bool:
    if _extract_camera_id_from_task_code(task_code) is None:
        return False
    normalized = (stream_url or "").strip().lower()
    return bool(normalized) and (
        normalized.startswith("rtsp://")
        or normalized.startswith("http://")
        or normalized.startswith("https://")
    )


def _build_drowning_evaluator(min_duration_sec: float) -> DrowningRuleEvaluator:
    return DrowningRuleEvaluator(
        min_duration_sec=min_duration_sec,
        posture_threshold=float(
            current_app.config.get("ENGINE_DROWNING_POSTURE_THRESHOLD", 0.7)
        ),
        thermal_threshold=float(
            current_app.config.get("ENGINE_DROWNING_THERMAL_THRESHOLD", 0.85)
        ),
        cooldown_sec=float(
            current_app.config.get("ENGINE_DROWNING_EVENT_COOLDOWN_SEC", 15.0)
        ),
    )


def _build_tracker() -> DeepSortTracker:
    return DeepSortTracker(
        iou_threshold=float(current_app.config.get("ENGINE_TRACK_IOU_THRESHOLD", 0.3)),
        max_age_sec=float(current_app.config.get("ENGINE_TRACK_MAX_AGE_SEC", 1.5)),
    )


def _run_loop_with_video_hub(task_code: str, stream_url: str, frame_interval: float):
    try:
        import cv2
        import numpy as np
    except Exception:
        current_app.logger.warning("cv2/numpy unavailable, fallback to sleep loop")
        _run_loop_without_stream(task_code, frame_interval)
        return

    camera_id = _extract_camera_id_from_task_code(task_code)
    if camera_id is None:
        current_app.logger.warning(
            "无法从 task_code 提取 camera_id，回退空循环: %s", task_code
        )
        _run_loop_without_stream(task_code, frame_interval)
        return

    tracker = _build_tracker()
    with _TASKS_LOCK:
        task_for_threshold = _TASKS.get(task_code)
        current_drowning_threshold_sec = (
            3.0
            if task_for_threshold is None
            else float(task_for_threshold.drowning_alert_threshold_sec)
        )
    evaluator = _build_drowning_evaluator(current_drowning_threshold_sec)
    video_hub_client.ensure_session(camera_id, stream_url)
    last_infer_at = 0.0

    while True:
        with _TASKS_LOCK:
            task = _TASKS.get(task_code)
            if task is None:
                return
            if task.stop_event.is_set():
                return
            model_version = task.model_version
            task_drowning_threshold_sec = float(task.drowning_alert_threshold_sec)

        if abs(task_drowning_threshold_sec - current_drowning_threshold_sec) > 1e-6:
            current_drowning_threshold_sec = task_drowning_threshold_sec
            evaluator = _build_drowning_evaluator(current_drowning_threshold_sec)

        jpeg_bytes = video_hub_client.fetch_snapshot(camera_id)
        if jpeg_bytes is None:
            time.sleep(0.01)
            continue

        now = time.monotonic()
        if now - last_infer_at < frame_interval:
            time.sleep(0.005)
            continue

        frame_buffer = np.frombuffer(jpeg_bytes, dtype=np.uint8)
        frame = cv2.imdecode(frame_buffer, cv2.IMREAD_COLOR)
        if frame is None:
            continue
        frame_height, frame_width = frame.shape[:2]
        frame_timestamp = time.time()

        infer_started_at = time.monotonic()
        detections = infer_stream_frame(frame, model_version=model_version)
        infer_latency = time.monotonic() - infer_started_at
        record_inference(
            model_version=model_version,
            latency_sec=infer_latency,
            status="success",
        )
        tracked_objects = tracker.update(detections, frame=frame, timestamp=now)
        drowning_objects = [obj for obj in tracked_objects if _is_drowning_label(obj.label)]
        decisions = [
            evaluator.evaluate(tracked_object, timestamp=now)
            for tracked_object in drowning_objects
        ]
        _sync_task_realtime(
            task_code=task_code,
            tracked_objects=tracked_objects,
            frame_width=frame_width,
            frame_height=frame_height,
            decisions=decisions,
            frame_timestamp=frame_timestamp,
        )
        _push_realtime_ws(
            task_code,
            tracked_objects,
            decisions,
            frame_width,
            frame_height,
            frame_timestamp=frame_timestamp,
        )
        if camera_id is not None:
            video_frame_push_service.push_frame(
                ai_ws_push_service,
                camera_id,
                frame,
                list(_TASKS.get(task_code).latest_detections) if _TASKS.get(task_code) else [],
            )

        frame_count = _touch_task_progress(task_code)
        _post_detection_event_if_needed(
            task_code=task_code,
            tracked_objects=drowning_objects,
            decisions=decisions,
            tracker_backend=tracker.backend,
            frame_count=frame_count,
            head_count=len(tracked_objects),
            frame_width=frame_width,
            frame_height=frame_height,
        )
        last_infer_at = now


def _run_loop_with_stream(task_code: str, stream_url: str, frame_interval: float):
    if _should_use_video_hub_for_stream(task_code, stream_url):
        _run_loop_with_video_hub(task_code, stream_url, frame_interval)
        return

    current_app.logger.warning(
        "stream_url 非标准协议，回退空循环: %s", stream_url
    )
    _run_loop_without_stream(task_code, frame_interval)


def _engine_task_worker(app, task_code: str):
    with app.app_context():
        try:
            warmup_model()
        except Exception as exc:
            current_app.logger.exception("Engine task warmup failed: %s", exc)
            _set_task_status(task_code, "FAILED", str(exc))
            return

        while True:
            with _TASKS_LOCK:
                task = _TASKS.get(task_code)
                if task is None:
                    return
                if task.stop_event.is_set():
                    _set_task_status(task_code, "STOPPED")
                    return
                stream_url = task.stream_url
                frame_interval = task.frame_interval

            try:
                if stream_url:
                    _run_loop_with_stream(task_code, stream_url, frame_interval)
                else:
                    _run_loop_without_stream(task_code, frame_interval)
                _set_task_status(task_code, "STOPPED")
                return
            except Exception as exc:
                current_app.logger.warning(
                    "Engine task %s loop failed, retrying in 5s: %s",
                    task_code,
                    exc,
                )
                _set_task_status(task_code, "FAILED", str(exc))
                with _TASKS_LOCK:
                    task = _TASKS.get(task_code)
                    if task is None:
                        return
                    stop_event = task.stop_event
                if stop_event.wait(timeout=5.0):
                    _set_task_status(task_code, "STOPPED")
                    return
                _set_task_status(task_code, "RUNNING", "")


def start_task(
    task_code: str,
    stream_url: str = "",
    display_stream_url: str = "",
    frame_interval: float | None = None,
    camera_code: str = "",
    model_version: str | None = None,
    drowning_alert_threshold_sec: float | None = None,
):
    task_code_text = task_code.strip()
    if not task_code_text:
        raise BusinessError("task_code is required", status_code=400)

    app = current_app._get_current_object()  # type: ignore[attr-defined]
    interval = float(
        frame_interval
        if frame_interval is not None
        else current_app.config.get("ENGINE_DEFAULT_FRAME_INTERVAL", 0.2)
    )
    interval = max(0.01, interval)
    model_version_text = str(
        model_version or current_app.config.get("MODEL_VERSION", "v1")
    )
    callback_interval = float(
        current_app.config.get("ENGINE_CALLBACK_INTERVAL_SEC", 3.0)
    )
    drowning_threshold_sec = float(
        drowning_alert_threshold_sec
        if drowning_alert_threshold_sec is not None
        else current_app.config.get("ENGINE_DROWNING_MIN_DURATION_SEC", 3.0)
    )
    drowning_threshold_sec = max(1.0, drowning_threshold_sec)

    with _TASKS_LOCK:
        existing = _TASKS.get(task_code_text)
        if existing is not None and existing.status == "RUNNING":
            raise BusinessError("task already running", status_code=409)

        task = EngineTaskState(
            task_code=task_code_text,
            camera_code=(camera_code or "").strip(),
            stream_url=(stream_url or "").strip(),
            display_stream_url=(display_stream_url or "").strip(),
            frame_interval=interval,
            model_version=model_version_text,
            callback_interval_sec=max(0.5, callback_interval),
            drowning_alert_threshold_sec=drowning_threshold_sec,
        )
        _TASKS[task_code_text] = task
        thread = threading.Thread(
            target=_engine_task_worker,
            args=(app, task_code_text),
            daemon=True,
            name=f"engine-task-{task_code_text}",
        )
        task.thread = thread

    thread.start()
    record_task_started()
    return _serialize_task(task)


def stop_task(task_code: str):
    task_code_text = task_code.strip()
    if not task_code_text:
        raise BusinessError("task_code is required", status_code=400)

    with _TASKS_LOCK:
        task = _TASKS.get(task_code_text)
        if task is None:
            now_iso = _utc_now().isoformat() + "Z"
            return {
                "task_code": task_code_text,
                "status": "STOPPED",
                "stream_url": "",
                "display_stream_url": "",
                "frame_interval": 0.0,
                "model_version": "",
                "frames_processed": 0,
                "error_message": "",
                "realtime": {
                    "frame_ts": 0.0,
                    "detections": [],
                    "risk_point": {},
                },
                "created_at": now_iso,
                "updated_at": now_iso,
            }
        task.stop_event.set()
        task.updated_at = _utc_now()
        record_task_stopped()
        payload = _serialize_task(task)
    return payload


def get_task(task_code: str):
    task_code_text = task_code.strip()
    with _TASKS_LOCK:
        task = _TASKS.get(task_code_text)
        if task is None:
            raise BusinessError("task not found", status_code=404)
        return _serialize_task(task)


def switch_task_model(task_code: str, model_version: str):
    task_code_text = task_code.strip()
    model_version_text = model_version.strip()
    if not task_code_text:
        raise BusinessError("task_code is required", status_code=400)
    if not model_version_text:
        raise BusinessError("model_version is required", status_code=400)

    warmup_model(model_version_text)
    with _TASKS_LOCK:
        task = _TASKS.get(task_code_text)
        if task is None:
            raise BusinessError("task not found", status_code=404)
        if task.status not in {"RUNNING", "STARTING"}:
            raise BusinessError("task is not running", status_code=409)
        task.model_version = model_version_text
        task.updated_at = _utc_now()
        return _serialize_task(task)


def update_task_config(
    task_code: str,
    drowning_alert_threshold_sec: float | None = None,
):
    task_code_text = task_code.strip()
    if not task_code_text:
        raise BusinessError("task_code is required", status_code=400)

    with _TASKS_LOCK:
        task = _TASKS.get(task_code_text)
        if task is None:
            raise BusinessError("task not found", status_code=404)
        if drowning_alert_threshold_sec is not None:
            task.drowning_alert_threshold_sec = max(
                1.0, float(drowning_alert_threshold_sec)
            )
        task.updated_at = _utc_now()
        return _serialize_task(task)
