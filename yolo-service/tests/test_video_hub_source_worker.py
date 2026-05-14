from threading import Thread
from time import sleep, time

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


def test_frame_cache_records_last_frame_at():
    cache = FrameCache()
    assert cache.last_frame_at() is None
    cache.update(b"\xff\xd8\xff\xe0", 320, 240)
    ts = cache.last_frame_at()
    assert ts is not None
    assert time() - ts < 1.0


def test_session_initial_state_is_connecting():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    assert session.state == "CONNECTING"


def test_state_transitions_to_connected_on_success():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    session._transition_to_connected()
    assert session.state == "CONNECTED"


def test_state_transitions_to_stale_on_no_frames():
    session = VideoHubSession(
        camera_id=1,
        source_url="http://192.168.1.88/stream",
        stale_frame_timeout_sec=0.1,
    )
    session._transition_to_connected()
    sleep(0.2)
    session._check_stale_frame()
    assert session.state == "STALE"


def test_state_transitions_to_circuit_open_after_10_failures():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    for _ in range(10):
        session._record_failure("ConnectionRefusedError")
    assert session.state == "CIRCUIT_OPEN"
    assert session.consecutive_failures == 10


def test_retry_delay_backoff():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    assert session._calc_retry_delay(1) == 1.5
    assert session._calc_retry_delay(3) == 3.0
    assert session._calc_retry_delay(5) == 5.0
    assert session._calc_retry_delay(7) == 10.0
    assert session._calc_retry_delay(10) == 60.0
    assert session._calc_retry_delay(20) == 60.0


def test_session_stop_terminates_loop():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    assert not session._stopped
    session.stop()
    assert session._stopped


def test_circuit_open_activated_by_ensure():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    for _ in range(10):
        session._record_failure("err")
    assert session.state == "CIRCUIT_OPEN"
    session.activate_from_circuit_open()
    assert session.state == "CONNECTING"
    assert session.consecutive_failures == 0
