from app import create_app
from app.video_hub.registry import VideoHubRegistry
from app.video_hub.source_worker import VideoHubSession


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

    def ensure_session(self, camera_id: int, source_url: str, rotation: int = 0):
        assert camera_id == 1001
        self.ensure_calls.append((camera_id, source_url))
        return self.session


def build_app():
    return create_app()


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


class StubSessionForManagement:
    def __init__(self, camera_id: int, source_url: str, rotation: int = 0):
        self.camera_id = camera_id
        self.source_url = source_url
        self.rotation = rotation
        self._stopped = False
        self._state = "CONNECTING"
        self._consecutive_failures = 0

    @property
    def state(self):
        return self._state

    @property
    def consecutive_failures(self):
        return self._consecutive_failures

    def stop(self):
        self._stopped = True

    def activate_from_circuit_open(self):
        if self._state == "CIRCUIT_OPEN":
            self._state = "CONNECTING"
            self._consecutive_failures = 0

    def status_dict(self):
        return {
            "camera_id": self.camera_id,
            "state": self._state,
            "connected": self._state == "CONNECTED",
            "consecutive_failures": self._consecutive_failures,
        }


class StubRegistryForManagement:
    def __init__(self):
        self._sessions: dict[int, StubSessionForManagement] = {}

    def ensure_session(self, camera_id: int, source_url: str, rotation: int = 0):
        if camera_id not in self._sessions:
            self._sessions[camera_id] = StubSessionForManagement(camera_id, source_url, rotation=rotation)
        return self._sessions[camera_id]

    def get_session(self, camera_id: int):
        return self._sessions.get(camera_id)

    def remove_session(self, camera_id: int):
        session = self._sessions.pop(camera_id, None)
        if session is not None:
            session.stop()


def test_reconnect_endpoint_returns_200(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()
    registry.ensure_session(1001, "http://192.168.1.88/stream")

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.post("/video-hub/cameras/1001/reconnect")

    assert response.status_code == 200
    data = response.get_json()
    assert data["data"]["camera_id"] == 1001


def test_reconnect_endpoint_activates_circuit_open(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()
    session = registry.ensure_session(1001, "http://192.168.1.88/stream")
    session._state = "CIRCUIT_OPEN"
    session._consecutive_failures = 10

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.post("/video-hub/cameras/1001/reconnect")

    assert response.status_code == 200
    assert session.state == "CONNECTING"
    assert session.consecutive_failures == 0


def test_reconnect_endpoint_returns_404_when_no_session(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.post("/video-hub/cameras/1001/reconnect")

    assert response.status_code == 404


def test_delete_session_endpoint_returns_200(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()
    session = registry.ensure_session(1001, "http://192.168.1.88/stream")

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.delete("/video-hub/cameras/1001/session")

    assert response.status_code == 200
    assert registry.get_session(1001) is None
    assert session._stopped


def test_delete_session_endpoint_returns_404_when_no_session(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.delete("/video-hub/cameras/1001/session")

    assert response.status_code == 404


def test_status_includes_state_fields(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()
    registry.ensure_session(1001, "http://192.168.1.88/stream")

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.get("/video-hub/cameras/1001/status")

    assert response.status_code == 200
    data = response.get_json()["data"]
    assert "state" in data
    assert "consecutive_failures" in data


def test_ensure_activates_circuit_open(monkeypatch):
    app = build_app()
    client = app.test_client()
    registry = StubRegistryForManagement()
    session = registry.ensure_session(1001, "http://192.168.1.88/stream")
    session._state = "CIRCUIT_OPEN"
    session._consecutive_failures = 10

    monkeypatch.setattr("app.api.video_hub.video_hub_registry", registry)

    response = client.post(
        "/video-hub/cameras/1001/ensure",
        json={"source_url": "http://192.168.1.88/stream"},
    )

    assert response.status_code == 200
    assert session.state == "CONNECTING"
    assert session.consecutive_failures == 0


def test_registry_remove_session_stops_and_removes():
    registry = VideoHubRegistry()
    session = registry.ensure_session(1, "http://192.168.1.88/stream")
    assert registry.get_session(1) is not None
    registry.remove_session(1)
    assert registry.get_session(1) is None
    assert session._stopped
