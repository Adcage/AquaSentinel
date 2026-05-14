from __future__ import annotations

import logging
import os
import time
from typing import Any

import cv2
import numpy as np
from flask import current_app
from PIL import Image, ImageDraw, ImageFont

logger = logging.getLogger(__name__)


def _get_chinese_font() -> ImageFont.FreeTypeFont | None:
    """尝试找到系统中可用的中文字体"""
    font_paths = [
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",  # Linux
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/System/Library/Fonts/PingFang.ttc",  # macOS
        "/System/Library/Fonts/STHeiti Light.ttc",
        "C:/Windows/Fonts/simhei.ttf",  # Windows
        "C:/Windows/Fonts/simsun.ttc",
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/msyhbd.ttc",
    ]
    for font_path in font_paths:
        if os.path.exists(font_path):
            try:
                return ImageFont.truetype(font_path, 20)
            except Exception:
                continue
    return None


_COLOR_DROWNING = (0, 0, 255)
_COLOR_SWIMMING = (0, 255, 0)
_COLOR_PERSON = (255, 255, 0)
_COLOR_DEFAULT = (0, 255, 255)

_LABEL_COLOR_MAP: dict[str, tuple[int, int, int]] = {
    "drowning": _COLOR_DROWNING,
    "drown": _COLOR_DROWNING,
    "swimming": _COLOR_SWIMMING,
    "swimmer": _COLOR_SWIMMING,
    "person": _COLOR_PERSON,
    "human": _COLOR_PERSON,
}


def _load_label_translations() -> dict[str, str]:
    import json
    import os

    base_dir = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
    labels_path = os.path.join(base_dir, "model", "labels.json")
    try:
        with open(labels_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            return {
                item["en"].strip().lower(): item["zh"]
                for item in data.get("labels", [])
                if "en" in item and "zh" in item
            }
    except Exception:
        return {
            "drowning": "溺水",
            "drown": "溺水",
            "swimming": "游泳",
            "swimmer": "游泳者",
            "person": "人员",
            "human": "人员",
            "out of water": "离水",
            "not swimming": "未游泳",
        }


_LABEL_ZH_MAP: dict[str, str] = _load_label_translations()


def _env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _env_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None:
        return default
    try:
        return int(raw.strip())
    except ValueError:
        return default


def _get_color_for_detection(
    detection: dict[str, Any], label: str
) -> tuple[int, int, int]:
    normalized = str(label or "").strip().lower()
    if "drown" in normalized:
        confidence = float(detection.get("confidence", 0))
        risk_level = str(detection.get("risk_level", "")).upper()
        triggered = bool(detection.get("triggered", False))
        if triggered or risk_level == "HIGH" or confidence >= 0.8:
            return _COLOR_DROWNING
        elif risk_level == "MEDIUM" or confidence >= 0.5:
            return (0, 127, 255)
        else:
            return (0, 255, 255)
    for key, color in _LABEL_COLOR_MAP.items():
        if key in normalized:
            return color
    return _COLOR_DEFAULT


def _draw_detection_box(
    frame: np.ndarray,
    detection: dict[str, Any],
    label: str,
    orig_height: int,
    orig_width: int,
) -> np.ndarray:
    bbox = detection.get("bbox", {})
    x_min = int(bbox.get("x_min", 0))
    y_min = int(bbox.get("y_min", 0))
    x_max = int(bbox.get("x_max", 0))
    y_max = int(bbox.get("y_max", 0))

    height, width = frame.shape[:2]

    scale_x = width / orig_width if orig_width > 0 else 1
    scale_y = height / orig_height if orig_height > 0 else 1

    x_min = int(x_min * scale_x)
    y_min = int(y_min * scale_y)
    x_max = int(x_max * scale_x)
    y_max = int(y_max * scale_y)

    x_min = max(0, min(x_min, width - 1))
    y_min = max(0, min(y_min, height - 1))
    x_max = max(0, min(x_max, width - 1))
    y_max = max(0, min(y_max, height - 1))

    color = _get_color_for_detection(detection, label)
    thickness = 2

    cv2.rectangle(frame, (x_min, y_min), (x_max, y_max), color, thickness)

    normalized_label = str(label or "").strip().lower()
    zh_label = _LABEL_ZH_MAP.get(normalized_label, label)
    label_text = f"{zh_label} {int(detection.get('confidence', 0) * 100)}%"

    font = cv2.FONT_HERSHEY_SIMPLEX
    font_scale = 0.8
    text_size = cv2.getTextSize(label_text, font, font_scale, thickness)[0]

    text_bg_x1 = x_min
    text_bg_y1 = max(y_min - text_size[1] - 8, 0)
    text_bg_x2 = x_min + text_size[0] + 8
    text_bg_y2 = y_min

    cv2.rectangle(
        frame,
        (text_bg_x1, text_bg_y1),
        (text_bg_x2, text_bg_y2),
        color,
        -1,
    )

    pil_font = _get_chinese_font()
    if pil_font is not None:
        pil_img = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
        draw = ImageDraw.Draw(pil_img)
        draw.text(
            (x_min + 4, text_bg_y1 + 2),
            label_text,
            font=pil_font,
            fill=(255, 255, 255),
        )
        frame[:] = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
    else:
        cv2.putText(
            frame,
            label_text,
            (x_min + 4, y_min - 4),
            font,
            font_scale,
            (255, 255, 255),
            1,
        )

    return frame


def draw_detections_on_frame(
    frame: np.ndarray,
    detections: list[dict[str, Any]],
    resize_width: int = 960,
) -> np.ndarray:
    if frame is None or frame.size == 0:
        return frame

    result_frame = frame.copy()

    orig_height, orig_width = result_frame.shape[:2]
    bbox_ref_width = orig_width
    bbox_ref_height = orig_height

    if resize_width > 0 and orig_width > resize_width:
        scale = resize_width / orig_width
        new_width = resize_width
        new_height = int(orig_height * scale)
        result_frame = cv2.resize(result_frame, (new_width, new_height))
        orig_height, orig_width = new_height, new_width

    for detection in detections:
        label = detection.get("label", "unknown")
        result_frame = _draw_detection_box(
            result_frame, detection, label, bbox_ref_height, bbox_ref_width
        )

    return result_frame


def encode_frame_to_jpeg(
    frame: np.ndarray,
    quality: int = 85,
) -> bytes | None:
    if frame is None or frame.size == 0:
        return None

    try:
        encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), quality]
        _, jpeg_bytes = cv2.imencode(".jpg", frame, encode_param)
        return jpeg_bytes.tobytes()
    except Exception as e:
        logger.warning("encode frame to jpeg failed: %s", e)
        return None


class VideoFramePushService:
    def __init__(self) -> None:
        self._enabled = True
        self._fps = 8
        self._resize_width = 960
        self._quality = 85
        self._last_push_time_map: dict[int, float] = {}
        self._min_interval = 1.0 / self._fps

    def configure(
        self,
        enabled: bool = True,
        fps: int = 8,
        resize_width: int = 960,
        quality: int = 85,
    ) -> None:
        self._enabled = enabled
        self._fps = max(1, min(fps, 30))
        self._resize_width = resize_width
        self._quality = max(1, min(quality, 100))
        self._min_interval = 1.0 / self._fps
        self._last_push_time_map.clear()
        logger.info(
            "video frame push configured: enabled=%s fps=%s width=%s quality=%s",
            enabled,
            self._fps,
            self._resize_width,
            self._quality,
        )

    def should_push(self, camera_id: int) -> bool:
        if not self._enabled:
            return False
        now = time.monotonic()
        last = self._last_push_time_map.get(camera_id, 0.0)
        if now - last < self._min_interval:
            return False
        self._last_push_time_map[camera_id] = now
        return True

    def push_frame(
        self,
        ws_service,
        camera_id: int,
        frame: np.ndarray,
        detections: list[dict[str, Any]],
    ) -> bool:
        if not self._enabled:
            return False

        if not ws_service.is_connected():
            return False

        try:
            if logger.isEnabledFor(logging.DEBUG):
                logger.debug(
                    "push_frame: camera_id=%d, frame shape=%s, detections=%d",
                    camera_id,
                    frame.shape if frame is not None else None,
                    len(detections),
                )

            if current_app.config.get("OVERLAY_SERVER_SIDE_ENABLED", False):
                drawn_frame = draw_detections_on_frame(
                    frame,
                    detections,
                    resize_width=self._resize_width,
                )
                jpeg_bytes = encode_frame_to_jpeg(drawn_frame, quality=self._quality)
            else:
                jpeg_bytes = encode_frame_to_jpeg(frame, quality=self._quality)
            if jpeg_bytes is None:
                return False
            if logger.isEnabledFor(logging.DEBUG):
                logger.debug("push_frame: jpeg size=%d", len(jpeg_bytes))

            frame_ts = int(time.time() * 1000)

            header_payload = {
                "type": "VIDEO_FRAME",
                "cameraId": camera_id,
                "frameTs": frame_ts,
                "headCount": len(detections),
                "detections": detections,
            }
            return ws_service.push_video_frame(header_payload, jpeg_bytes)
        except Exception as e:
            logger.warning("push video frame failed: %s", e)
            return False


video_frame_push_service = VideoFramePushService()
video_frame_push_service.configure(
    enabled=_env_bool("VIDEO_FRAME_PUSH_ENABLED", True),
    fps=_env_int("VIDEO_FRAME_PUSH_FPS", 8),
    resize_width=_env_int("VIDEO_FRAME_PUSH_RESIZE_WIDTH", 1280),
    quality=_env_int("VIDEO_FRAME_PUSH_JPEG_QUALITY", 90),
)
