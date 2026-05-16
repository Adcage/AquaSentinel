from __future__ import annotations

import json
from unittest.mock import MagicMock, patch

from app.video_hub.redis_sync import RedisStreamSync, STREAM_HASH_KEY, EVENT_CHANNEL


class FakeRegistry:
    def __init__(self):
        self.sessions: dict[int, str] = {}
        self.removed: list[int] = []

    def ensure_session(self, camera_id: int, source_url: str):
        self.sessions[camera_id] = source_url

    def remove_session(self, camera_id: int):
        self.sessions.pop(camera_id, None)
        self.removed.append(camera_id)


def test_start_skips_when_redis_url_empty():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync.start("")
    assert registry.sessions == {}
    assert sync._thread is None


def test_start_skips_when_redis_url_none():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync.start(None)
    assert registry.sessions == {}


@patch("app.video_hub.redis_sync.redis")
def test_start_syncs_all_cameras_from_hash(mock_redis_module):
    mock_r = MagicMock()
    mock_r.hgetall.return_value = {
        "1": json.dumps({"stream_url": "http://cam1/stream", "enabled": True}),
        "2": json.dumps({"stream_url": "rtsp://cam2/live", "enabled": True}),
    }
    mock_redis_module.from_url.return_value = mock_r
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync.start("redis://localhost:6379/1")
    assert registry.sessions == {1: "http://cam1/stream", 2: "rtsp://cam2/live"}
    sync.stop()


@patch("app.video_hub.redis_sync.redis")
def test_start_handles_empty_hash(mock_redis_module):
    mock_r = MagicMock()
    mock_r.hgetall.return_value = {}
    mock_redis_module.from_url.return_value = mock_r
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync.start("redis://localhost:6379/1")
    assert registry.sessions == {}
    sync.stop()


@patch("app.video_hub.redis_sync.redis")
def test_start_handles_redis_connection_error(mock_redis_module):
    mock_redis_module.from_url.side_effect = Exception("Connection refused")
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync.start("redis://localhost:6379/1")
    assert registry.sessions == {}


def test_handle_event_upsert():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync._handle_event({"action": "upsert", "camera_id": 10, "stream_url": "rtsp://cam10/live"})
    assert registry.sessions[10] == "rtsp://cam10/live"


def test_handle_event_upsert_http():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync._handle_event({"action": "upsert", "camera_id": 11, "stream_url": "http://cam11/stream"})
    assert registry.sessions[11] == "http://cam11/stream"


def test_handle_event_upsert_skips_empty_url():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync._handle_event({"action": "upsert", "camera_id": 10, "stream_url": ""})
    assert 10 not in registry.sessions


def test_handle_event_delete():
    registry = FakeRegistry()
    registry.sessions[20] = "rtsp://cam20/live"
    sync = RedisStreamSync(registry)
    sync._handle_event({"action": "delete", "camera_id": 20})
    assert 20 not in registry.sessions
    assert 20 in registry.removed


def test_handle_event_ignores_unknown_action():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync._handle_event({"action": "unknown", "camera_id": 30})
    assert 30 not in registry.sessions


def test_handle_event_ignores_missing_camera_id():
    registry = FakeRegistry()
    sync = RedisStreamSync(registry)
    sync._handle_event({"action": "upsert", "stream_url": "http://x/stream"})
    assert len(registry.sessions) == 0
