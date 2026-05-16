from __future__ import annotations

from flask import Blueprint, Response, jsonify, request

from app.common.errors import BusinessError
from app.common.response import success_payload
from app.video_hub import video_hub_registry

blp = Blueprint("video_hub", __name__)


def _resolve_source_url() -> str:
    payload = request.get_json(silent=True) or {}
    source_url = str(payload.get("source_url") or request.args.get("source_url") or "").strip()
    if not source_url:
        raise BusinessError(
            "首次建立视频会话必须提供 source_url",
            status_code=400,
            code="VIDEO_HUB_SOURCE_REQUIRED",
        )
    return source_url


def _resolve_rotation() -> int:
    payload = request.get_json(silent=True) or {}
    raw = payload.get("rotation", request.args.get("rotation", 0))
    try:
        rotation = int(raw)
    except (TypeError, ValueError):
        rotation = 0
    if rotation not in (0, 90, 180, 270):
        rotation = 0
    return rotation


def _get_or_ensure_session(camera_id: int):
    requested_source_url = str(request.args.get("source_url") or "").strip()
    rotation = _resolve_rotation()
    if requested_source_url:
        return video_hub_registry.ensure_session(camera_id, requested_source_url, rotation=rotation)

    session = video_hub_registry.get_session(camera_id)
    if session is not None:
        return session
    source_url = _resolve_source_url()
    return video_hub_registry.ensure_session(camera_id, source_url, rotation=rotation)


@blp.post("/video-hub/cameras/<int:camera_id>/ensure")
def ensure_camera_session(camera_id: int):
    source_url = _resolve_source_url()
    rotation = _resolve_rotation()
    session = video_hub_registry.ensure_session(camera_id, source_url, rotation=rotation)
    if session.state == "CIRCUIT_OPEN":
        session.activate_from_circuit_open()
    return jsonify(success_payload(session.status_dict(), message="视频会话已就绪"))


@blp.get("/video-hub/cameras/<int:camera_id>/status")
def camera_session_status(camera_id: int):
    session = video_hub_registry.get_session(camera_id)
    if session is None:
        raise BusinessError(
            "视频会话尚未建立",
            status_code=404,
            code="VIDEO_HUB_SESSION_NOT_FOUND",
        )
    return jsonify(success_payload(session.status_dict()))


@blp.post("/video-hub/cameras/<int:camera_id>/reconnect")
def reconnect_camera(camera_id: int):
    session = video_hub_registry.get_session(camera_id)
    if session is None:
        raise BusinessError(
            "视频会话尚未建立",
            status_code=404,
            code="VIDEO_HUB_SESSION_NOT_FOUND",
        )
    session.activate_from_circuit_open()
    return jsonify(success_payload({"camera_id": camera_id, "state": session.state}))


@blp.delete("/video-hub/cameras/<int:camera_id>/session")
def delete_camera_session(camera_id: int):
    session = video_hub_registry.get_session(camera_id)
    if session is None:
        raise BusinessError(
            "视频会话尚未建立",
            status_code=404,
            code="VIDEO_HUB_SESSION_NOT_FOUND",
        )
    video_hub_registry.remove_session(camera_id)
    return jsonify(success_payload({"camera_id": camera_id}))


@blp.get("/video-hub/cameras/<int:camera_id>/snapshot")
def camera_snapshot(camera_id: int):
    session = video_hub_registry.get_session(camera_id)
    if session is None:
        raise BusinessError(
            "暂无可用视频帧，请先建立视频会话",
            status_code=503,
            code="VIDEO_HUB_FRAME_UNAVAILABLE",
        )
    frame = session.get_latest_frame()
    if frame is None:
        raise BusinessError(
            "暂无可用视频帧，请稍后重试",
            status_code=503,
            code="VIDEO_HUB_FRAME_UNAVAILABLE",
        )
    return Response(frame["jpeg_bytes"], mimetype="image/jpeg")


@blp.get("/video-hub/cameras/<int:camera_id>/stream")
def camera_stream(camera_id: int):
    session = _get_or_ensure_session(camera_id)
    return Response(
        session.open_stream(),
        mimetype="multipart/x-mixed-replace; boundary=frame",
    )
