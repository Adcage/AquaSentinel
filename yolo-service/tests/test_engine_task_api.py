from app import create_app


def build_app():
    return create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": True,
        }
    )


def test_engine_task_start_endpoint_forwards_payload(monkeypatch):
    app = build_app()
    client = app.test_client()

    captured = {}

    def fake_start_task(
        task_code: str,
        stream_url: str = "",
        display_stream_url: str = "",
        frame_interval: float | None = None,
        camera_code: str = "",
        model_version: str | None = None,
        drowning_alert_threshold_sec: float | None = None,
    ):
        captured["task_code"] = task_code
        captured["camera_code"] = camera_code
        captured["stream_url"] = stream_url
        captured["display_stream_url"] = display_stream_url
        captured["frame_interval"] = frame_interval
        captured["model_version"] = model_version
        captured["drowning_alert_threshold_sec"] = drowning_alert_threshold_sec
        return {
            "task_code": task_code,
            "camera_code": camera_code,
            "status": "RUNNING",
        }

    monkeypatch.setattr("app.api.engine_tasks.start_task", fake_start_task)

    resp = client.post(
        "/engine/tasks/start",
        json={
            "task_code": "TASK_CAM_100_1710000000000",
            "camera_code": "CAM-SH-PD-001",
            "stream_url": "rtsp://10.10.2.18/live/1",
            "display_stream_url": "http://127.0.0.1:8300/api/streams/cameras/100/preview",
            "frame_interval": 0.2,
            "model_version": "yolo11n-drown-v1",
            "drowning_alert_threshold_sec": 4.0,
        },
    )

    assert resp.status_code == 200
    assert captured["task_code"] == "TASK_CAM_100_1710000000000"
    assert captured["camera_code"] == "CAM-SH-PD-001"
    assert captured["stream_url"] == "rtsp://10.10.2.18/live/1"
    assert (
        captured["display_stream_url"]
        == "http://127.0.0.1:8300/api/streams/cameras/100/preview"
    )
    assert captured["frame_interval"] == 0.2
    assert captured["model_version"] == "yolo11n-drown-v1"
    assert captured["drowning_alert_threshold_sec"] == 4.0

    payload = resp.get_json()["data"]
    assert payload["status"] == "RUNNING"


def test_engine_task_start_endpoint_rejects_missing_task_code():
    app = build_app()
    client = app.test_client()

    resp = client.post(
        "/engine/tasks/start",
        json={
            "camera_code": "CAM-SH-PD-001",
            "stream_url": "rtsp://10.10.2.18/live/1",
        },
    )

    assert resp.status_code == 422
    payload = resp.get_json()
    assert payload["code"] == "PARAM_ERROR"


def test_engine_task_stop_and_get_endpoints(monkeypatch):
    app = build_app()
    client = app.test_client()

    monkeypatch.setattr(
        "app.api.engine_tasks.stop_task",
        lambda task_code: {
            "task_code": task_code,
            "status": "STOPPED",
        },
    )
    monkeypatch.setattr(
        "app.api.engine_tasks.get_task",
        lambda task_code: {
            "task_code": task_code,
            "status": "RUNNING",
        },
    )

    stop_resp = client.post(
        "/engine/tasks/stop",
        json={"task_code": "TASK_CAM_100_1710000000000"},
    )
    assert stop_resp.status_code == 200
    assert stop_resp.get_json()["data"]["status"] == "STOPPED"

    get_resp = client.get("/engine/tasks/TASK_CAM_100_1710000000000")
    assert get_resp.status_code == 200
    assert get_resp.get_json()["data"]["status"] == "RUNNING"


def test_engine_task_stop_endpoint_idempotent_when_task_missing():
    app = build_app()
    client = app.test_client()

    resp = client.post(
        "/engine/tasks/stop",
        json={"task_code": "TASK_CAM_999_1710000000999"},
    )

    assert resp.status_code == 200
    payload = resp.get_json()["data"]
    assert payload["task_code"] == "TASK_CAM_999_1710000000999"
    assert payload["status"] == "STOPPED"


def test_engine_task_model_switch_endpoint_forwards_payload(monkeypatch):
    app = build_app()
    client = app.test_client()

    captured = {}

    def fake_switch_task_model(task_code: str, model_version: str):
        captured["task_code"] = task_code
        captured["model_version"] = model_version
        return {
            "task_code": task_code,
            "status": "RUNNING",
            "model_version": model_version,
        }

    monkeypatch.setattr(
        "app.api.engine_tasks.switch_task_model",
        fake_switch_task_model,
    )

    resp = client.post(
        "/engine/tasks/model/switch",
        json={
            "task_code": "TASK_CAM_100_1710000000000",
            "model_version": "yolo11n-drown-v2",
        },
    )

    assert resp.status_code == 200
    assert captured["task_code"] == "TASK_CAM_100_1710000000000"
    assert captured["model_version"] == "yolo11n-drown-v2"
    assert resp.get_json()["data"]["model_version"] == "yolo11n-drown-v2"


def test_engine_task_config_update_endpoint_forwards_payload(monkeypatch):
    app = build_app()
    client = app.test_client()

    captured = {}

    def fake_update_task_config(
        task_code: str,
        drowning_alert_threshold_sec: float | None = None,
    ):
        captured["task_code"] = task_code
        captured["drowning_alert_threshold_sec"] = drowning_alert_threshold_sec
        return {
            "task_code": task_code,
            "drowning_alert_threshold_sec": drowning_alert_threshold_sec,
            "status": "RUNNING",
        }

    monkeypatch.setattr(
        "app.api.engine_tasks.update_task_config",
        fake_update_task_config,
    )

    resp = client.post(
        "/engine/tasks/config/update",
        json={
            "task_code": "TASK_CAM_100_1710000000000",
            "drowning_alert_threshold_sec": 6.0,
        },
    )

    assert resp.status_code == 200
    assert captured["task_code"] == "TASK_CAM_100_1710000000000"
    assert captured["drowning_alert_threshold_sec"] == 6.0
