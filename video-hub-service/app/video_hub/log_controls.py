from __future__ import annotations


def should_log_after_cooldown(
    last_logged_at: float | None,
    now_ts: float,
    cooldown_sec: float,
) -> bool:
    if last_logged_at is None:
        return True
    return now_ts - last_logged_at >= cooldown_sec


def should_log_frame_progress(version: int, interval: int) -> bool:
    if interval <= 0:
        return False
    return version > 0 and version % interval == 0
