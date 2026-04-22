from __future__ import annotations

from pathlib import Path
import threading
from typing import Any, cast

from flask import current_app

from app.core.errors import BusinessError
from app.models.detection_common import get_label_set

_MODEL_LOCK = threading.Lock()
_WARMUP_LOCK = threading.Lock()
_MODEL_INSTANCES: dict[str, object] = {}
_MODEL_PATH_BY_VERSION: dict[str, str] = {}
_MODEL_WARMED_VERSIONS: set[str] = set()


def _backend_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _normalize_model_version(model_version: str | None) -> str:
    default_version = str(current_app.config.get("MODEL_VERSION", "v1")).strip()
    if not default_version:
        default_version = "v1"
    if model_version is None:
        return default_version
    text = str(model_version).strip()
    return text or default_version


def _resolve_model_path(model_version: str | None = None) -> Path:
    normalized_model_version = _normalize_model_version(model_version)
    model_path_text = ""
    configured_paths = current_app.config.get("MODEL_VERSION_PATHS", {})
    if isinstance(configured_paths, dict):
        configured_value = configured_paths.get(normalized_model_version)
        if configured_value is not None:
            model_path_text = str(configured_value)
    if not model_path_text:
        model_path_text = str(current_app.config.get("MODEL_PATH", "model/best.pt"))
    model_path = Path(model_path_text)
    if not model_path.is_absolute():
        model_path = _backend_root() / model_path
    if not model_path.exists() and model_version is not None:
        fallback_path_text = str(current_app.config.get("MODEL_PATH", "model/best.pt"))
        fallback_model_path = Path(fallback_path_text)
        if not fallback_model_path.is_absolute():
            fallback_model_path = _backend_root() / fallback_model_path
        if fallback_model_path.exists():
            return fallback_model_path
    return model_path


def _resolve_label(names, cls_idx: int) -> str:
    if isinstance(names, dict):
        return str(names.get(cls_idx, ""))
    if isinstance(names, (list, tuple)) and 0 <= cls_idx < len(names):
        return str(names[cls_idx])
    return ""


def _build_detection_payload(
    label: str,
    confidence: float,
    xyxy: list[float],
    frame_index: int | None = None,
    fps: float = 25.0,
) -> dict:
    payload = {
        "label": label,
        "confidence": confidence,
        "x_min": float(xyxy[0]),
        "y_min": float(xyxy[1]),
        "x_max": float(xyxy[2]),
        "y_max": float(xyxy[3]),
        "extra_json": {},
    }
    if frame_index is not None:
        payload["frame_index"] = frame_index
        payload["timestamp_sec"] = float(frame_index / fps)
    return payload


def _load_ultralytics_model(model_path: Path) -> object:
    try:
        from ultralytics import YOLO  # type: ignore[attr-defined]
    except Exception as exc:  # pragma: no cover
        raise BusinessError(f"ultralytics is unavailable: {exc}", status_code=500)

    try:
        return YOLO(model_path.as_posix())
    except Exception as exc:
        raise BusinessError(f"ultralytics model load failed: {exc}", status_code=500)


def _get_shared_model(model_version: str | None = None) -> object:
    normalized_model_version = _normalize_model_version(model_version)
    model_path = _resolve_model_path(normalized_model_version)
    if not model_path.exists():
        raise BusinessError("model file not found", status_code=500)

    model_path_text = model_path.as_posix()
    with _MODEL_LOCK:
        existing_model = _MODEL_INSTANCES.get(normalized_model_version)
        existing_path = _MODEL_PATH_BY_VERSION.get(normalized_model_version)
        if existing_model is None or existing_path != model_path_text:
            _MODEL_INSTANCES[normalized_model_version] = _load_ultralytics_model(
                model_path
            )
            _MODEL_PATH_BY_VERSION[normalized_model_version] = model_path_text
            _MODEL_WARMED_VERSIONS.discard(normalized_model_version)
        return _MODEL_INSTANCES[normalized_model_version]


def _warmup_real_model(model_version: str | None = None):
    normalized_model_version = _normalize_model_version(model_version)
    if normalized_model_version in _MODEL_WARMED_VERSIONS:
        return

    with _WARMUP_LOCK:
        if normalized_model_version in _MODEL_WARMED_VERSIONS:
            return

        try:
            import numpy as np
        except Exception as exc:
            raise BusinessError(f"warmup dependency unavailable: {exc}", status_code=500)

        model = cast(Any, _get_shared_model(normalized_model_version))
        warmup_frame = np.zeros((64, 64, 3), dtype=np.uint8)
        try:
            model.predict(source=warmup_frame, conf=0.01, verbose=False)
        except Exception as exc:
            raise BusinessError(f"model warmup failed: {exc}", status_code=500)

        _MODEL_WARMED_VERSIONS.add(normalized_model_version)


def warmup_model(model_version: str | None = None):
    if bool(current_app.config.get("RECOGNITION_USE_FAKE_MODEL", False)):
        return

    try:
        _warmup_real_model(model_version)
    except Exception as exc:
        current_app.logger.warning("Skip model warmup: %s", exc)


def infer_stream_frame(frame, model_version: str | None = None) -> list[dict]:
    if bool(current_app.config.get("RECOGNITION_USE_FAKE_MODEL", False)):
        return _fake_image_detections()

    _warmup_real_model(model_version)
    confidence_threshold = float(current_app.config.get("MODEL_CONF_THRESHOLD", 0.25))
    allowed_labels = get_label_set()
    model = cast(Any, _get_shared_model(model_version))

    try:
        results = model.predict(source=frame, conf=confidence_threshold, verbose=False)
    except Exception as exc:
        raise BusinessError(f"ultralytics inference failed: {exc}", status_code=500)

    output: list[dict] = []
    for result in results:
        _append_ultralytics_result_to_output(
            output,
            result,
            allowed_labels,
            confidence_threshold,
        )
    return output


def _append_ultralytics_result_to_output(
    output: list[dict],
    result,
    allowed_labels: set[str],
    confidence_threshold: float,
    frame_index: int | None = None,
    fps: float = 25.0,
):
    if result.boxes is None:
        return

    names = result.names
    for box in result.boxes:
        cls_idx = int(box.cls.item())
        label = _resolve_label(names, cls_idx)
        confidence = float(box.conf.item())
        if label not in allowed_labels or confidence < confidence_threshold:
            continue
        xyxy = box.xyxy[0].tolist()
        output.append(
            _build_detection_payload(
                label=label,
                confidence=confidence,
                xyxy=xyxy,
                frame_index=frame_index,
                fps=fps,
            )
        )


def _fake_image_detections() -> list[dict]:
    return [
        {
            "label": "Tank",
            "confidence": 0.9,
            "x_min": 10.0,
            "y_min": 10.0,
            "x_max": 120.0,
            "y_max": 120.0,
            "extra_json": {},
        }
    ]


def _fake_video_detections() -> list[dict]:
    return [
        {
            "frame_index": 0,
            "timestamp_sec": 0.0,
            "label": "drone",
            "confidence": 0.88,
            "x_min": 8.0,
            "y_min": 8.0,
            "x_max": 80.0,
            "y_max": 80.0,
            "extra_json": {},
        }
    ]


def infer_image(file_path: str) -> list[dict]:
    if bool(current_app.config.get("RECOGNITION_USE_FAKE_MODEL", False)):
        return _fake_image_detections()

    _warmup_real_model()
    confidence_threshold = float(current_app.config.get("MODEL_CONF_THRESHOLD", 0.25))
    allowed_labels = get_label_set()
    model = cast(Any, _get_shared_model())

    try:
        results = model.predict(
            source=file_path, conf=confidence_threshold, verbose=False
        )
    except Exception as exc:
        raise BusinessError(f"ultralytics inference failed: {exc}", status_code=500)

    output: list[dict] = []
    for result in results:
        _append_ultralytics_result_to_output(
            output,
            result,
            allowed_labels,
            confidence_threshold,
        )
    return output


def infer_image_bytes(
    file_bytes: bytes, target_width: int = 640
) -> tuple[list[dict], int, int]:
    try:
        import cv2
        import numpy as np
    except Exception as exc:  # pragma: no cover
        raise BusinessError(f"inference dependency unavailable: {exc}", status_code=500)

    np_array = np.frombuffer(file_bytes, dtype=np.uint8)
    frame = cv2.imdecode(np_array, cv2.IMREAD_COLOR)
    if frame is None:
        raise BusinessError("failed to decode image", status_code=400)

    height, width = frame.shape[:2]
    if target_width > 0 and width > target_width:
        target_height = max(1, int(round(height * float(target_width) / float(width))))
        frame = cv2.resize(
            frame, (int(target_width), target_height), interpolation=cv2.INTER_AREA
        )

    frame_height, frame_width = frame.shape[:2]

    if bool(current_app.config.get("RECOGNITION_USE_FAKE_MODEL", False)):
        return _fake_image_detections(), frame_width, frame_height

    _warmup_real_model()
    confidence_threshold = float(current_app.config.get("MODEL_CONF_THRESHOLD", 0.25))
    allowed_labels = get_label_set()
    model = cast(Any, _get_shared_model())

    try:
        results = model.predict(
            source=frame,
            conf=confidence_threshold,
            verbose=False,
        )
    except Exception as exc:
        raise BusinessError(f"ultralytics inference failed: {exc}", status_code=500)

    output: list[dict] = []
    for result in results:
        _append_ultralytics_result_to_output(
            output,
            result,
            allowed_labels,
            confidence_threshold,
        )
    return output, frame_width, frame_height


def infer_video(file_path: str) -> list[dict]:
    if bool(current_app.config.get("RECOGNITION_USE_FAKE_MODEL", False)):
        return _fake_video_detections()

    _warmup_real_model()

    try:
        import cv2
    except Exception as exc:  # pragma: no cover
        raise BusinessError(f"inference dependency unavailable: {exc}", status_code=500)

    confidence_threshold = float(current_app.config.get("MODEL_CONF_THRESHOLD", 0.25))
    frame_interval_sec = float(
        current_app.config.get("VIDEO_INFER_FRAME_INTERVAL", 0.2)
    )
    allowed_labels = get_label_set()
    model = cast(Any, _get_shared_model())

    capture = cv2.VideoCapture(file_path)
    if not capture.isOpened():
        raise BusinessError("failed to open video", status_code=400)

    fps = capture.get(cv2.CAP_PROP_FPS) or 25.0
    frame_step = int(round(fps * frame_interval_sec))
    frame_step = max(1, frame_step)
    frame_index = 0
    output: list[dict] = []
    try:
        while True:
            success, frame = capture.read()
            if not success:
                break
            if frame_index % frame_step != 0:
                frame_index += 1
                continue

            try:
                results = model.predict(
                    source=frame,
                    conf=confidence_threshold,
                    verbose=False,
                )
            except Exception as exc:
                raise BusinessError(
                    f"ultralytics inference failed: {exc}",
                    status_code=500,
                )

            for result in results:
                _append_ultralytics_result_to_output(
                    output,
                    result,
                    allowed_labels,
                    confidence_threshold,
                    frame_index=frame_index,
                    fps=fps,
                )
            frame_index += 1
    finally:
        capture.release()
    return output
