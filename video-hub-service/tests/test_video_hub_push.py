from time import sleep
from unittest.mock import MagicMock, patch

from app.video_hub.registry import VideoHubRegistry
from app.video_hub.source_worker import VideoHubSession, _parse_jpeg_size


def test_push_session_has_stream_mode():
    session = VideoHubSession(camera_id=5021, source_url="http://192.168.137.228/stream", stream_mode="push")
    assert session.stream_mode == "push"
    assert session.source_url == "http://192.168.137.228/stream"


def test_default_stream_mode_is_pull():
    session = VideoHubSession(camera_id=5021, source_url="rtsp://10.0.0.1/live/1")
    assert session.stream_mode == "pull"


def test_push_session_frame_cache_can_be_updated():
    session = VideoHubSession(camera_id=5021, source_url="http://192.168.137.228/stream", stream_mode="push")
    jpeg_data = b"\xff\xd8\xff\xe0\x00\x10JFIF"
    width, height = _parse_jpeg_size(jpeg_data)
    session.frame_cache.update(jpeg_data, width, height, 1000)
    result = session.frame_cache.wait_for_new_frame(1.0, after_version=0)
    assert result is not None
    assert result["jpeg_bytes"] == jpeg_data


def test_registry_get_or_create_session_creates_push_session():
    created = []

    def factory(camera_id, source_url, rotation=0, stream_mode="pull"):
        s = VideoHubSession(camera_id, source_url, rotation=rotation, stream_mode=stream_mode)
        created.append(s)
        return s

    registry = VideoHubRegistry(session_factory=factory)
    session = registry.get_or_create_session(5021)
    assert len(created) == 1
    assert created[0].stream_mode == "push"


def test_registry_get_or_create_session_reuses_existing():
    created = []

    def factory(camera_id, source_url, rotation=0, stream_mode="pull"):
        s = VideoHubSession(camera_id, source_url, rotation=rotation, stream_mode=stream_mode)
        created.append(s)
        return s

    registry = VideoHubRegistry(session_factory=factory)
    first = registry.ensure_session(5021, "http://192.168.137.228/stream", stream_mode="push")
    second = registry.get_or_create_session(5021)
    assert first is second
    assert len(created) == 1
    assert first.stream_mode == "push"
    assert first.source_url == "http://192.168.137.228/stream"


def test_registry_get_or_create_session_switches_pull_to_push():
    created = []

    def factory(camera_id, source_url, rotation=0, stream_mode="pull"):
        s = VideoHubSession(camera_id, source_url, rotation=rotation, stream_mode=stream_mode)
        created.append(s)
        return s

    registry = VideoHubRegistry(session_factory=factory)
    first = registry.ensure_session(5021, "http://192.168.137.228/stream")
    assert first.stream_mode == "pull"
    second = registry.get_or_create_session(5021)
    assert first is second
    assert first.stream_mode == "push"
    assert first.source_url == "http://192.168.137.228/stream"


def test_ensure_session_with_stream_mode_push():
    created = []

    def factory(camera_id, source_url, rotation=0, stream_mode="pull"):
        s = VideoHubSession(camera_id, source_url, rotation=rotation, stream_mode=stream_mode)
        created.append(s)
        return s

    registry = VideoHubRegistry(session_factory=factory)
    session = registry.ensure_session(5021, "http://192.168.137.228/stream", stream_mode="push")
    assert session.stream_mode == "push"
    assert session.source_url == "http://192.168.137.228/stream"


def test_parse_jpeg_size_on_minimal_jpeg():
    jpeg = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00"
    w, h = _parse_jpeg_size(jpeg)
    assert isinstance(w, int)
    assert isinstance(h, int)
    assert w >= 0 and h >= 0


def test_config_has_push_token():
    from app.core.config import BaseConfig

    assert hasattr(BaseConfig, "VIDEO_HUB_PUSH_TOKEN")
