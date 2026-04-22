import hmac
import hashlib
import json

from app import create_app
from app.services.callback_client_service import _build_signature, post_task_callback


def build_app(callback_url: str):
    return create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": True,
            "CALLBACK_URL": callback_url,
            "CALLBACK_KEY": "ai-service",
            "CALLBACK_SECRET": "demo-secret",
            "CALLBACK_RETRY_TIMES": 3,
            "CALLBACK_TIMEOUT_SEC": 5,
        }
    )


def test_build_signature_matches_standard_hmac():
    timestamp = "1710000000"
    body = '{"eventUid":"evt_1"}'

    expected = hmac.new(
        b"demo-secret",
        f"{timestamp}\n{body}".encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

    assert _build_signature("demo-secret", timestamp, body) == expected


def test_post_task_callback_sends_signed_request(monkeypatch):
    app = build_app("http://127.0.0.1:8101/api/internal/ai/events")
    captured = {}

    class _Response:
        ok = True
        status_code = 200

    def fake_post(url, data, headers, timeout):
        captured["url"] = url
        captured["data"] = data
        captured["headers"] = headers
        captured["timeout"] = timeout
        return _Response()

    monkeypatch.setattr("app.services.callback_client_service.requests.post", fake_post)
    monkeypatch.setattr(
        "app.services.callback_client_service.time.time", lambda: 1710000000
    )

    with app.app_context():
        ok = post_task_callback({"eventUid": "evt_1", "riskType": "DROWING"})

    assert ok is True
    assert captured["url"] == "http://127.0.0.1:8101/api/internal/ai/events"
    assert captured["timeout"] == 5.0
    assert captured["headers"]["X-AI-Key"] == "ai-service"
    assert captured["headers"]["X-AI-Timestamp"] == "1710000000"

    body_text = captured["data"].decode("utf-8")
    expected_signature = _build_signature(
        "demo-secret",
        "1710000000",
        body_text,
    )
    assert captured["headers"]["X-AI-Signature"] == expected_signature
    assert json.loads(body_text)["eventUid"] == "evt_1"


def test_post_task_callback_retries_then_succeeds(monkeypatch):
    app = build_app("http://127.0.0.1:8101/api/internal/ai/events")
    attempts = []
    sleeps = []

    class _Response:
        def __init__(self, ok: bool, status_code: int):
            self.ok = ok
            self.status_code = status_code

    def fake_post(url, data, headers, timeout):
        attempts.append((url, timeout))
        if len(attempts) == 1:
            return _Response(False, 500)
        return _Response(True, 200)

    monkeypatch.setattr("app.services.callback_client_service.requests.post", fake_post)
    monkeypatch.setattr(
        "app.services.callback_client_service.time.sleep",
        lambda sec: sleeps.append(sec),
    )

    with app.app_context():
        ok = post_task_callback({"eventUid": "evt_2", "riskType": "DROWING"})

    assert ok is True
    assert len(attempts) == 2
    assert len(sleeps) == 1
    assert sleeps[0] == 0.2


def test_post_task_callback_returns_false_when_callback_url_empty(monkeypatch):
    app = build_app("")

    def fail_post(*args, **kwargs):
        raise AssertionError("requests.post should not be called")

    monkeypatch.setattr("app.services.callback_client_service.requests.post", fail_post)

    with app.app_context():
        ok = post_task_callback({"eventUid": "evt_3"})

    assert ok is False
