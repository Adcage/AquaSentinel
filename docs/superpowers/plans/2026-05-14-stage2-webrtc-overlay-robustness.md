# 阶段二实施计划：WebRTC 输出 + 叠框组件 + 拉流健壮性

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将视频播放从 MJPEG 升级为 WebRTC WHEP，补齐 video_hub 拉流健壮性（状态机/熔断/退避），前端叠框抽为独立组件并补齐元数据契约，Java 后端新增 WHEP 反代。

**Architecture:** aiortc 内嵌 video_hub 同进程输出 WebRTC，VideoStreamTrack 从 frame_cache 取帧；source_worker 引入四态状态机与熔断退避；叠框从 CameraGridCard 抽到 CameraOverlayLayer；Java 后端反代 WHEP 信令保持前端单入口约定。

**Tech Stack:** aiortc / PyAV / Flask / Spring Boot / Vue 3 + TypeScript / Vitest / pytest

**Spec:** `docs/superpowers/specs/2026-05-14-stage2-webrtc-overlay-robustness-design.md`

---

## File Structure

### yolo-service

| 文件 | 操作 | 职责 |
|------|------|------|
| `app/video_hub/frame_cache.py` | Modify | 新增 `last_frame_at` 字段 |
| `app/video_hub/source_worker.py` | Modify | 四态状态机、退避、熔断、无帧超时、stop() |
| `app/video_hub/registry.py` | Modify | 新增 `remove_session()` |
| `app/video_hub/webrtc_session.py` | Create | VideoStreamTrack、WebrtcSessionManager |
| `app/video_hub/webrtc_signaling.py` | Create | WHEP SDP 解析辅助 |
| `app/video_hub/__init__.py` | Modify | 导出 webrtc_session_manager |
| `app/api/video_hub.py` | Modify | 新增 /reconnect、DELETE /session、ensure/status 扩展 |
| `app/api/video_hub_webrtc.py` | Create | POST /whip、DELETE /sessions/{id} |
| `app/api/__init__.py` | Modify | 注册 video_hub_webrtc blueprint |
| `app/core/config.py` | Modify | 新增 OVERLAY_SERVER_SIDE_ENABLED 等配置 |
| `app/services/video_overlay_service.py` | Modify | 默认关闭服务端叠框 |
| `requirements.txt` | Modify | 新增 aiortc、av |
| `tests/test_video_hub_source_worker.py` | Modify | 状态机、退避、熔断、无帧超时测试 |
| `tests/test_video_hub_stream_api.py` | Modify | /reconnect、DELETE /session 测试 |
| `tests/test_video_hub_webrtc.py` | Create | WHEP 信令、VideoStreamTrack 测试 |

### backend

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/com/springboot/controller/VideoHubProxyController.java` | Create | WHEP 信令反代 |
| `src/test/java/com/springboot/controller/VideoHubProxyControllerTest.java` | Create | 反代测试 |

### frontend

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/components/business/CameraOverlayLayer.vue` | Create | 独立叠框组件 |
| `src/types/videoHub.ts` | Create | DetectionFrame 等类型契约 |
| `src/components/business/CameraGridCard.vue` | Modify | 删除内联叠框，接入 CameraOverlayLayer |
| `src/types/business.ts` | Modify | 补充 RealtimeDetection 字段 |
| `.env.development` | Modify | WebRTC 模式配置 |
| `src/tests/cameraOverlayLayer.test.ts` | Create | 叠框缩放、偏移、超时测试 |

---

## Task 1: frame_cache 新增 last_frame_at

**Files:**
- Modify: `yolo-service/app/video_hub/frame_cache.py`
- Modify: `yolo-service/tests/test_video_hub_source_worker.py`

- [ ] **Step 1: 写失败测试**

在 `test_video_hub_source_worker.py` 新增：

```python
def test_frame_cache_records_last_frame_at():
    cache = FrameCache()
    assert cache.last_frame_at() is None
    cache.update(b"\xff\xd8\xff\xe0", 320, 240)
    ts = cache.last_frame_at()
    assert ts is not None
    assert time.time() - ts < 1.0
```

- [ ] **Step 2: 运行测试确认失败**

Run: `pytest tests/test_video_hub_source_worker.py::test_frame_cache_records_last_frame_at -v`
Expected: FAIL (`AttributeError: 'FrameCache' object has no attribute 'last_frame_at'`)

- [ ] **Step 3: 实现**

在 `frame_cache.py` 的 `FrameCache.__init__` 新增 `self._last_frame_at: float | None = None`，`update()` 方法内新增 `self._last_frame_at = time.time()`，新增 `last_frame_at()` 方法返回 `self._last_frame_at`。

- [ ] **Step 4: 运行测试确认通过**

Run: `pytest tests/test_video_hub_source_worker.py::test_frame_cache_records_last_frame_at -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add yolo-service/app/video_hub/frame_cache.py yolo-service/tests/test_video_hub_source_worker.py
git commit -m "feat(yolo): frame_cache 新增 last_frame_at 字段用于无帧超时检测"
```

---

## Task 2: source_worker 四态状态机 + 退避 + 熔断 + 无帧超时 + stop()

**Files:**
- Modify: `yolo-service/app/video_hub/source_worker.py`
- Modify: `yolo-service/tests/test_video_hub_source_worker.py`

- [ ] **Step 1: 写状态机测试**

```python
def test_session_initial_state_is_connecting():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    assert session.state == "CONNECTING"

def test_state_transitions_to_connected_on_success():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    session._transition_to_connected()
    assert session.state == "CONNECTED"

def test_state_transitions_to_stale_on_no_frames():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream", stale_frame_timeout_sec=0.1)
    session._transition_to_connected()
    time.sleep(0.2)
    session._check_stale_frame()
    assert session.state == "STALE"

def test_state_transitions_to_circuit_open_after_10_failures():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    for _ in range(10):
        session._record_failure("ConnectionRefusedError")
    assert session.state == "CIRCUIT_OPEN"
    assert session.consecutive_failures == 10
```

- [ ] **Step 2: 写退避策略测试**

```python
def test_retry_delay_backoff():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    assert session._calc_retry_delay(1) == 1.5
    assert session._calc_retry_delay(3) == 3.0
    assert session._calc_retry_delay(5) == 5.0
    assert session._calc_retry_delay(7) == 10.0
    assert session._calc_retry_delay(10) == 60.0
    assert session._calc_retry_delay(20) == 60.0
```

- [ ] **Step 3: 写 stop() 测试**

```python
def test_session_stop_terminates_loop():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    assert not session._stopped
    session.stop()
    assert session._stopped
```

- [ ] **Step 4: 写熔断激活测试**

```python
def test_circuit_open_activated_by_ensure():
    session = VideoHubSession(camera_id=1, source_url="http://192.168.1.88/stream")
    for _ in range(10):
        session._record_failure("err")
    assert session.state == "CIRCUIT_OPEN"
    session.activate_from_circuit_open()
    assert session.state == "CONNECTING"
    assert session.consecutive_failures == 0
```

- [ ] **Step 5: 运行测试确认失败**

Run: `pytest tests/test_video_hub_source_worker.py -v -k "state or retry or stop or circuit"`
Expected: FAIL

- [ ] **Step 6: 实现 source_worker 改造**

在 `source_worker.py` 中：

1. 新增 `SessionState` 枚举：`CONNECTING, CONNECTED, STALE, CIRCUIT_OPEN`
2. `VideoHubSession.__init__` 新增：`self._state = SessionState.CONNECTING`、`self._consecutive_failures = 0`、`self._last_failure_at = None`、`self._last_failure_detail = None`、`self._circuit_open_reason = None`、`self._stopped = False`、`self._stale_frame_timeout_sec` 参数
3. 新增 `_transition_to_connected()`、`_transition_to_stale()`、`_transition_to_circuit_open(reason)`、`_record_failure(detail)`、`_record_success()`、`activate_from_circuit_open()` 方法
4. 新增 `_calc_retry_delay(consecutive_failures)` 方法实现退避表
5. 新增 `_check_stale_frame()` 方法：CONNECTED 状态下检查 `frame_cache.last_frame_at()`
6. 改造 `_run_loop()`：用状态机驱动，`_consume_stream` 成功时 `_transition_to_connected()`，失败时 `_record_failure()` 并按退避间隔 sleep，CIRCUIT_OPEN 时 60s 探测
7. 新增 `stop()` 方法设置 `_stopped = True`
8. 改造 `status_dict()` 返回扩展字段

- [ ] **Step 7: 运行测试确认通过**

Run: `pytest tests/test_video_hub_source_worker.py -v`
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add yolo-service/app/video_hub/source_worker.py yolo-service/tests/test_video_hub_source_worker.py
git commit -m "feat(yolo): source_worker 四态状态机、退避策略、熔断、无帧超时、stop()"
```

---

## Task 3: registry 新增 remove_session + API 管理接口

**Files:**
- Modify: `yolo-service/app/video_hub/registry.py`
- Modify: `yolo-service/app/api/video_hub.py`
- Modify: `yolo-service/tests/test_video_hub_stream_api.py`

- [ ] **Step 1: 写 remove_session 测试**

在 `test_video_hub_stream_api.py` 新增：

```python
def test_registry_remove_session_stops_and_removes():
    registry = VideoHubRegistry()
    session = registry.ensure_session(1, "http://192.168.1.88/stream")
    assert registry.get_session(1) is not None
    registry.remove_session(1)
    assert registry.get_session(1) is None
    assert session._stopped
```

- [ ] **Step 2: 写 API 管理接口测试**

```python
def test_reconnect_endpoint(client):
    # POST /video-hub/cameras/1/ensure 先建立会话
    # POST /video-hub/cameras/1/reconnect -> 200
    pass

def test_delete_session_endpoint(client):
    # POST /video-hub/cameras/1/ensure 先建立会话
    # DELETE /video-hub/cameras/1/session -> 200
    # GET /video-hub/cameras/1/status -> 404
    pass

def test_status_includes_state_fields(client):
    # GET /video-hub/cameras/1/status 返回 state, consecutive_failures 等
    pass

def test_ensure_activates_circuit_open(client):
    # 让会话进入 CIRCUIT_OPEN
    # POST /video-hub/cameras/1/ensure -> 跳出熔断
    pass
```

- [ ] **Step 3: 运行测试确认失败**

Run: `pytest tests/test_video_hub_stream_api.py -v -k "remove or reconnect or delete_session or state_fields or activates"`
Expected: FAIL

- [ ] **Step 4: 实现 registry.remove_session()**

在 `registry.py` 新增：

```python
def remove_session(self, camera_id: int) -> None:
    session = self._sessions.pop(camera_id, None)
    if session is not None:
        session.stop()
```

- [ ] **Step 5: 实现 API 管理接口**

在 `api/video_hub.py` 新增：

```python
@blp.post("/video-hub/cameras/<int:camera_id>/reconnect")
def reconnect_camera(camera_id):
    session = video_hub_registry.get_session(camera_id)
    if session is None:
        abort(404, message="会话不存在")
    session.activate_from_circuit_open()
    return success_payload({"camera_id": camera_id, "state": session.state})

@blp.delete("/video-hub/cameras/<int:camera_id>/session")
def delete_camera_session(camera_id):
    session = video_hub_registry.get_session(camera_id)
    if session is None:
        abort(404, message="会话不存在")
    video_hub_registry.remove_session(camera_id)
    return success_payload({"camera_id": camera_id})
```

改造 `ensure_camera_session`：如果会话处于 CIRCUIT_OPEN，调用 `activate_from_circuit_open()`。

改造 `camera_session_status`：返回 `session.status_dict()` 扩展字段。

- [ ] **Step 6: 运行测试确认通过**

Run: `pytest tests/test_video_hub_stream_api.py -v`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add yolo-service/app/video_hub/registry.py yolo-service/app/api/video_hub.py yolo-service/tests/test_video_hub_stream_api.py
git commit -m "feat(yolo): video_hub 新增 /reconnect、DELETE /session 管理接口，status 扩展状态字段"
```

---

## Task 4: WebRTC 依赖 + VideoStreamTrack

**Files:**
- Create: `yolo-service/app/video_hub/webrtc_session.py`
- Modify: `yolo-service/requirements.txt`
- Create: `yolo-service/tests/test_video_hub_webrtc.py`

- [ ] **Step 1: 新增依赖**

在 `requirements.txt` 新增：

```
aiortc>=1.5.0
av>=10.0.0
```

- [ ] **Step 2: 写 VideoStreamTrack 测试**

```python
import pytest
from app.video_hub.webrtc_session import VideoStreamTrack
from app.video_hub.frame_cache import FrameCache

def test_video_stream_track_kind():
    cache = FrameCache()
    track = VideoStreamTrack(camera_id=1, frame_cache=cache, target_fps=10)
    assert track.kind == "video"

@pytest.mark.asyncio
async def test_video_stream_track_recv_returns_frame():
    cache = FrameCache()
    # 构造一个最小 JPEG (1x1 白色像素)
    import PIL.Image, io
    img = PIL.Image.new("RGB", (1, 1), (255, 255, 255))
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    jpeg_bytes = buf.getvalue()
    cache.update(jpeg_bytes, 1, 1)

    track = VideoStreamTrack(camera_id=1, frame_cache=cache, target_fps=10)
    frame = await track.recv()
    assert frame is not None
    assert frame.width == 1
    assert frame.height == 1
```

- [ ] **Step 3: 运行测试确认失败**

Run: `pytest tests/test_video_hub_webrtc.py -v`
Expected: FAIL (module not found)

- [ ] **Step 4: 实现 VideoStreamTrack**

创建 `webrtc_session.py`：

```python
from __future__ import annotations

import asyncio
import logging
import time
from typing import TYPE_CHECKING

import av
import numpy as np
from aiortc import MediaStreamTrack
from PIL import Image

if TYPE_CHECKING:
    from app.video_hub.frame_cache import FrameCache

logger = logging.getLogger(__name__)


class VideoStreamTrack(MediaStreamTrack):
    kind = "video"

    def __init__(
        self,
        camera_id: int,
        frame_cache: FrameCache,
        target_fps: float = 10.0,
    ):
        super().__init__()
        self._camera_id = camera_id
        self._frame_cache = frame_cache
        self._target_fps = target_fps
        self._frame_interval = 1.0 / target_fps
        self._last_send_time: float = 0.0
        self._last_frame_bytes: bytes | None = None
        self._last_av_frame: av.VideoFrame | None = None

    async def recv(self) -> av.VideoFrame:
        loop = asyncio.get_event_loop()

        while True:
            now = time.time()
            elapsed = now - self._last_send_time
            wait = self._frame_interval - elapsed
            if wait > 0:
                await asyncio.sleep(wait)

            try:
                snapshot = await loop.run_in_executor(
                    None,
                    self._frame_cache.wait_for_new_frame,
                    2.0,
                    self._frame_cache.latest().get("version", 0) if self._frame_cache.latest() else 0,
                )
            except Exception:
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            jpeg_bytes = snapshot.get("jpeg_bytes")
            if jpeg_bytes is None:
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            try:
                img = Image.open(io.BytesIO(jpeg_bytes)) if False else None  # placeholder
                arr = np.asarray(img)
                frame = av.VideoFrame.from_ndarray(arr, format="rgb24")
            except Exception:
                if self._last_av_frame is not None:
                    self._last_send_time = time.time()
                    return self._last_av_frame
                await asyncio.sleep(self._frame_interval)
                continue

            self._last_frame_bytes = jpeg_bytes
            self._last_av_frame = frame
            self._last_send_time = time.time()
            return frame

    def stop(self):
        super().stop()
```

注意：实际实现中 `Image.open` 需要正确导入 `io`，此处伪代码需在实现时补全。

- [ ] **Step 5: 运行测试确认通过**

Run: `pytest tests/test_video_hub_webrtc.py -v`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add yolo-service/app/video_hub/webrtc_session.py yolo-service/requirements.txt yolo-service/tests/test_video_hub_webrtc.py
git commit -m "feat(yolo): 新增 VideoStreamTrack，从 frame_cache 取帧推 WebRTC"
```

---

## Task 5: WebrtcSessionManager + WHEP 信令端点

**Files:**
- Modify: `yolo-service/app/video_hub/webrtc_session.py`
- Create: `yolo-service/app/video_hub/webrtc_signaling.py`
- Create: `yolo-service/app/api/video_hub_webrtc.py`
- Modify: `yolo-service/app/video_hub/__init__.py`
- Modify: `yolo-service/app/api/__init__.py`
- Modify: `yolo-service/tests/test_video_hub_webrtc.py`

- [ ] **Step 1: 写 WebrtcSessionManager 测试**

```python
def test_webrtc_session_manager_create_and_delete():
    manager = WebrtcSessionManager()
    assert len(manager._sessions) == 0
    # create_whip_session 和 delete_whip_session 的集成测试
    # 需要有效的 SDP offer，可构造最小 offer
```

- [ ] **Step 2: 写 WHEP 端点测试**

```python
def test_whip_endpoint_creates_session(client):
    # POST /video-hub/cameras/1/whip with SDP offer
    # Expect 201 with SDP answer and Location header
    pass

def test_whip_delete_session(client):
    # DELETE /video-hub/sessions/{session_id}
    # Expect 200
    pass
```

- [ ] **Step 3: 运行测试确认失败**

Run: `pytest tests/test_video_hub_webrtc.py -v`
Expected: FAIL

- [ ] **Step 4: 实现 WebrtcSessionManager**

在 `webrtc_session.py` 新增 `WebrtcSessionManager` 类：

```python
import uuid
from aiortc import RTCPeerConnection, RTCSessionDescription

class WebrtcSessionManager:
    def __init__(self, max_sessions_per_camera: int = 10):
        self._sessions: dict[str, RTCPeerConnection] = {}
        self._session_to_camera: dict[str, int] = {}
        self._max_sessions_per_camera = max_sessions_per_camera

    async def create_whip_session(
        self, camera_id: int, sdp_offer: str
    ) -> tuple[str, str]:
        pc = RTCPeerConnection()
        # ... addTrack, setRemoteDescription, createAnswer ...
        session_id = str(uuid.uuid4())
        self._sessions[session_id] = pc
        self._session_to_camera[session_id] = camera_id
        return pc.localDescription.sdp, session_id

    async def delete_whip_session(self, session_id: str) -> None:
        pc = self._sessions.pop(session_id, None)
        if pc is not None:
            await pc.close()
            self._session_to_camera.pop(session_id, None)
```

- [ ] **Step 5: 实现 WHEP 信令端点**

创建 `api/video_hub_webrtc.py`：

```python
from flask import Blueprint, Response, request, abort
from flask_smorest import Api

blp = Blueprint("video_hub_webrtc", __name__, url_prefix="/video-hub")

@blp.post("/cameras/<int:camera_id>/whip")
def whip_offer(camera_id):
    sdp_offer = request.get_data(as_text=True)
    # asyncio.run 或从 app 获取 event loop
    sdp_answer, session_id = await manager.create_whip_session(camera_id, sdp_offer)
    response = Response(sdp_answer, status=201, content_type="application/sdp")
    response.headers["Location"] = f"/video-hub/sessions/{session_id}"
    return response

@blp.delete("/sessions/<string:session_id>")
def delete_whip_session(session_id):
    await manager.delete_whip_session(session_id)
    return success_payload({"session_id": session_id})
```

注意：Flask 同步框架中调用 async 需要通过 `asyncio.run()` 或 `loop.run_until_complete()` 桥接，实现时需处理事件循环。

- [ ] **Step 6: 注册 blueprint**

在 `app/api/__init__.py` 注册 `video_hub_webrtc` blueprint。

在 `app/video_hub/__init__.py` 导出 `webrtc_session_manager = WebrtcSessionManager()`。

- [ ] **Step 7: 运行测试确认通过**

Run: `pytest tests/test_video_hub_webrtc.py -v`
Expected: PASS

- [ ] **Step 8: 提交**

```bash
git add yolo-service/app/video_hub/webrtc_session.py yolo-service/app/video_hub/webrtc_signaling.py yolo-service/app/api/video_hub_webrtc.py yolo-service/app/video_hub/__init__.py yolo-service/app/api/__init__.py yolo-service/tests/test_video_hub_webrtc.py
git commit -m "feat(yolo): 新增 WHEP 信令端点，WebrtcSessionManager 管理 PeerConnection"
```

---

## Task 6: 前端叠框类型契约 + CameraOverlayLayer.vue

**Files:**
- Create: `frontend/src/types/videoHub.ts`
- Create: `frontend/src/components/business/CameraOverlayLayer.vue`
- Modify: `frontend/src/types/business.ts`
- Create: `frontend/src/tests/cameraOverlayLayer.test.ts`

- [ ] **Step 1: 写类型契约**

创建 `videoHub.ts`：

```ts
export interface DetectionFrame {
  cameraId: number;
  frameWidth: number;
  frameHeight: number;
  timestamp: number;
  detections: import("@/types/business").RealtimeDetection[];
  headCount?: number;
  riskPoint?: import("@/types/business").RealtimeRiskPoint;
}
```

补充 `business.ts` 中 `RealtimeDetection` 缺失的 `timestamp` 字段（如果尚未存在）。

- [ ] **Step 2: 写叠框组件测试**

```ts
import { describe, expect, it } from "vitest";
import { computeOverlayStyle, filterVisibleDetections } from "@/components/business/CameraOverlayLayer.vue";

describe("CameraOverlayLayer", () => {
  it("computeOverlayStyle 缩放归一化坐标到显示尺寸", () => {
    const style = computeOverlayStyle(
      { xMin: 0.1, yMin: 0.2, xMax: 0.5, yMax: 0.8 },
      640, 480,
    );
    expect(style.left).toBe("64px");
    expect(style.top).toBe("96px");
    expect(style.width).toBe("256px");
    expect(style.height).toBe("288px");
  });

  it("filterVisibleDetections 丢弃超时检测结果", () => {
    const now = Date.now();
    const detections = [
      { trackId: 1, timestamp: now - 500, label: "person", confidence: 0.9 },
      { trackId: 2, timestamp: now - 3000, label: "person", confidence: 0.8 },
    ];
    const visible = filterVisibleDetections(detections, 2000);
    expect(visible).toHaveLength(1);
    expect(visible[0].trackId).toBe(1);
  });
});
```

- [ ] **Step 3: 运行测试确认失败**

Run: `npx vitest run src/tests/cameraOverlayLayer.test.ts`
Expected: FAIL

- [ ] **Step 4: 实现 CameraOverlayLayer.vue**

创建组件，包含：

1. Props: `detections`, `frameWidth`, `frameHeight`, `displayWidth`, `displayHeight`, `objectFit`, `maxAgeMs`
2. `computeOverlayStyle(bboxNorm, displayWidth, displayHeight)` — 归一化坐标转像素
3. `computeContainOffset(frameW, frameH, displayW, displayH)` — object-fit: contain 黑边偏移
4. `filterVisibleDetections(detections, maxAgeMs)` — 超时丢弃
5. `riskLevelClass(riskLevel)` — 高中低风险颜色
6. `requestAnimationFrame` 驱动 `now` 刷新以触发超时过滤
7. ResizeObserver 获取容器尺寸（由父组件传入 displayWidth/displayHeight）

- [ ] **Step 5: 运行测试确认通过**

Run: `npx vitest run src/tests/cameraOverlayLayer.test.ts`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add frontend/src/types/videoHub.ts frontend/src/components/business/CameraOverlayLayer.vue frontend/src/types/business.ts frontend/src/tests/cameraOverlayLayer.test.ts
git commit -m "feat(frontend): 新增 CameraOverlayLayer 叠框组件与 DetectionFrame 类型契约"
```

---

## Task 7: CameraGridCard.vue 改造接入叠框

**Files:**
- Modify: `frontend/src/components/business/CameraGridCard.vue`

- [ ] **Step 1: 删除内联叠框**

在 `CameraGridCard.vue` 中：
1. 删除 `detection-layer` div 及其内容
2. 删除 `toBoxStyle` 方法
3. 新增 `import CameraOverlayLayer from "./CameraOverlayLayer.vue"`

- [ ] **Step 2: 接入 CameraOverlayLayer**

在视频元素之后、detection-layer 原位置新增：

```vue
<CameraOverlayLayer
  v-if="item.detections.length > 0"
  :detections="item.detections"
  :frame-width="item.frameWidth ?? 0"
  :frame-height="item.frameHeight ?? 0"
  :display-width="videoDisplayWidth"
  :display-height="videoDisplayHeight"
  object-fit="contain"
  :max-age-ms="2000"
  class="overlay-container"
/>
```

新增 `videoDisplayWidth` / `videoDisplayHeight` ref，通过 `ResizeObserver` 监听视频元素尺寸。

- [ ] **Step 3: 补充 CameraGridItem 类型**

在 `business.ts` 的 `CameraGridItem` 新增 `frameWidth?: number`、`frameHeight?: number` 字段。

- [ ] **Step 4: 运行前端测试**

Run: `npx vitest run`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add frontend/src/components/business/CameraGridCard.vue frontend/src/types/business.ts
git commit -m "refactor(frontend): CameraGridCard 叠框抽为 CameraOverlayLayer 独立组件"
```

---

## Task 8: video_overlay_service.py 默认关闭服务端叠框

**Files:**
- Modify: `yolo-service/app/services/video_overlay_service.py`
- Modify: `yolo-service/app/core/config.py`

- [ ] **Step 1: 新增配置项**

在 `config.py` 新增：

```python
OVERLAY_SERVER_SIDE_ENABLED: bool = False
```

- [ ] **Step 2: 改造 push_frame**

在 `video_overlay_service.py` 的 `VideoFramePushService.push_frame()` 中：

```python
if current_app.config.get("OVERLAY_SERVER_SIDE_ENABLED", False):
    frame_with_boxes = draw_detections_on_frame(frame_array, detections)
    jpeg_bytes = encode_frame_to_jpeg(frame_with_boxes)
else:
    jpeg_bytes = encode_frame_to_jpeg(frame_array)
```

- [ ] **Step 3: 提交**

```bash
git add yolo-service/app/services/video_overlay_service.py yolo-service/app/core/config.py
git commit -m "refactor(yolo): 默认关闭服务端叠框，改为前端叠框（OVERLAY_SERVER_SIDE_ENABLED 开关）"
```

---

## Task 9: Java 后端 WHEP 反代

**Files:**
- Create: `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java`
- Create: `backend/src/test/java/com/springboot/controller/VideoHubProxyControllerTest.java`

- [ ] **Step 1: 写反代测试**

```java
@Test
void whipProxyForwardsSdpToYoloService() {
    // Mock RestTemplate, 验证 POST 转发到 yolo-service
    // 验证 Location header rewrite
}

@Test
void whipDeleteForwardsToYoloService() {
    // Mock RestTemplate, 验证 DELETE 转发
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=VideoHubProxyControllerTest`
Expected: FAIL

- [ ] **Step 3: 实现 VideoHubProxyController**

```java
@RestController
@RequestMapping("/video-hub")
public class VideoHubProxyController {

    @Resource
    private AppAiEngineProperties aiEngineProperties;

    @Resource
    private StreamTokenAuthService streamTokenAuthService;

    @PostMapping("/cameras/{cameraId}/whip")
    public ResponseEntity<byte[]> whipOffer(
            @PathVariable Long cameraId,
            @RequestParam Map<String, String> params,
            @RequestBody byte[] sdpOffer) {
        String token = params.get(streamTokenAuthService.resolveTokenParamName());
        streamTokenAuthService.verifyPreviewToken(token);

        String yoloUrl = aiEngineProperties.getBaseUrl()
                + "/video-hub/cameras/" + cameraId + "/whip";
        // RestTemplate POST 转发 SDP offer
        // 返回 201 + SDP answer + rewrite Location header
    }

    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Boolean> deleteWhipSession(@PathVariable String sessionId) {
        String yoloUrl = aiEngineProperties.getBaseUrl()
                + "/video-hub/sessions/" + sessionId;
        // RestTemplate DELETE 转发
        return ResultUtils.success(true);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=VideoHubProxyControllerTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/springboot/controller/VideoHubProxyController.java backend/src/test/java/com/springboot/controller/VideoHubProxyControllerTest.java
git commit -m "feat(backend): 新增 WHEP 信令反代端点，转发 SDP 到 yolo-service"
```

---

## Task 10: 前端 WebRTC 模式配置 + 联调验证

**Files:**
- Modify: `frontend/.env.development`
- Modify: `frontend/.env.production`

- [ ] **Step 1: 配置 WebRTC 模式**

在 `.env.development` 新增/修改：

```
VITE_CAMERA_PREVIEW_MODE=webrtc
VITE_WEBRTC_WHEP_BASE_URL=/api
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_WEBRTC_APPEND_TOKEN_QUERY=true
```

在 `.env.production` 新增/修改：

```
VITE_CAMERA_PREVIEW_MODE=webrtc
VITE_WEBRTC_WHEP_BASE_URL=/api
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_WEBRTC_APPEND_TOKEN_QUERY=true
```

- [ ] **Step 2: 验证 Vite proxy 覆盖 WHEP 路径**

确认 `vite.config.ts` 的 `/api` proxy 能转发 `/api/video-hub/cameras/{id}/whip` 到 Java 后端。当前 proxy 配置已覆盖所有 `/api` 前缀，无需修改。

- [ ] **Step 3: 联调验证**

1. 启动 ESP32-CAM、yolo-service、Java 后端、前端 dev server
2. 打开监控总览页，确认视频通过 WebRTC 播放（非 MJPEG `<img>`）
3. F12 Network 确认请求路径为 `/api/video-hub/cameras/{id}/whip`
4. 确认叠框正常显示、超时丢弃生效
5. 确认 PTZ 控制仍可用
6. 确认 MJPEG 回退可用（切换 `VITE_CAMERA_PREVIEW_MODE=backend_proxy`）

- [ ] **Step 4: 提交**

```bash
git add frontend/.env.development frontend/.env.production
git commit -m "feat(frontend): 开发与生产环境切换 WebRTC WHEP 预览模式"
```

---

## Task 11: 阶段二整体回归测试

- [ ] **Step 1: yolo-service 全量测试**

Run: `pytest tests/ -v`
Expected: ALL PASS

- [ ] **Step 2: backend 全量测试**

Run: `mvn test`
Expected: ALL PASS

- [ ] **Step 3: frontend 全量测试**

Run: `npm test`
Expected: ALL PASS

- [ ] **Step 4: 更新规划文档**

在 `docs/规划文档/esp32-cam-video-hub-staged-plan.md` 中将阶段二相关步骤标记为 `[x]`。

- [ ] **Step 5: 提交**

```bash
git add docs/规划文档/esp32-cam-video-hub-staged-plan.md
git commit -m "docs: 阶段二完成，更新规划文档步骤状态"
```
