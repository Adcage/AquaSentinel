from app import create_app


class StubStreamSession:
    def __init__(self):
        self.viewer_count = 0

    def open_stream(self):
        self.viewer_count += 1

        def generator():
            yield b"--frame\r\n"
            yield b"Content-Type: image/jpeg\r\n"
            yield b"Content-Length: 12\r\n\r\n"
            yield b"\xff\xd8fakejpeg\xff\xd9"
            yield b"\r\n"

        return generator()


class StubRegistry:
    def __init__(self, session: StubStreamSession):
        self.session = session
        self.ensure_calls: list[tuple[int, str]] = []

    def get_session(self, camera_id: int):
        assert camera_id == 1001
        return self.session

    def ensure_session(self, camera_id: int, source_url: str):
        assert camera_id == 1001
        self.ensure_calls.append((camera_id, source_url))
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


def test_stream_endpoint_returns_platform_mjpeg(monkeypatch):
    app = build_app()
    client = app.test_client()
    session = StubStreamSession()

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", StubRegistry(session))

    response = client.get("/video-hub/cameras/1001/stream")

    assert response.status_code == 200
    assert response.content_type.startswith("multipart/x-mixed-replace")
    assert b"fakejpeg" in response.data
    assert session.viewer_count == 1


def test_stream_endpoint_updates_existing_session_source_url(monkeypatch):
    app = build_app()
    client = app.test_client()
    session = StubStreamSession()
    registry = StubRegistry(session)

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.get(
        "/video-hub/cameras/1001/stream?source_url=http://192.168.137.178/stream"
    )

    assert response.status_code == 200
    assert registry.ensure_calls == [(1001, "http://192.168.137.178/stream")]
