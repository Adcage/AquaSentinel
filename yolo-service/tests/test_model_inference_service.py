import json
import sys
import types

import pytest

from app import create_app
from app.core.errors import BusinessError
from app.services import model_inference_service
from app.services.model_inference_service import infer_image, infer_video


@pytest.fixture(autouse=True)
def _reset_model_cache():
    model_inference_service._MODEL_INSTANCES.clear()
    model_inference_service._MODEL_PATH_BY_VERSION.clear()
    model_inference_service._MODEL_WARMED_VERSIONS.clear()
    yield
    model_inference_service._MODEL_INSTANCES.clear()
    model_inference_service._MODEL_PATH_BY_VERSION.clear()
    model_inference_service._MODEL_WARMED_VERSIONS.clear()


def test_infer_image_raises_when_ultralytics_model_load_fails(tmp_path, monkeypatch):
    model_path = tmp_path / "best.pt"
    image_path = tmp_path / "demo.png"
    model_path.write_bytes(b"fake-weights")
    image_path.write_bytes(b"fake-image")

    class _FakeYOLO:
        def __init__(self, _):
            raise RuntimeError("weights not supported")

    fake_ultralytics = types.SimpleNamespace(YOLO=_FakeYOLO)
    monkeypatch.setitem(sys.modules, "ultralytics", fake_ultralytics)

    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": False,
            "MODEL_PATH": model_path.as_posix(),
        }
    )

    with app.app_context():
        with pytest.raises(BusinessError, match="ultralytics model load failed"):
            infer_image(image_path.as_posix())


def test_infer_video_raises_when_ultralytics_predict_fails(tmp_path, monkeypatch):
    model_path = tmp_path / "best.pt"
    video_path = tmp_path / "demo.mp4"
    model_path.write_bytes(b"fake-weights")
    video_path.write_bytes(b"fake-video")

    class _FakeModel:
        def predict(self, **kwargs):
            if kwargs["conf"] == 0.01:
                return []
            assert kwargs["conf"] == 0.25
            raise RuntimeError("predict failed")

    class _FakeYOLO:
        def __init__(self, _):
            self.model = _FakeModel()

        def predict(self, **kwargs):
            return self.model.predict(**kwargs)

    class _FakeCapture:
        def __init__(self, source):
            assert source == video_path.as_posix()
            self._frames = [object()]

        def isOpened(self):
            return True

        def get(self, _):
            return 25.0

        def read(self):
            if not self._frames:
                return False, None
            return True, self._frames.pop(0)

        def release(self):
            return None

    fake_ultralytics = types.SimpleNamespace(YOLO=_FakeYOLO)
    fake_cv2 = types.SimpleNamespace(CAP_PROP_FPS=5, VideoCapture=_FakeCapture)
    monkeypatch.setitem(sys.modules, "ultralytics", fake_ultralytics)
    monkeypatch.setitem(sys.modules, "cv2", fake_cv2)

    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": False,
            "MODEL_PATH": model_path.as_posix(),
            "VIDEO_INFER_FRAME_INTERVAL": 0.2,
        }
    )

    with app.app_context():
        with pytest.raises(BusinessError, match="ultralytics inference failed"):
            infer_video(video_path.as_posix())


def test_infer_image_filters_unknown_labels_by_labels_json(tmp_path, monkeypatch):
    model_path = tmp_path / "best.pt"
    labels_path = tmp_path / "labels.json"
    image_path = tmp_path / "demo.png"
    model_path.write_bytes(b"fake-weights")
    image_path.write_bytes(b"fake-image")
    labels_path.write_text(
        json.dumps({"labels": [{"en": "drone", "zh": "无人机"}]}, ensure_ascii=False),
        encoding="utf-8",
    )

    class _ValueHolder:
        def __init__(self, value):
            self._value = value

        def item(self):
            return self._value

    class _XYXYHolder:
        def __init__(self, values):
            self._values = values

        def tolist(self):
            return self._values

    class _FakeBox:
        def __init__(self, cls_idx, conf, xyxy):
            self.cls = _ValueHolder(cls_idx)
            self.conf = _ValueHolder(conf)
            self.xyxy = [_XYXYHolder(xyxy)]

    class _FakeModel:
        def predict(self, **kwargs):
            source = kwargs["source"]
            if isinstance(source, str):
                assert source == image_path.as_posix()
            result = types.SimpleNamespace()
            result.names = {0: "Tank", 1: "drone"}
            result.boxes = [
                _FakeBox(0, 0.9, [1, 2, 3, 4]),
                _FakeBox(1, 0.8, [5, 6, 7, 8]),
            ]
            return [result]

    class _FakeYOLO:
        def __init__(self, _):
            self.model = _FakeModel()

        def predict(self, **kwargs):
            return self.model.predict(**kwargs)

    fake_ultralytics = types.SimpleNamespace(YOLO=_FakeYOLO)
    monkeypatch.setitem(sys.modules, "ultralytics", fake_ultralytics)

    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": False,
            "MODEL_PATH": model_path.as_posix(),
            "MODEL_LABELS_PATH": labels_path.as_posix(),
        }
    )

    with app.app_context():
        detections = infer_image(image_path.as_posix())

    assert len(detections) == 1
    assert detections[0]["label"] == "drone"