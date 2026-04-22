from __future__ import annotations

from flask_smorest import Blueprint
from marshmallow import Schema, fields

from app.api.schemas import ResponseEnvelopeSchema
from app.core.response import success_payload
from app.services.engine_task_service import (
    get_task,
    start_task,
    stop_task,
    switch_task_model,
    update_task_config,
)

blp = Blueprint("engine_tasks", __name__, description="Minimal inference engine tasks")


class EngineTaskStartSchema(Schema):
    task_code = fields.String(required=True)
    camera_code = fields.String(required=False, load_default="")
    stream_url = fields.String(required=False, load_default="")
    display_stream_url = fields.String(required=False, load_default="")
    frame_interval = fields.Float(required=False, allow_none=True, load_default=None)
    model_version = fields.String(required=False, allow_none=True, load_default=None)
    drowning_alert_threshold_sec = fields.Float(
        required=False,
        allow_none=True,
        load_default=None,
    )


class EngineTaskStopSchema(Schema):
    task_code = fields.String(required=True)


class EngineTaskModelSwitchSchema(Schema):
    task_code = fields.String(required=True)
    model_version = fields.String(required=True)


class EngineTaskConfigUpdateSchema(Schema):
    task_code = fields.String(required=True)
    drowning_alert_threshold_sec = fields.Float(
        required=False,
        allow_none=True,
        load_default=None,
    )


@blp.route("/engine/tasks/start", methods=["POST"])
@blp.arguments(EngineTaskStartSchema)
@blp.response(200, ResponseEnvelopeSchema)
def start_engine_task_endpoint(payload):
    data = start_task(
        task_code=payload["task_code"],
        camera_code=payload.get("camera_code", ""),
        stream_url=payload.get("stream_url", ""),
        display_stream_url=payload.get("display_stream_url", ""),
        frame_interval=payload.get("frame_interval"),
        model_version=payload.get("model_version"),
        drowning_alert_threshold_sec=payload.get("drowning_alert_threshold_sec"),
    )
    return success_payload(data)


@blp.route("/engine/tasks/stop", methods=["POST"])
@blp.arguments(EngineTaskStopSchema)
@blp.response(200, ResponseEnvelopeSchema)
def stop_engine_task_endpoint(payload):
    data = stop_task(task_code=payload["task_code"])
    return success_payload(data)


@blp.route("/engine/tasks/<string:task_code>", methods=["GET"])
@blp.response(200, ResponseEnvelopeSchema)
def get_engine_task_endpoint(task_code: str):
    return success_payload(get_task(task_code))


@blp.route("/engine/tasks/model/switch", methods=["POST"])
@blp.arguments(EngineTaskModelSwitchSchema)
@blp.response(200, ResponseEnvelopeSchema)
def switch_engine_task_model_endpoint(payload):
    data = switch_task_model(
        task_code=payload["task_code"],
        model_version=payload["model_version"],
    )
    return success_payload(data)


@blp.route("/engine/tasks/config/update", methods=["POST"])
@blp.arguments(EngineTaskConfigUpdateSchema)
@blp.response(200, ResponseEnvelopeSchema)
def update_engine_task_config_endpoint(payload):
    data = update_task_config(
        task_code=payload["task_code"],
        drowning_alert_threshold_sec=payload.get("drowning_alert_threshold_sec"),
    )
    return success_payload(data)
