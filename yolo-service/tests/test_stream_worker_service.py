import sys
import time
import types

from app import create_app
from app.services import engine_task_service
from app.services.drowning_rule_service import DrowningDecision
from app.services.tracker_service import TrackedObject


def build_app():
    return create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": True,
            "ENGINE_CALLBACK_INTERVAL_SEC": 0.5,
        }
    )


def test_stream_worker_via_video_hub_generates_drowning_event(monkeypatch):
    import numpy as np

    app = build_app()
    callback_payloads = []
    snapshot_call_count = {"n": 0}

    fake_jpeg = np.zeros((720, 1280, 3), dtype=np.uint8)
    import cv2

    encode_result = cv2.imencode(".jpg", fake_jpeg)
    fake_jpeg_bytes = encode_result[1].tobytes()

    class FakeVideoHubClient:
        def ensure_session(self, camera_id, source_url):
            return {"camera_id": camera_id, "status": "CONNECTED"}

        def fetch_snapshot(self, camera_id):
            snapshot_call_count["n"] += 1
            if snapshot_call_count["n"] > 5:
                return None
            return fake_jpeg_bytes

    class FakeTracker:
        backend = "simple"

        def __init__(self, *args, **kwargs):
            pass

        def update(self, detections, frame=None, timestamp=None):
            return [
                TrackedObject(
                    track_id="track_9",
                    x_min=12.0,
                    y_min=20.0,
                    x_max=120.0,
                    y_max=88.0,
                    confidence=0.96,
                    label="drowning",
                    extra_json={},
                )
            ]

    class FakeEvaluator:
        def __init__(self, *args, **kwargs):
            pass

        def evaluate(self, track, timestamp=None):
            return DrowningDecision(
                track_id=track.track_id,
                triggered=True,
                posture_score=0.91,
                thermal_score=0.94,
                duration_sec=4.2,
                posture_abnormal=True,
                thermal_abnormal=True,
                duration_abnormal=True,
            )

    def fake_callback(payload):
        callback_payloads.append(payload)
        task_code = payload.get("taskCode")
        if task_code:
            with engine_task_service._TASKS_LOCK:
                task = engine_task_service._TASKS.get(task_code)
                if task is not None:
                    task.stop_event.set()
        return True

    monkeypatch.setattr("app.services.engine_task_service.warmup_model", lambda: None)
    monkeypatch.setattr(
        "app.services.engine_task_service.infer_stream_frame",
        lambda _frame, model_version=None: [
            {
                "label": "person",
                "confidence": 0.95,
                "x_min": 10.0,
                "y_min": 18.0,
                "x_max": 118.0,
                "y_max": 90.0,
                "extra_json": {},
            }
        ],
    )
    monkeypatch.setattr(
        "app.services.engine_task_service.post_task_callback", fake_callback
    )
    monkeypatch.setattr(
        "app.services.engine_task_service.DeepSortTracker", FakeTracker
    )
    monkeypatch.setattr(
        "app.services.engine_task_service.DrowningRuleEvaluator", FakeEvaluator
    )
    monkeypatch.setattr(
        "app.services.engine_task_service.video_hub_client", FakeVideoHubClient()
    )

    with app.app_context():
        engine_task_service._TASKS.clear()
        task_code = "TASK_CAM_100_1710000000000"
        task_data = engine_task_service.start_task(
            task_code=task_code,
            camera_code="CAM-SH-PD-001",
            stream_url="rtsp://10.10.2.18/live/1",
            display_stream_url="http://127.0.0.1:8300/api/streams/cameras/100/preview",
            frame_interval=0.01,
            model_version="yolo11n-drown-v1",
        )

        assert task_data["status"] in ("RUNNING", "STOPPED")
        assert task_data["camera_code"] == "CAM-SH-PD-001"

        deadline = time.time() + 3.0
        status = "RUNNING"
        while time.time() < deadline:
            status = engine_task_service.get_task(task_code)["status"]
            if status == "STOPPED":
                break
            time.sleep(0.02)

        assert status == "STOPPED"

        latest_task = engine_task_service.get_task(task_code)
        assert latest_task["frames_processed"] >= 1
        assert callback_payloads
        event_payload = callback_payloads[0]
        assert event_payload["cameraCode"] == "CAM-SH-PD-001"
        assert event_payload["taskCode"] == task_code
        assert len(event_payload["eventUid"]) <= 64
        assert event_payload["targetId"] == "track_9"
        assert (
            event_payload["videoStreamUrl"]
            == "http://127.0.0.1:8300/api/streams/cameras/100/preview"
        )
        assert event_payload["extJson"]["postureAbnormal"] is True
        assert event_payload["extJson"]["thermalAbnormal"] is True

        engine_task_service._TASKS.clear()

    assert snapshot_call_count["n"] >= 1


def test_should_use_video_hub_for_rtsp():
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_CAM_100_test", "rtsp://10.10.2.18/live/1"
        )
        is True
    )


def test_should_use_video_hub_for_http():
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_CAM_100_test", "http://192.168.1.88/stream"
        )
        is True
    )


def test_should_use_video_hub_for_http_flv():
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_CAM_100_test", "http://127.0.0.1:18081/live/camera1.flv"
        )
        is True
    )


def test_should_not_use_video_hub_without_camera_id():
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_UNKNOWN", "rtsp://10.10.2.18/live/1"
        )
        is False
    )


def test_should_not_use_video_hub_with_empty_url():
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_CAM_100_test", ""
        )
        is False
    )


def test_run_loop_with_stream_routes_rtsp_to_video_hub(monkeypatch):
    called = {"video_hub": False}

    def fake_run_loop_with_video_hub(task_code, stream_url, frame_interval):
        called["video_hub"] = True

    monkeypatch.setattr(
        "app.services.engine_task_service._run_loop_with_video_hub",
        fake_run_loop_with_video_hub,
    )

    app = build_app()
    with app.app_context():
        engine_task_service._run_loop_with_stream(
            "TASK_CAM_100_1710000000000",
            "rtsp://10.10.2.18/live/1",
            0.2,
        )

    assert called["video_hub"] is True


def test_run_loop_with_stream_routes_http_flv_to_video_hub(monkeypatch):
    called = {"video_hub": False}

    def fake_run_loop_with_video_hub(task_code, stream_url, frame_interval):
        called["video_hub"] = True

    monkeypatch.setattr(
        "app.services.engine_task_service._run_loop_with_video_hub",
        fake_run_loop_with_video_hub,
    )

    app = build_app()
    with app.app_context():
        engine_task_service._run_loop_with_stream(
            "TASK_CAM_100_1710000000000",
            "http://127.0.0.1:18081/live/camera1.flv",
            0.2,
        )

    assert called["video_hub"] is True


def test_build_event_payload_event_uid_should_fit_backend_length_limit():
    task = engine_task_service.EngineTaskState(
        task_code="TASK_CAM_5001_1710000000000",
        camera_code="CAM-PD-0001",
        stream_url="rtsp://10.10.10.1/live/1",
        display_stream_url="",
        frame_interval=0.2,
        model_version="yolo11n-drown-v1",
    )
    tracked_object = TrackedObject(
        track_id="track_" + "x" * 80,
        x_min=10.0,
        y_min=20.0,
        x_max=110.0,
        y_max=120.0,
        confidence=0.98,
        label="drowning",
        extra_json={},
    )
    decision = DrowningDecision(
        track_id=tracked_object.track_id,
        triggered=True,
        posture_score=0.9,
        thermal_score=0.9,
        duration_sec=4.0,
        posture_abnormal=True,
        thermal_abnormal=True,
        duration_abnormal=True,
    )

    payload = engine_task_service._build_event_payload(
        task=task,
        tracked_object=tracked_object,
        decision=decision,
        head_count=1,
        frame_count=1234,
        tracker_backend="test",
        frame_width=640,
        frame_height=480,
    )

    assert len(payload["eventUid"]) <= 64
