from __future__ import annotations

import pytest

from app import create_app
from app.common.errors import BusinessError
from app.security import camera_source_resolver


class _DummyResponse:
    def __init__(self, status_code: int, payload: dict):
        self.status_code = status_code
        self._payload = payload

    def json(self):
        return self._payload


def test_resolve_camera_source_rejects_missing_token():
    app = create_app({"TESTING": True})
    with app.app_context():
        with pytest.raises(BusinessError) as exc_info:
            camera_source_resolver.resolve_camera_source(5021, "")
    assert exc_info.value.status_code == 401


def test_resolve_camera_source_calls_backend(monkeypatch):
    app = create_app({"TESTING": True, "VIDEO_HUB_BACKEND_BASE_URL": "http://backend"})
    called: dict[str, object] = {}

    def _fake_get(url: str, params: dict[str, object], timeout: int):
        called["url"] = url
        called["params"] = params
        called["timeout"] = timeout
        return _DummyResponse(200, {"code": 0, "data": {"sourceUrl": "http://camera/stream"}})

    monkeypatch.setattr(camera_source_resolver._resolve_session, "get", _fake_get)

    with app.app_context():
        source_url = camera_source_resolver.resolve_camera_source(5021, "abc123")

    assert source_url == "http://camera/stream"
    assert called == {
        "url": "http://backend/api/video-hub/auth/camera-source",
        "params": {"cameraId": 5021, "token": "abc123"},
        "timeout": 5,
    }


def test_resolve_camera_source_rejects_non_200(monkeypatch):
    app = create_app({"TESTING": True, "VIDEO_HUB_BACKEND_BASE_URL": "http://backend"})
    monkeypatch.setattr(
        camera_source_resolver._resolve_session,
        "get",
        lambda url, params, timeout: _DummyResponse(401, {"code": 40100}),
    )

    with app.app_context():
        with pytest.raises(BusinessError) as exc_info:
            camera_source_resolver.resolve_camera_source(5021, "bad")

    assert exc_info.value.status_code == 401
