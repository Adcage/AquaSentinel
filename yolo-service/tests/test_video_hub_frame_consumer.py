import sys
import time
import types

from app import create_app
from app.services import engine_task_service
from app.services.engine_task_service import EngineTaskState
from app.services.tracker_service import TrackedObject


def build_app():
    return create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": True,
        }
    )


def test_run_loop_with_video_hub_consumes_frame_cache(monkeypatch):
    app = build_app()
    ensured = {}

    class FakeFrameCache:
        def __init__(self):
            self.calls = 0

        def wait_for_frame(self, _timeout):
            self.calls += 1
            return {
                "jpeg_bytes": b"fake-jpeg-bytes",
                "timestamp": 1715500000123,
            }

    class FakeSession:
        def __init__(self):
            self.frame_cache = FakeFrameCache()

    class FakeRegistry:
        def __init__(self):
            self.session = FakeSession()

        def ensure_session(self, camera_id, source_url):
            ensured["camera_id"] = camera_id
            ensured["source_url"] = source_url
            return self.session

    class FakeTracker:
        backend = "simple"

        def __init__(self, *args, **kwargs):
            pass

        def update(self, detections, frame=None, timestamp=None):
            return [
                TrackedObject(
                    track_id="track_1",
                    x_min=10.0,
                    y_min=20.0,
                    x_max=110.0,
                    y_max=220.0,
                    confidence=0.91,
                    label="person",
                    extra_json={},
                )
            ]

    class FakeEvaluator:
        def __init__(self, *args, **kwargs):
            pass

        def evaluate(self, tracked_object, timestamp=None):
            return types.SimpleNamespace(
                track_id=tracked_object.track_id,
                triggered=False,
                posture_score=0.0,
                thermal_score=0.0,
                duration_sec=0.0,
                posture_abnormal=False,
                thermal_abnormal=False,
                duration_abnormal=False,
            )

    fake_cv2 = types.SimpleNamespace(
        IMREAD_COLOR=1,
        imdecode=lambda _buffer, _mode: types.SimpleNamespace(shape=(240, 320, 3)),
    )
    fake_numpy = types.SimpleNamespace(frombuffer=lambda data, dtype=None: data, uint8="uint8")

    monkeypatch.setitem(sys.modules, "cv2", fake_cv2)
    monkeypatch.setitem(sys.modules, "numpy", fake_numpy)
    monkeypatch.setattr("app.services.engine_task_service.video_hub_registry", FakeRegistry())
    monkeypatch.setattr("app.services.engine_task_service.DeepSortTracker", FakeTracker)
    monkeypatch.setattr(
        "app.services.engine_task_service.DrowningRuleEvaluator", FakeEvaluator
    )
    monkeypatch.setattr(
        "app.services.engine_task_service.infer_stream_frame",
        lambda _frame, model_version=None: [
            {
                "label": "person",
                "confidence": 0.91,
                "x_min": 10.0,
                "y_min": 20.0,
                "x_max": 110.0,
                "y_max": 220.0,
                "extra_json": {},
            }
        ],
    )
    monkeypatch.setattr("app.services.engine_task_service.record_inference", lambda **kwargs: None)
    monkeypatch.setattr(
        "app.services.engine_task_service._push_realtime_ws", lambda *args, **kwargs: None
    )
    monkeypatch.setattr(
        "app.services.engine_task_service._post_detection_event_if_needed",
        lambda *args, **kwargs: None,
    )
    monkeypatch.setattr(
        "app.services.engine_task_service.video_frame_push_service.push_frame",
        lambda *args, **kwargs: None,
    )

    task_code = "TASK_CAM_1001_1710000000000"
    task = EngineTaskState(
        task_code=task_code,
        camera_code="CAM-1001",
        stream_url="http://192.168.1.88/stream",
        display_stream_url="http://127.0.0.1:5000/video-hub/cameras/1001/stream",
        frame_interval=0.01,
        model_version="v1",
    )
    engine_task_service._TASKS[task_code] = task

    def stop_after_first_progress(task_code_param):
        with engine_task_service._TASKS_LOCK:
            task_ref = engine_task_service._TASKS[task_code_param]
            task_ref.frames_processed += 1
            task_ref.stop_event.set()
            return task_ref.frames_processed

    monkeypatch.setattr(
        "app.services.engine_task_service._touch_task_progress",
        stop_after_first_progress,
    )

    with app.app_context():
        try:
            engine_task_service._run_loop_with_video_hub(
                task_code,
                "http://192.168.1.88/stream",
                0.01,
            )

            payload = engine_task_service._serialize_task(task)
            realtime = payload["realtime"]
            assert ensured == {
                "camera_id": 1001,
                "source_url": "http://192.168.1.88/stream",
            }
            assert realtime["frame_width"] == 320
            assert realtime["frame_height"] == 240
            assert realtime["frame_ts"] == 1715500000.123
            assert payload["frames_processed"] == 1
            assert realtime["detections"][0]["track_id"] == "track_1"
        finally:
            engine_task_service._TASKS.pop(task_code, None)
