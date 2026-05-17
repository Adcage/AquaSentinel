from app.video_hub.registry import VideoHubRegistry


class DummySession:
    def __init__(self, camera_id: int, source_url: str, rotation: int = 0, stream_mode: str = "pull"):
        self.camera_id = camera_id
        self.source_url = source_url
        self.rotation = rotation
        self.stream_mode = stream_mode
        self.start_calls = 0
        self.state = "CONNECTING"

    def ensure_started(self):
        self.start_calls += 1

    def activate_from_circuit_open(self):
        self.state = "CONNECTING"

    def switch_to_push_mode(self):
        self.stream_mode = "push"


def test_registry_reuses_existing_session_for_same_camera():
    created: list[DummySession] = []

    def factory(camera_id: int, source_url: str, rotation: int = 0, stream_mode: str = "pull"):
        session = DummySession(camera_id, source_url, rotation=rotation, stream_mode=stream_mode)
        created.append(session)
        return session

    registry = VideoHubRegistry(session_factory=factory)

    first = registry.ensure_session(1001, "http://esp32-a/stream")
    second = registry.ensure_session(1001, "http://esp32-b/stream")

    assert first is second
    assert len(created) == 1
    assert first.source_url == "http://esp32-b/stream"
    assert first.start_calls == 1


def test_registry_updates_source_url_when_changed():
    created: list[DummySession] = []

    def factory(camera_id: int, source_url: str, rotation: int = 0, stream_mode: str = "pull"):
        session = DummySession(camera_id, source_url, rotation=rotation, stream_mode=stream_mode)
        created.append(session)
        return session

    registry = VideoHubRegistry(session_factory=factory)

    first = registry.ensure_session(1001, "http://192.168.1.100/stream")
    assert first.source_url == "http://192.168.1.100/stream"

    second = registry.ensure_session(1001, "http://192.168.1.200/stream")
    assert first is second
    assert first.source_url == "http://192.168.1.200/stream"
