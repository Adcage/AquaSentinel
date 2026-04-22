from __future__ import annotations

import re

from flask_smorest import Blueprint
from marshmallow import Schema, fields

from app.api.schemas import ResponseEnvelopeSchema
from app.core.response import success_payload
from app.services.callback_client_service import post_task_callback
from app.services.drowning_rule_service import DrowningDecision
from app.services.engine_task_service import (
    _TASKS,
    _TASKS_LOCK,
    _build_event_payload,
)
from app.services.tracker_service import TrackedObject


blp = Blueprint(
    "engine_test", __name__, description="Test endpoints for drowning alert triggering"
)


class TestTriggerAlertSchema(Schema):
    camera_id = fields.Integer(required=False)
    task_code = fields.String(required=False)
    posture_score = fields.Float(required=False, load_default=0.95)
    thermal_score = fields.Float(required=False, load_default=0.95)
    duration_sec = fields.Float(required=False, load_default=3.5)


def _find_task_by_camera_id(camera_id: int):
    with _TASKS_LOCK:
        for _task_code, task in _TASKS.items():
            extracted_id = _extract_camera_id(task.task_code)
            if extracted_id == camera_id:
                return task
    return None


def _find_task_by_code(task_code: str):
    with _TASKS_LOCK:
        task = _TASKS.get(task_code)
        return task
    return None


def _extract_camera_id(task_code: str) -> int | None:
    match = re.search(r"TASK_CAM_(\d+)_", task_code)
    if match:
        try:
            return int(match.group(1))
        except Exception:
            return None
    match = re.search(r"TASK-CAM-(\d+)-", task_code)
    if match:
        try:
            return int(match.group(1))
        except Exception:
            return None
    return None


@blp.route("/engine/test/trigger-alert", methods=["POST"])
@blp.arguments(TestTriggerAlertSchema)
@blp.response(200, ResponseEnvelopeSchema)
def trigger_test_alert(payload):
    camera_id = payload.get("camera_id")
    task_code = payload.get("task_code")
    posture_score = payload.get("posture_score", 0.95)
    thermal_score = payload.get("thermal_score", 0.95)
    duration_sec = payload.get("duration_sec", 3.5)

    if task_code:
        task = _find_task_by_code(task_code)
        if task is None:
            return success_payload(
                {
                    "triggered": False,
                    "message": f"任务 {task_code} 未找到或未运行",
                }
            )
    elif camera_id:
        task = _find_task_by_camera_id(camera_id)
        if task is None:
            return success_payload(
                {
                    "triggered": False,
                    "message": f"camera_id={camera_id} 的任务未找到或未运行",
                }
            )
    else:
        return success_payload(
            {
                "triggered": False,
                "message": "请提供 camera_id 或 task_code",
            }
        )

    resolved_camera_id = camera_id if camera_id else _extract_camera_id(task.task_code)
    if resolved_camera_id is None:
        return success_payload(
            {
                "triggered": False,
                "message": f"无法从任务 {task.task_code} 解析 camera_id",
            }
        )

    track_id = f"test_track_{resolved_camera_id}"
    fake_tracked_obj = TrackedObject(
        track_id=track_id,
        x_min=100.0,
        y_min=100.0,
        x_max=200.0,
        y_max=180.0,
        confidence=0.95,
        label="drowning",
        extra_json={
            "posture_score": posture_score,
            "thermal_score": thermal_score,
        },
    )

    fake_decision = DrowningDecision(
        track_id=track_id,
        triggered=True,
        posture_score=posture_score,
        thermal_score=thermal_score,
        duration_sec=duration_sec,
        posture_abnormal=True,
        thermal_abnormal=True,
        duration_abnormal=True,
    )

    event_payload = _build_event_payload(
        task=task,
        tracked_object=fake_tracked_obj,
        decision=fake_decision,
        head_count=1,
        frame_count=9999,
        tracker_backend="test",
        frame_width=640,
        frame_height=480,
    )

    event_payload["cameraId"] = resolved_camera_id
    risk_point = event_payload.get("riskPoint") or {}
    risk_point["cameraId"] = resolved_camera_id
    event_payload["riskPoint"] = risk_point
    ext_json = event_payload.get("extJson") or {}
    ext_risk_point = ext_json.get("riskPoint") or {}
    ext_risk_point["cameraId"] = resolved_camera_id
    ext_json["riskPoint"] = ext_risk_point
    event_payload["extJson"] = ext_json

    success = post_task_callback(event_payload)

    if success:
        return success_payload(
            {
                "triggered": True,
                "eventUid": event_payload.get("eventUid"),
                "cameraId": resolved_camera_id,
                "taskCode": task.task_code,
                "message": "报警触发成功",
            }
        )
    else:
        return success_payload(
            {
                "triggered": False,
                "message": "回调发送失败，请检查 AI Service 日志",
            }
        )
