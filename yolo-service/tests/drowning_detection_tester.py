import argparse
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import cv2
from app import create_app
from app.services.drowning_rule_service import DrowningRuleEvaluator
from app.services.model_inference_service import infer_stream_frame, warmup_model
from app.services.tracker_service import DeepSortTracker, TrackedObject


_COLOR_NORMAL = (0, 255, 0)
_COLOR_DROWNING = (0, 0, 255)
_COLOR_SUSPICIOUS = (0, 165, 255)

_LABEL_DROWNING = {
    "triggered": False,
    "posture_score": 0.0,
    "thermal_score": 0.0,
    "duration_sec": 0.0,
    "posture_abnormal": False,
    "thermal_abnormal": False,
    "duration_abnormal": False,
    "risk_level": "LOW",
    "risk_score": 0.0,
}


def _resolve_color(is_drowning: bool, risk_level: str) -> tuple[int, int, int]:
    if is_drowning:
        return _COLOR_DROWNING
    if risk_level == "MEDIUM":
        return _COLOR_SUSPICIOUS
    return _COLOR_NORMAL


def _resolve_label_text(
    track: TrackedObject,
    extra_info: dict,
) -> str:
    label = track.label or "person"
    track_id = track.track_id or "?"
    conf = track.confidence or 0.0
    posture = extra_info.get("posture_score", 0.0)
    thermal = extra_info.get("thermal_score", 0.0)
    risk = extra_info.get("risk_level", "LOW")
    duration = extra_info.get("duration_sec", 0.0)
    triggered = extra_info.get("triggered", False)

    status = "DROWNING!" if triggered else f"{risk}"
    return f"#{track_id} {label} {status} conf:{conf:.2f} p:{posture:.2f} t:{thermal:.2f} dur:{duration:.1f}s"


def _draw_detection(
    frame,
    track: TrackedObject,
    extra_info: dict,
    scale: float = 1.0,
):
    x_min = int(track.x_min * scale)
    y_min = int(track.y_min * scale)
    x_max = int(track.x_max * scale)
    y_max = int(track.y_max * scale)

    is_drowning = extra_info.get("triggered", False)
    risk_level = extra_info.get("risk_level", "LOW")
    color = _resolve_color(is_drowning, risk_level)

    cv2.rectangle(frame, (x_min, y_min), (x_max, y_max), color, 2)

    label_text = _resolve_label_text(track, extra_info)
    font = cv2.FONT_HERSHEY_SIMPLEX
    font_scale = max(0.3, min(0.6, 0.5 * scale))
    thickness = max(1, int(1 * scale))
    (text_w, text_h), baseline = cv2.getTextSize(
        label_text, font, font_scale, thickness
    )
    text_x = x_min
    text_y = y_min - 10 if y_min - 10 > text_h else y_max + text_h + 10

    bg_x_min = text_x
    bg_y_min = text_y - text_h - 2
    bg_x_max = text_x + text_w + 4
    bg_y_max = text_y + 2

    cv2.rectangle(
        frame,
        (bg_x_min, bg_y_min),
        (bg_x_max, bg_y_max),
        color,
        -1,
    )
    cv2.putText(
        frame,
        label_text,
        (text_x + 2, text_y - 2),
        font,
        font_scale,
        (255, 255, 255),
        thickness,
    )


def _is_drowning_label(label: str) -> bool:
    normalized = str(label or "").strip().lower()
    return (
        normalized == "drowning"
        or normalized == "drown"
        or "drown" in normalized
        or "溺" in normalized
    )


def _calc_risk_score(decision) -> float:
    score = (
        decision.posture_score * 0.35
        + decision.thermal_score * 0.35
        + min(max(decision.duration_sec / 5.0, 0.0), 1.0) * 0.3
    )
    return min(max(score, 0.0), 1.0)


def _resolve_risk_level(decision, risk_score: float) -> str:
    if decision.triggered or risk_score >= 0.85:
        return "HIGH"
    if risk_score >= 0.6:
        return "MEDIUM"
    return "LOW"


def run_detection(
    source: str,
    frame_interval: float = 0.2,
    use_fake_model: bool = False,
):
    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
            "RECOGNITION_USE_FAKE_MODEL": use_fake_model,
        }
    )

    try:
        int(source)
        is_camera = True
    except ValueError:
        is_camera = False

    if is_camera:
        stream_url = source
    else:
        stream_url = source

    os_environ_backup = None
    if not is_camera and stream_url.lower().startswith("http"):
        os_environ_backup = os.environ.get("OPENCV_FFMPEG_CAPTURE_OPTIONS")
        os.environ["OPENCV_FFMPEG_CAPTURE_OPTIONS"] = (
            "timeout;10000000|reconnect;1|reconnect_streamed;1|reconnect_delay_max;5"
        )
    elif not is_camera:
        os_environ_backup = os.environ.get("OPENCV_FFMPEG_CAPTURE_OPTIONS")
        os.environ["OPENCV_FFMPEG_CAPTURE_OPTIONS"] = (
            "rtsp_transport;tcp|stimeout;10000000|max_delay;500000|analyzeduration;2000000|reconnect;1|reconnect_streamed;1|reconnect_delay_max;5"
        )

    capture = None
    try:
        with app.app_context():
            warmup_model()

            if is_camera:
                capture = cv2.VideoCapture(int(stream_url))
            else:
                capture = cv2.VideoCapture(stream_url)

            if not capture.isOpened():
                print(f"[ERROR] Failed to open source: {source}")
                return

            tracker = DeepSortTracker(
                iou_threshold=0.3,
                max_age_sec=1.5,
            )
            evaluator = DrowningRuleEvaluator(
                min_duration_sec=3.0,
                posture_threshold=0.7,
                thermal_threshold=0.85,
                cooldown_sec=15.0,
            )

            last_infer_at = 0.0
            frame_count = 0

            video_fps = capture.get(cv2.CAP_PROP_FPS)
            if video_fps <= 0 or video_fps > 120:
                video_fps = 25.0
            frame_duration_ms = int(1000 / video_fps)

            ret_flag, first_frame = capture.read()
            if not ret_flag:
                print("[ERROR] Failed to read first frame")
                capture.release()
                return
            capture.set(cv2.CAP_PROP_POS_FRAMES, 0)

            orig_height, orig_width = first_frame.shape[:2]
            max_display_width = 1280
            display_scale = (
                min(1.0, max_display_width / orig_width)
                if orig_width > max_display_width
                else 1.0
            )
            disp_width = int(orig_width * display_scale)
            disp_height = int(orig_height * display_scale)

            window_name = f"Drowning Detection - {source}"
            cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
            cv2.resizeWindow(window_name, disp_width, disp_height)

            print(f"[INFO] Starting detection on: {source}")
            print(
                f"[INFO] Video: {orig_width}x{orig_height}, FPS: {video_fps:.2f}, Display: {disp_width}x{disp_height}"
            )
            print("[INFO] Press 'Q' or 'ESC' to quit")

            max_infer_width = 640
            last_tracked_objects: list = []

            while True:
                ok, frame = capture.read()
                if not ok:
                    print("[INFO] End of video")
                    break

                now = time.monotonic()
                orig_h, orig_w = frame.shape[:2]

                # 只在间隔达到时进行推理
                if now - last_infer_at >= frame_interval:
                    # 大分辨率先缩小再推理
                    if orig_w > max_infer_width:
                        infer_scale = max_infer_width / orig_w
                        infer_h = int(orig_h * infer_scale)
                        infer_w = max_infer_width
                        infer_frame = cv2.resize(
                            frame, (infer_w, infer_h), interpolation=cv2.INTER_LINEAR
                        )
                    else:
                        infer_scale = 1.0
                        infer_frame = frame

                    detections = infer_stream_frame(infer_frame)

                    # 如果缩小过，检测结果坐标需要放大回原始尺寸
                    if infer_scale < 1.0:
                        for det in detections:
                            det["x_min"] /= infer_scale
                            det["x_max"] /= infer_scale
                            det["y_min"] /= infer_scale
                            det["y_max"] /= infer_scale

                    tracked_objects = tracker.update(
                        detections, frame=frame, timestamp=now
                    )
                    last_tracked_objects = tracked_objects

                    # 溺水专项分析
                    drowning_objects = [
                        obj for obj in tracked_objects if _is_drowning_label(obj.label)
                    ]
                    if drowning_objects:
                        decisions = [
                            evaluator.evaluate(tracked_object, timestamp=now)
                            for tracked_object in drowning_objects
                        ]
                        for decision in decisions:
                            if decision.triggered:
                                print(
                                    f"[ALERT] Drowning detected! Track: {decision.track_id}"
                                )

                    last_infer_at = now
                else:
                    tracked_objects = last_tracked_objects

                # 在原始帧上绘制
                for obj in tracked_objects:
                    extra_info = dict(obj.extra_json or {})
                    _draw_detection(frame, obj, extra_info, 1.0)

                # 缩放显示
                frame_display = cv2.resize(
                    frame,
                    (disp_width, disp_height),
                    interpolation=cv2.INTER_AREA,
                )

                info_font_scale = max(0.4, min(0.8, 0.7 * display_scale))
                info_thickness = max(1, int(display_scale))
                info_y = int(30 * display_scale)
                cv2.putText(
                    frame_display,
                    f"Frame: {frame_count} | Objects: {len(tracked_objects)}",
                    (10, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    info_font_scale,
                    (255, 255, 255),
                    info_thickness + 1,
                )
                cv2.putText(
                    frame_display,
                    f"Frame: {frame_count} | Objects: {len(tracked_objects)}",
                    (10, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    info_font_scale,
                    (0, 255, 0),
                    info_thickness,
                )

                cv2.imshow(window_name, frame_display)
                frame_count += 1

                key = cv2.waitKey(frame_duration_ms) & 0xFF
                if key in (ord("q"), ord("Q"), 27):
                    break

                now = time.monotonic()

                # 每帧都检测
                detections = infer_stream_frame(frame)
                tracked_objects = tracker.update(detections, frame=frame, timestamp=now)

                # 在原始帧上绘制所有检测结果
                for obj in tracked_objects:
                    extra_info = dict(obj.extra_json or {})
                    _draw_detection(frame, obj, extra_info, 1.0)

                # 溺水专项分析（仅用于日志/统计，不影响绘制）
                if now - last_infer_at >= frame_interval:
                    drowning_objects = [
                        obj for obj in tracked_objects if _is_drowning_label(obj.label)
                    ]
                    if drowning_objects:
                        decisions = [
                            evaluator.evaluate(tracked_object, timestamp=now)
                            for tracked_object in drowning_objects
                        ]
                        for i, decision in enumerate(decisions):
                            if decision.triggered:
                                print(
                                    f"[ALERT] Drowning detected! Track: {decision.track_id}, Risk: {decision.posture_score:.2f}"
                                )
                    last_infer_at = now

                # 缩放显示
                frame_display = cv2.resize(
                    frame,
                    (disp_width, disp_height),
                    interpolation=cv2.INTER_AREA,
                )

                info_font_scale = max(0.4, min(0.8, 0.7 * display_scale))
                info_thickness = max(1, int(display_scale))
                info_y = int(30 * display_scale)
                cv2.putText(
                    frame_display,
                    f"Frame: {frame_count} | Objects: {len(tracked_objects)}",
                    (10, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    info_font_scale,
                    (255, 255, 255),
                    info_thickness + 1,
                )
                cv2.putText(
                    frame_display,
                    f"Frame: {frame_count} | Objects: {len(tracked_objects)}",
                    (10, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    info_font_scale,
                    (0, 255, 0),
                    info_thickness,
                )

                cv2.imshow(window_name, frame_display)
                frame_count += 1

                key = cv2.waitKey(frame_duration_ms) & 0xFF
                if key in (ord("q"), ord("Q"), 27):
                    break

    finally:
        if capture is not None:
            capture.release()
        cv2.destroyAllWindows()
        if os_environ_backup is not None:
            os.environ["OPENCV_FFMPEG_CAPTURE_OPTIONS"] = os_environ_backup


def _parse_args():
    parser = argparse.ArgumentParser(
        description="Drowning Detection Tester - Real-time visualization"
    )
    parser.add_argument(
        "source",
        nargs="?",
        default="0",
        help="Video file path or camera index (default: 0)",
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=0.2,
        help="Frame inference interval in seconds (default: 0.2)",
    )
    parser.add_argument(
        "--fake",
        action="store_true",
        help="Use fake model instead of real YOLO model",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = _parse_args()
    run_detection(
        source=args.source,
        frame_interval=args.interval,
        use_fake_model=args.fake,
    )
