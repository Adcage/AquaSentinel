from app.services import engine_task_service
from app.services.engine_task_service import EngineTaskState
from app.services.tracker_service import TrackedObject


def test_should_use_video_hub_for_http_camera_stream():
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_CAM_1001_1710000000000", "http://192.168.1.88/stream"
        )
        is True
    )
    assert (
        engine_task_service._should_use_video_hub_for_stream(
            "TASK_CAM_1001_1710000000000", "rtsp://192.168.1.88/live"
        )
        is True
    )


def test_sync_task_realtime_persists_frame_dimensions_and_timestamp():
    task_code = "TASK_CAM_1001_1710000000000"
    task = EngineTaskState(
        task_code=task_code,
        camera_code="CAM-1001",
        stream_url="http://192.168.1.88/stream",
        display_stream_url="http://127.0.0.1:5000/video-hub/cameras/1001/stream",
        frame_interval=0.2,
        model_version="v1",
    )
    engine_task_service._TASKS[task_code] = task

    try:
        engine_task_service._sync_task_realtime(
            task_code=task_code,
            tracked_objects=[
                TrackedObject(
                    track_id="track_1",
                    x_min=10,
                    y_min=20,
                    x_max=110,
                    y_max=220,
                    confidence=0.9,
                    label="person",
                    extra_json={},
                )
            ],
            frame_width=320,
            frame_height=240,
        )

        payload = engine_task_service._serialize_task(task)
        realtime = payload["realtime"]
        assert realtime["frame_width"] == 320
        assert realtime["frame_height"] == 240
        assert realtime["frame_ts"] > 0
    finally:
        engine_task_service._TASKS.pop(task_code, None)
