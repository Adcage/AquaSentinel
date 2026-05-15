from app.video_hub.log_controls import should_log_after_cooldown
from app.video_hub.log_controls import should_log_frame_progress


def test_should_log_after_cooldown_logs_first_event():
    assert should_log_after_cooldown(None, 100.0, 60.0) is True


def test_should_log_after_cooldown_suppresses_within_window():
    assert should_log_after_cooldown(100.0, 130.0, 60.0) is False


def test_should_log_after_cooldown_allows_after_window():
    assert should_log_after_cooldown(100.0, 170.1, 60.0) is True


def test_should_log_frame_progress_logs_first_multiple():
    assert should_log_frame_progress(100, 100) is True


def test_should_log_frame_progress_skips_non_multiple():
    assert should_log_frame_progress(199, 100) is False


def test_should_log_frame_progress_uses_larger_interval():
    assert should_log_frame_progress(300, 300) is True
    assert should_log_frame_progress(600, 300) is True
