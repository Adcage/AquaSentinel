from __future__ import annotations

from flask import Blueprint, Response, jsonify, request

from app.core.errors import BusinessError
from app.core.response import success_payload
from app.video_hub import webrtc_session_manager
from app.video_hub.webrtc_signaling import run_async

blp = Blueprint("video_hub_webrtc", __name__)


@blp.post("/video-hub/cameras/<int:camera_id>/whip")
def whip_offer(camera_id: int):
    sdp_offer = request.get_data(as_text=True)
    if not sdp_offer.strip():
        raise BusinessError(
            "SDP offer 不能为空",
            status_code=400,
            code="WEBRTC_SDP_EMPTY",
        )
    try:
        sdp_answer, session_id = run_async(
            webrtc_session_manager.create_whip_session(camera_id, sdp_offer)
        )
    except ValueError as exc:
        raise BusinessError(
            str(exc),
            status_code=503,
            code="WEBRTC_SESSION_ERROR",
        )
    except Exception as exc:
        raise BusinessError(
            f"WebRTC 信令处理失败: {exc}",
            status_code=400,
            code="WEBRTC_SIGNALING_ERROR",
        )
    response = Response(sdp_answer, status=201, content_type="application/sdp")
    response.headers["Location"] = f"/video-hub/sessions/{session_id}"
    return response


@blp.delete("/video-hub/sessions/<string:session_id>")
def delete_whip_session(session_id: str):
    run_async(webrtc_session_manager.delete_whip_session(session_id))
    return jsonify(success_payload({"session_id": session_id}))
