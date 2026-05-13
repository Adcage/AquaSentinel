from threading import Thread
from time import sleep

from app.video_hub.source_worker import VideoHubSession
from app.video_hub.frame_cache import FrameCache


def test_video_hub_session_disables_env_proxy():
    session = VideoHubSession(1001, "http://192.168.137.232/stream")

    assert session.http_session.trust_env is False


def test_video_hub_session_uses_short_read_timeout_for_reboot_recovery():
    session = VideoHubSession(1001, "http://192.168.137.232/stream")

    assert session.read_timeout_sec == 10.0


def test_wait_for_new_frame_blocks_until_new_version():
    cache = FrameCache()

    results: list[dict | None] = []

    def consumer():
        r1 = cache.wait_for_new_frame(5.0, after_version=0)
        results.append(r1)
        r2 = cache.wait_for_new_frame(5.0, after_version=r1["_version"] if r1 else 0)
        results.append(r2)

    t = Thread(target=consumer, daemon=True)
    t.start()
    sleep(0.05)

    assert len(results) == 0

    cache.update(b"\xff\xd8frame1", 320, 240, 1000)
    t.join(timeout=3)
    assert len(results) >= 1
    assert results[0]["jpeg_bytes"] == b"\xff\xd8frame1"
    assert results[0]["_version"] == 1

    cache.update(b"\xff\xd8frame2", 320, 240, 1001)
    t.join(timeout=3)
    assert len(results) == 2
    assert results[1]["jpeg_bytes"] == b"\xff\xd8frame2"
    assert results[1]["_version"] == 2


def test_wait_for_new_frame_returns_none_on_timeout():
    cache = FrameCache()

    result = cache.wait_for_new_frame(0.05, after_version=5)
    assert result is None


def test_wait_for_new_frame_returns_immediately_if_already_newer():
    cache = FrameCache()
    cache.update(b"\xff\xd8data", 640, 480, 1000)

    result = cache.wait_for_new_frame(0.1, after_version=0)
    assert result is not None
    assert result["_version"] == 1
