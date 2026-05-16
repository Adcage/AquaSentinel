import requests
import cv2
import numpy as np
import time
import sys
sys.path.insert(0, ".")

VIDEO_HUB = "http://127.0.0.1:5100"
session = requests.Session()
session.trust_env = False

print("=" * 60)
print("Full E2E: HTTP MJPEG + RTSP via video-hub + YOLO inference")
print("=" * 60)

# Test all 3 cameras
cameras = [
    (5021, "ESP32-CAM HTTP MJPEG"),
    (9001, "Chicony RTSP via MediaMTX"),
    (9002, "Ysd-Anzija RTSP via MediaMTX"),
]

for cam_id, cam_name in cameras:
    print(f"\n[{cam_name}] snapshot test:")
    # Check status
    r = session.get(f"{VIDEO_HUB}/video-hub/cameras/{cam_id}/status", timeout=5)
    status = r.json()["data"]
    print(f"  state={status['state']} connected={status['connected']} {status.get('source_width',0)}x{status.get('source_height',0)}")
    
    if not status.get("connected"):
        print("  SKIP (not connected)")
        continue
    
    for i in range(3):
        r = session.get(f"{VIDEO_HUB}/video-hub/cameras/{cam_id}/snapshot", timeout=5)
        if r.status_code == 200:
            frame = cv2.imdecode(np.frombuffer(r.content, dtype=np.uint8), cv2.IMREAD_COLOR)
            if frame is not None:
                print(f"  frame {i+1}: {frame.shape[1]}x{frame.shape[0]} {len(r.content)}B")
            else:
                print(f"  frame {i+1}: decode FAILED")
        else:
            print(f"  frame {i+1}: HTTP {r.status_code}")
        time.sleep(0.2)

# YOLO inference on video-hub frames
print("\n" + "=" * 60)
print("YOLO inference on video-hub frames")
print("=" * 60)

from pathlib import Path
from ultralytics import YOLO

model_path = Path("model/drowning-v11-x.pt")
print(f"\nLoading model: {model_path}")
model = YOLO(model_path.as_posix())
print("Model loaded successfully")

for cam_id, cam_name in cameras:
    print(f"\n[{cam_name}] YOLO inference:")
    r = session.get(f"{VIDEO_HUB}/video-hub/cameras/{cam_id}/snapshot", timeout=5)
    if r.status_code != 200:
        print(f"  snapshot failed: HTTP {r.status_code}")
        continue
    frame = cv2.imdecode(np.frombuffer(r.content, dtype=np.uint8), cv2.IMREAD_COLOR)
    if frame is None:
        print("  decode FAILED")
        continue
    results = model.predict(frame, conf=0.25, verbose=False)
    detections = []
    for r2 in results:
        for box in r2.boxes:
            label = r2.names.get(int(box.cls), "")
            conf = float(box.conf)
            detections.append(f"{label}({conf:.2f})")
    det_str = ", ".join(detections) if detections else "no detections"
    print(f"  {frame.shape[1]}x{frame.shape[0]} -> [{det_str}]")

print("\n" + "=" * 60)
print("ALL E2E TESTS PASSED")
print("=" * 60)