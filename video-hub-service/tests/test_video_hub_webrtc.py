from __future__ import annotations

import asyncio
import io

import PIL.Image
import pytest
from aiortc import RTCPeerConnection

from app import create_app
from app.api.video_hub_webrtc import _pin_answer_candidates, _force_setup_passive
from app.video_hub.frame_cache import FrameCache
from app.video_hub.webrtc_session import VideoStreamTrack, WebrtcSessionManager
from app.video_hub.webrtc_signaling import run_async


def test_video_stream_track_kind():
    cache = FrameCache()
    track = VideoStreamTrack(camera_id=1, frame_cache=cache, target_fps=10)
    assert track.kind == "video"


@pytest.mark.asyncio
async def test_video_stream_track_recv_returns_frame():
    cache = FrameCache()
    img = PIL.Image.new("RGB", (1, 1), (255, 255, 255))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    jpeg_bytes = buf.getvalue()
    cache.update(jpeg_bytes, 1, 1)

    track = VideoStreamTrack(camera_id=1, frame_cache=cache, target_fps=10)
    frame = await track.recv()
    assert frame is not None
    assert frame.width == 1
    assert frame.height == 1


@pytest.mark.asyncio
async def test_video_stream_track_recv_resends_last_frame_on_timeout():
    cache = FrameCache()
    img = PIL.Image.new("RGB", (2, 2), (128, 128, 128))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    jpeg_bytes = buf.getvalue()
    cache.update(jpeg_bytes, 2, 2)

    track = VideoStreamTrack(camera_id=1, frame_cache=cache, target_fps=100)
    frame1 = await track.recv()
    assert frame1.width == 2
    assert frame1.height == 2

    frame2 = await track.recv()
    assert frame2 is not None
    assert frame2.width == 2
    assert frame2.height == 2


@pytest.mark.asyncio
async def test_video_stream_track_recv_handles_decode_error():
    cache = FrameCache()
    img = PIL.Image.new("RGB", (1, 1), (0, 0, 0))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    valid_jpeg = buf.getvalue()
    cache.update(valid_jpeg, 1, 1)

    track = VideoStreamTrack(camera_id=1, frame_cache=cache, target_fps=100)
    frame1 = await track.recv()
    assert frame1 is not None

    cache.update(b"not_a_jpeg_at_all", 1, 1)
    frame2 = await track.recv()
    assert frame2 is not None
    assert frame2.width == 1
    assert frame2.height == 1


async def _create_sdp_offer():
    pc = RTCPeerConnection()
    pc.addTransceiver("video", direction="recvonly")
    offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    sdp = pc.localDescription.sdp
    await pc.close()
    return sdp


def _generate_sdp_offer():
    return run_async(_create_sdp_offer())


class StubVideoHubSessionForWebrtc:
    def __init__(self, camera_id: int, source_url: str):
        self.camera_id = camera_id
        self.source_url = source_url
        self.frame_cache = FrameCache()
        self._state = "CONNECTED"

    @property
    def state(self):
        return self._state

    def activate_from_circuit_open(self):
        if self._state == "CIRCUIT_OPEN":
            self._state = "CONNECTING"


class StubRegistryForWebrtc:
    def __init__(self):
        self._sessions: dict[int, StubVideoHubSessionForWebrtc] = {}

    def get_session(self, camera_id: int):
        return self._sessions.get(camera_id)

    def ensure_session(self, camera_id: int, source_url: str, rotation: int = 0):
        if camera_id not in self._sessions:
            self._sessions[camera_id] = StubVideoHubSessionForWebrtc(
                camera_id, source_url
            )
        return self._sessions[camera_id]


def _put_test_frame(session):
    img = PIL.Image.new("RGB", (2, 2), (128, 128, 128))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    session.frame_cache.update(buf.getvalue(), 2, 2)


def _build_app():
    return create_app()


def test_webrtc_session_manager_create_and_delete():
    registry = StubRegistryForWebrtc()
    session = registry.ensure_session(1, "http://192.168.1.88/stream")
    _put_test_frame(session)

    manager = WebrtcSessionManager(registry=registry)
    sdp_offer = _generate_sdp_offer()

    sdp_answer, session_id = run_async(manager.create_whip_session(1, sdp_offer))

    assert sdp_answer is not None
    assert len(sdp_answer) > 0
    assert session_id in manager._sessions
    assert manager._session_to_camera[session_id] == 1

    run_async(manager.delete_whip_session(session_id))
    assert session_id not in manager._sessions
    assert session_id not in manager._session_to_camera


def test_webrtc_session_manager_rejects_when_no_video_session():
    registry = StubRegistryForWebrtc()
    manager = WebrtcSessionManager(registry=registry)
    sdp_offer = _generate_sdp_offer()

    with pytest.raises(ValueError, match="视频会话尚未建立"):
        run_async(manager.create_whip_session(1, sdp_offer))


def test_webrtc_session_manager_rejects_excess_sessions():
    registry = StubRegistryForWebrtc()
    session = registry.ensure_session(1, "http://192.168.1.88/stream")
    _put_test_frame(session)

    manager = WebrtcSessionManager(registry=registry, max_sessions_per_camera=1)
    sdp_offer = _generate_sdp_offer()

    run_async(manager.create_whip_session(1, sdp_offer))

    with pytest.raises(ValueError, match="WebRTC 会话数已达上限"):
        run_async(manager.create_whip_session(1, sdp_offer))

    for sid in list(manager._sessions.keys()):
        run_async(manager.delete_whip_session(sid))


def test_webrtc_session_manager_activates_circuit_open():
    registry = StubRegistryForWebrtc()
    session = registry.ensure_session(1, "http://192.168.1.88/stream")
    _put_test_frame(session)
    session._state = "CIRCUIT_OPEN"

    manager = WebrtcSessionManager(registry=registry)
    sdp_offer = _generate_sdp_offer()

    run_async(manager.create_whip_session(1, sdp_offer))
    assert session.state == "CONNECTING"

    for sid in list(manager._sessions.keys()):
        run_async(manager.delete_whip_session(sid))


def test_whip_endpoint_returns_201_with_sdp_answer(monkeypatch):
    app = _build_app()
    client = app.test_client()

    registry = StubRegistryForWebrtc()
    session = registry.ensure_session(1001, "http://192.168.1.88/stream")
    _put_test_frame(session)

    manager = WebrtcSessionManager(registry=registry)
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.webrtc_session_manager", manager
    )
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.video_hub_registry", registry
    )
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.resolve_camera_source",
        lambda camera_id, token: "http://192.168.1.88/stream",
    )

    sdp_offer = _generate_sdp_offer()
    response = client.post(
        "/video-hub/cameras/1001/whip",
        data=sdp_offer,
        content_type="application/sdp",
        headers={"Authorization": "Bearer abc123"},
    )

    assert response.status_code == 201
    assert response.content_type.startswith("application/sdp")
    assert "Location" in response.headers
    assert "/video-hub/sessions/" in response.headers["Location"]
    sdp_answer = response.get_data(as_text=True)
    assert "v=0" in sdp_answer

    for sid in list(manager._sessions.keys()):
        run_async(manager.delete_whip_session(sid))


def test_whip_endpoint_returns_error_when_no_session(monkeypatch):
    app = _build_app()
    client = app.test_client()

    registry = StubRegistryForWebrtc()
    manager = WebrtcSessionManager(registry=registry)
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.webrtc_session_manager", manager
    )
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.video_hub_registry", registry
    )
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.resolve_camera_source",
        lambda camera_id, token: (_ for _ in ()).throw(
            __import__("app.common.errors", fromlist=["BusinessError"]).BusinessError(
                "camera_id=1001 视频会话尚未建立",
                status_code=503,
                code="WEBRTC_SESSION_ERROR",
            )
        ),
    )

    sdp_offer = _generate_sdp_offer()
    response = client.post(
        "/video-hub/cameras/1001/whip",
        data=sdp_offer,
        content_type="application/sdp",
        headers={"Authorization": "Bearer abc123"},
    )

    assert response.status_code == 503


def test_whip_delete_returns_200(monkeypatch):
    app = _build_app()
    client = app.test_client()

    registry = StubRegistryForWebrtc()
    session = registry.ensure_session(1001, "http://192.168.1.88/stream")
    _put_test_frame(session)

    manager = WebrtcSessionManager(registry=registry)
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.webrtc_session_manager", manager
    )
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.video_hub_registry", registry
    )
    monkeypatch.setattr(
        "app.api.video_hub_webrtc.resolve_camera_source",
        lambda camera_id, token: "http://192.168.1.88/stream",
    )

    sdp_offer = _generate_sdp_offer()
    whip_response = client.post(
        "/video-hub/cameras/1001/whip",
        data=sdp_offer,
        content_type="application/sdp",
        headers={"Authorization": "Bearer abc123"},
    )

    assert whip_response.status_code == 201
    location = whip_response.headers["Location"]
    session_id = location.rsplit("/", 1)[-1]

    delete_response = client.delete(f"/video-hub/sessions/{session_id}")
    assert delete_response.status_code == 200
    data = delete_response.get_json()
    assert data["data"]["session_id"] == session_id

    assert session_id not in manager._sessions


def test_pin_answer_candidates_keeps_only_preferred_ip():
    sdp = (
        "v=0\r\n"
        "m=video 9 UDP/TLS/RTP/SAVPF 96\r\n"
        "c=IN IP6 ::1\r\n"
        "a=candidate:1 1 UDP 100 192.168.0.181 5000 typ host\r\n"
        "a=candidate:2 1 UDP 200 192.168.137.1 5001 typ host\r\n"
        "a=candidate:3 1 UDP 300 169.254.1.1 5002 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address="192.168.0.181", fallback_ip=None)
    assert "192.168.0.181" in result
    assert "192.168.137.1" not in result
    assert "169.254.1.1" not in result
    assert "c=IN IP6 ::1" in result
    assert "m=video 9 " in result


def test_pin_answer_candidates_uses_fallback_ip():
    sdp = (
        "v=0\r\n"
        "m=video 9 UDP/TLS/RTP/SAVPF 96\r\n"
        "c=IN IP6 ::1\r\n"
        "a=candidate:1 1 UDP 100 192.168.0.181 5000 typ host\r\n"
        "a=candidate:2 1 UDP 200 8.8.8.8 5001 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address=None, fallback_ip="192.168.0.181")
    assert "192.168.0.181" in result
    assert "8.8.8.8" not in result


def test_pin_answer_candidates_removes_tcp():
    sdp = (
        "v=0\r\n"
        "m=video 9 UDP/TLS/RTP/SAVPF 96\r\n"
        "c=IN IP6 ::1\r\n"
        "a=candidate:1 1 TCP 100 192.168.0.181 9 typ host tcptype active\r\n"
        "a=candidate:2 1 UDP 200 192.168.0.181 5001 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address="192.168.0.181", fallback_ip=None)
    assert "192.168.0.181 5001" in result
    assert "TCP" not in result
    assert "tcptype" not in result


def test_pin_answer_candidates_no_preferred_removes_all():
    sdp = (
        "v=0\r\n"
        "m=video 9 UDP/TLS/RTP/SAVPF 96\r\n"
        "c=IN IP6 ::1\r\n"
        "a=candidate:1 1 UDP 100 192.168.0.181 5000 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address=None, fallback_ip=None)
    assert "192.168.0.181" not in result


def test_pin_answer_candidates_does_not_modify_m_or_c_lines():
    sdp = (
        "v=0\r\n"
        "m=video 56798 UDP/TLS/RTP/SAVPF 96\r\n"
        "c=IN IP6 2409:8d38:18:1369:863d:95d3:31d9:8f77\r\n"
        "a=candidate:abc 1 udp 2130706431 2409:8d38::1 56798 typ host\r\n"
        "a=candidate:def 1 udp 2130706431 192.168.0.181 56800 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address="192.168.0.181", fallback_ip=None)
    assert "m=video 56798 " in result
    assert "c=IN IP6 2409:8d38:18:1369:863d:95d3:31d9:8f77" in result
    assert "m=video 56800 " not in result
    assert "c=IN IP4 192.168.0.181" not in result


def test_force_setup_passive_changes_active_to_passive():
    sdp = "v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\na=setup:active\r\n"
    result = _force_setup_passive(sdp)
    assert "a=setup:passive" in result
    assert "a=setup:active" not in result


def test_force_setup_passive_keeps_passive_unchanged():
    sdp = "v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\na=setup:passive\r\n"
    result = _force_setup_passive(sdp)
    assert result == sdp


def test_force_setup_passive_keeps_actpass_unchanged():
    sdp = "v=0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96\r\na=setup:actpass\r\n"
    result = _force_setup_passive(sdp)
    assert "a=setup:actpass" in result
