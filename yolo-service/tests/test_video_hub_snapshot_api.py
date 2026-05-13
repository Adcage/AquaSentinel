from app import create_app


class StubSession:
    def __init__(self, frame: bytes | None = None):
        self.frame = frame

    def get_latest_frame(self):
        if self.frame is None:
            return None
        return {
            "jpeg_bytes": self.frame,
            "frame_width": 320,
            "frame_height": 240,
            "timestamp": 1715500000123,
        }

    def status_dict(self):
        return {
            "camera_id": 1001,
            "connected": self.frame is not None,
            "last_frame_at": 1715500000123 if self.frame else None,
            "source_width": 320 if self.frame else 0,
            "source_height": 240 if self.frame else 0,
            "last_error": "" if self.frame else "暂无帧缓存",
            "viewer_count": 0,
            "source_url": "http://esp32-a/stream",
        }


class StubRegistry:
    def __init__(self, session: StubSession | None = None):
        self.session = session

    def get_session(self, camera_id: int):
        assert camera_id == 1001
        return self.session


def build_app():
    return create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": True,
        }
    )


def test_snapshot_endpoint_returns_latest_jpeg(monkeypatch):
    app = build_app()
    client = app.test_client()

    monkeypatch.setattr(
        "app.api.video_hub.video_hub_registry",
        StubRegistry(StubSession(b"\xff\xd8fakejpeg\xff\xd9")),
    )

    response = client.get("/video-hub/cameras/1001/snapshot")

    assert response.status_code == 200
    assert response.content_type == "image/jpeg"
    assert response.data.startswith(b"\xff\xd8")


def test_snapshot_endpoint_rejects_missing_frame(monkeypatch):
    app = build_app()
    client = app.test_client()

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", StubRegistry())

    response = client.get("/video-hub/cameras/1001/snapshot")

    assert response.status_code == 503
    payload = response.get_json()
    assert payload["code"] == "VIDEO_HUB_FRAME_UNAVAILABLE"
    assert "暂无可用视频帧" in payload["message"]
