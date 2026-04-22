import pytest

from app.services.manual_alert_trigger_service import (
    build_trigger_payload,
    parse_command,
)


def test_parse_command_accepts_plain_camera_id():
    action, data = parse_command("5001")
    assert action == "trigger"
    assert data == {"camera_id": 5001, "times": 1, "interval_sec": 1.0}


def test_parse_command_accepts_trigger_syntax():
    action, data = parse_command("trigger 6002 3 2.5")
    assert action == "trigger"
    assert data == {"camera_id": 6002, "times": 3, "interval_sec": 2.5}


def test_parse_command_exit():
    action, data = parse_command("q")
    assert action == "exit"
    assert data == {}


def test_parse_command_invalid_camera_id_raises():
    with pytest.raises(ValueError):
        parse_command("trigger abc")


def test_build_trigger_payload_contains_expected_fields():
    payload = build_trigger_payload(
        camera_id=5001,
        posture_score=0.95,
        thermal_score=0.88,
        duration_sec=4.1,
    )
    assert payload["camera_id"] == 5001
    assert payload["posture_score"] == 0.95
    assert payload["thermal_score"] == 0.88
    assert payload["duration_sec"] == 4.1
