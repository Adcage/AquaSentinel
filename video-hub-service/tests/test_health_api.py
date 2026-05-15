from __future__ import annotations

from app import create_app


def test_health_returns_ok():
    app = create_app()
    client = app.test_client()
    response = client.get("/health")
    assert response.status_code == 200
    payload = response.get_json()
    assert payload["code"] == "OK"
    assert payload["data"]["service"] == "video-hub-service"
