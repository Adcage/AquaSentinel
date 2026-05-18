# 阶段二设计：WebRTC 输出 + 叠框组件 + 拉流健壮性

> 对应规划文档 `docs/archive/plans/esp32-cam-video-hub-staged-plan.md` 阶段二（5.7 + 5.8 + WebRTC）

## 1. 目标与范围

### 1.1 目标

在不改变阶段一核心前提（ESP32 单路源、video_hub 唯一拉流、识别框与视频分离传输）的前提下，完成：

1. 视频主播放从 MJPEG 升级为 WebRTC WHEP，端到端延迟 < 200ms
2. video_hub 拉流引入四态状态机、熔断退避、无帧超时检测、管理接口
3. 前端叠框抽为独立组件，识别结果补齐元数据契约，超时丢弃旧框
4. Java 后端新增 WHEP 信令反代，保持"前端只跟 Java 后端交互"约定

### 1.2 不覆盖

1. STUN/TURN 配置（局域网直连，ICE servers 为空）
2. 视频录制、回放、切片
3. 多码流/自适应码率
4. yolo-service 独立部署拆分

### 1.3 核心决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 低延迟方案 | WebRTC WHEP | 延迟最低，WebRtcWhepPlayer.vue 已就绪 |
| WebRTC 实现 | aiortc 内嵌 video_hub | 同进程零 IPC，frame_cache 直接消费 |
| 叠框数据通道 | 沿用 Java 后端转发 | 不暴露 yolo-service，前端架构一致 |
| 叠框坐标格式 | bboxNorm 归一化 + 帧尺寸 | 最灵活，任意显示尺寸缩放都正确 |

---

## 2. 模块 1：video_hub 拉流健壮性（5.7）

### 2.1 四态状态机

`VideoHubSession` 从 `_connected: bool` 升级为显式四态：

```
CONNECTING → CONNECTED → STALE → CIRCUIT_OPEN
     ↑                              │
     └──────── (激活条件) ──────────┘
```

| 状态 | 含义 | 行为 |
|------|------|------|
| `CONNECTING` | 正在建立上游连接 | 退避重连：1.5s → 3s → 5s → 10s |
| `CONNECTED` | 上游已连接，持续收帧 | 正常推帧到 frame_cache，定期检查无帧超时 |
| `STALE` | 已连接但超时无帧 | 主动断开并立即重连（进入 CONNECTING） |
| `CIRCUIT_OPEN` | 连续失败 ≥10 次，熔断暂停 | 60s 低频探测，不推帧 |

激活条件（任一满足即从 CIRCUIT_OPEN 跳出并重连）：

1. 有新 viewer 打开 `/stream` 或 WHEP 连接
2. 收到 `/ensure` 请求
3. `source_url` 变更
4. 收到 `/reconnect` 手动触发

### 2.2 退避策略

| 连续失败次数 | 重连间隔 |
|-------------|---------|
| 1-2 | 1.5s |
| 3-4 | 3s |
| 5-6 | 5s |
| 7-9 | 10s |
| ≥10 | 60s（熔断探测） |

实现方式：`_calc_retry_delay(consecutive_failures) -> float`，参数可配置化。

### 2.3 无帧超时检测

- 参数：`stale_frame_timeout_sec = 5.0`（可配置）
- `frame_cache` 新增 `last_frame_at` 字段，每次 `update()` 时更新
- `_run_loop` 在 `CONNECTED` 状态下每秒检查：若 `now - last_frame_at > stale_frame_timeout_sec`，进入 `STALE`，断开后进入 `CONNECTING` 重连
- 与修改前的区别：修改前只依赖 HTTP read_timeout（10s），无法区分"上游还在但断流"和"上游彻底重启"

### 2.4 熔断原因记录

`status_dict()` 新增字段：

```json
{
  "camera_id": 1001,
  "state": "CIRCUIT_OPEN",
  "connected": false,
  "circuit_open_reason": "连续连接失败10次: ConnectionRefusedError",
  "consecutive_failures": 10,
  "last_failure_at": 1715616000123,
  "last_failure_detail": "HTTPConnectionPool(host='192.168.137.86', port=80): Max retries exceeded",
  "stale_frame_timeout_sec": 5,
  "last_frame_at": 1715615998000,
  "source_url": "http://192.168.137.86/stream",
  "viewer_count": 0,
  "retry_delay_sec": 60
}
```

### 2.5 管理接口

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/video-hub/cameras/{id}/reconnect` | 强制重连：断开现有连接，清零失败计数，立即重连 |
| DELETE | `/video-hub/cameras/{id}/session` | 销毁会话：停止拉流线程，清除缓存，移除会话 |

已有接口增强：

- `POST /video-hub/cameras/{id}/ensure`：CIRCUIT_OPEN 状态下收到 ensure 立即跳出熔断重连
- `GET /video-hub/cameras/{id}/status`：响应体新增 `state`、`circuit_open_reason`、`consecutive_failures` 等字段

### 2.6 会话可停止

`VideoHubSession` 新增 `stop()` 方法，设置 `_stopped = True`，拉流线程退出循环。`registry.remove_session(camera_id)` 调用 `stop()` 并从 dict 中移除。

### 2.7 改动文件

| 文件 | 改什么 |
|------|--------|
| `yolo-service/app/video_hub/source_worker.py` | 四态状态机、退避策略、熔断逻辑、无帧超时、原因记录、stop() |
| `yolo-service/app/video_hub/frame_cache.py` | 新增 `last_frame_at` 字段，update() 时更新 |
| `yolo-service/app/video_hub/registry.py` | 新增 `remove_session()` 方法 |
| `yolo-service/app/api/video_hub.py` | 新增 `/reconnect` 和 `DELETE /session`，status/ensure 扩展 |
| `yolo-service/tests/test_video_hub_source_worker.py` | 状态机、退避、熔断、无帧超时测试 |
| `yolo-service/tests/test_video_hub_stream_api.py` | `/reconnect`、`DELETE /session` 测试 |

---

## 3. 模块 2：WebRTC WHEP 输出层

### 3.1 架构

```
前端 WebRtcWhepPlayer.vue
  → POST /api/video-hub/cameras/{id}/whip  (SDP offer, application/sdp)
  ← 201 Location: /api/video-hub/sessions/{session-id}  (SDP answer)
  → DELETE /api/video-hub/sessions/{session-id}  (挂断)

video_hub 内部:
  WebrtcSessionManager
    └── 每个前端连接 = 1个 aiortc RTCPeerConnection
          └── VideoStreamTrack (自定义 MediaStreamTrack)
                └── 从 frame_cache.wait_for_new_frame() 取帧
                └── JPEG decode → av.VideoFrame → 推给 PeerConnection
```

### 3.2 WebrtcSessionManager

- 管理所有活跃的 WHEP session（`session_id → PeerConnection`）
- `create_whip_session(camera_id, sdp_offer) -> (sdp_answer, session_id)`
  1. 从 registry 获取/ensure 该 camera 的 VideoHubSession
  2. 创建 `aiortc.RTCPeerConnection`，配置 ICE servers 为空
  3. 创建自定义 `VideoStreamTrack(camera_id, frame_cache)`
  4. `pc.addTrack(video_track)`
  5. `await pc.setRemoteDescription(RTCSessionDescription(sdp_offer, "offer"))`
  6. `answer = await pc.createAnswer()`
  7. `await pc.setLocalDescription(answer)`
  8. 生成 `session_id`（UUID4），存入 sessions dict
  9. 返回 `(pc.localDescription.sdp, session_id)`
- `delete_whip_session(session_id)`：close PeerConnection，移除
- 自动清理：PeerConnection 连接状态变 `closed`/`failed` 时自动移除

### 3.3 VideoStreamTrack

```python
class VideoStreamTrack(MediaStreamTrack):
    kind = "video"

    def __init__(self, camera_id, frame_cache, target_fps=10):
        super().__init__()
        self._camera_id = camera_id
        self._frame_cache = frame_cache
        self._target_fps = target_fps
        self._last_frame_bytes = None

    async def recv(self):
        # 帧率控制：按 target_fps 计算等待时间
        # 从 frame_cache.wait_for_new_frame() 取帧
        # JPEG decode → PIL/numpy → av.VideoFrame
        # 超时时发送上一帧（保持连接活跃）
```

- 帧率控制：默认 10fps，避免 ESP32 低帧率源被 WebRTC 过度拉取
- JPEG 解码：`PIL.Image.open(jpeg_bytes)` → `numpy.asarray()` → `av.VideoFrame.from_ndarray(arr, format="rgb24")`
- 超时：frame_cache 等待超时时重发上一帧（保持 WebRTC 连接活跃，避免黑屏）
- 异步桥接：`frame_cache.wait_for_new_frame()` 是同步阻塞调用（基于 threading.Condition），`recv()` 是 async，需通过 `asyncio.get_event_loop().run_in_executor(None, frame_cache.wait_for_new_frame, ...)` 桥接

### 3.4 WHEP 信令端点（yolo-service）

```
POST /video-hub/cameras/{camera_id}/whip
  Content-Type: application/sdp
  Body: SDP offer
  Response: 201 Created
    Content-Type: application/sdp
    Body: SDP answer
    Location: /video-hub/sessions/{session_id}

DELETE /video-hub/sessions/{session_id}
  Response: 200 OK
```

### 3.5 依赖

`requirements.txt` 新增：
- `aiortc>=1.5.0`
- `av>=10.0.0`（PyAV，aiortc 的 VideoFrame 依赖）

### 3.6 改动文件

| 文件 | 改什么 |
|------|--------|
| `yolo-service/app/video_hub/webrtc_session.py` | 新建：VideoStreamTrack、WebrtcSessionManager |
| `yolo-service/app/video_hub/webrtc_signaling.py` | 新建：WHEP SDP 解析/构建辅助函数 |
| `yolo-service/app/api/video_hub_webrtc.py` | 新建：POST /whip、DELETE /sessions/{id} 端点 |
| `yolo-service/app/video_hub/__init__.py` | 导出 WebrtcSessionManager 单例 |
| `yolo-service/requirements.txt` | 新增 aiortc、av |

---

## 4. 模块 3：前端叠框组件与元数据契约（5.8）

### 4.1 CameraOverlayLayer.vue

独立叠框组件，从 CameraGridCard 中抽离。

**Props**：

| Prop | 类型 | 说明 |
|------|------|------|
| `detections` | `RealtimeDetection[]` | 当前检测结果列表 |
| `frameWidth` | `number` | 原始帧宽（像素） |
| `frameHeight` | `number` | 原始帧高（像素） |
| `displayWidth` | `number` | 视频实际显示宽 |
| `displayHeight` | `number` | 视频实际显示高 |
| `objectFit` | `"contain" \| "cover"` | 视频 object-fit 模式，默认 `"contain"` |
| `maxAgeMs` | `number` | 检测结果最大存活时间，默认 2000ms |

**核心逻辑**：

1. **坐标缩放**：`detection.bboxNorm`（0-1 归一化）× `displayWidth/displayHeight` → 像素定位
2. **object-fit 偏移处理**：
   - `contain` 模式：计算视频保持宽高比后的实际渲染区域，叠框坐标加上左/上偏移
   - `cover` 模式：计算裁剪偏移，叠框坐标减去裁剪量
3. **超时丢弃**：`visibleDetections = computed(() => detections.filter(d => now - d.timestamp <= maxAgeMs))`，用 `requestAnimationFrame` 驱动刷新
4. **风险高亮**：`extraJson.riskLevel === 'HIGH'` 红色加粗，`'MEDIUM'` 橙色，其余蓝色

### 4.2 CameraGridCard.vue 改造

- 删除内联叠框 div（`detection-layer`）和 `toBoxStyle` 方法
- 新增 `<CameraOverlayLayer>` 组件，覆盖在视频元素之上
- 用 `ResizeObserver` 获取视频元素实际显示尺寸，传给 overlay
- 叠框层在三种协议（webrtc / ws_jpeg / mjpeg）之上统一覆盖
- 从 WebSocket 推送的 `DetectionFrame` 中提取 `frameWidth/frameHeight/timestamp`，传给 overlay

### 4.3 元数据契约

当前 `_push_realtime_ws()` 已包含 `frameWidth/frameHeight/frameTs`，detection 用 `bboxNorm` 归一化坐标。**不需要改字段格式**。

前端类型补充：

```ts
export interface RealtimeDetection {
  trackId: number;
  label: string;
  confidence: number;
  bboxNorm: { xMin: number; yMin: number; xMax: number; yMax: number };
  extraJson?: { riskLevel?: string; triggered?: boolean; ruleHits?: string[] };
}

export interface DetectionFrame {
  cameraId: number;
  frameWidth: number;
  frameHeight: number;
  timestamp: number;
  detections: RealtimeDetection[];
  headCount?: number;
  riskPoint?: RealtimeRiskPoint;
}
```

### 4.4 video_overlay_service.py 调整

- **主路径**：只推送结构化检测结果（不画框），前端叠框
- **降级路径**：保留 `draw_detections_on_frame` 作为可选调试/降级通道，通过配置开关 `OVERLAY_SERVER_SIDE_ENABLED=false` 控制
- `VideoFramePushService.push_frame()` 默认不再调用 `draw_detections_on_frame`，直接推送原始帧 + 结构化检测结果

### 4.5 改动文件

| 文件 | 改什么 |
|------|--------|
| `frontend/src/components/business/CameraOverlayLayer.vue` | 新建：独立叠框组件 |
| `frontend/src/types/videoHub.ts` | 新建：DetectionFrame 等类型契约 |
| `frontend/src/components/business/CameraGridCard.vue` | 删除内联叠框，接入 CameraOverlayLayer |
| `frontend/src/types/business.ts` | 补充 RealtimeDetection.timestamp、DetectionFrame 类型 |
| `yolo-service/app/services/video_overlay_service.py` | 默认关闭服务端叠框，保留降级开关 |
| `yolo-service/app/core/config.py` | 新增 OVERLAY_SERVER_SIDE_ENABLED 配置 |

---

## 5. 模块 4：Java 后端 WHEP 反代

### 5.1 端点

```
POST /api/video-hub/cameras/{cameraId}/whip
  → 转发到 yolo-service:5000/video-hub/cameras/{cameraId}/whip
  → 透传 Content-Type: application/sdp
  → 返回 201 + SDP answer + Location header

DELETE /api/video-hub/sessions/{sessionId}
  → 转发到 yolo-service:5000/video-hub/sessions/{sessionId}
  → 返回 200
```

### 5.2 鉴权

- WHEP POST 需验证 JWT token（query param `token`，与 `/streams/cameras/{id}/preview` 同策略）
- DELETE 需验证同一 token
- 复用 `StreamTokenAuthService`

### 5.3 实现

- 复用 `AppAiEngineProperties.getBaseUrl()` 获取 yolo-service 地址
- 使用 `RestTemplate` 或 `HttpClient` 转发 SDP 请求
- Location header 中的路径需要 rewrite：yolo-service 返回 `/video-hub/sessions/{id}`，Java 后端需改为 `/api/video-hub/sessions/{id}`

### 5.4 前端配置

```
VITE_WEBRTC_WHEP_BASE_URL=/api
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_CAMERA_PREVIEW_MODE=webrtc
```

### 5.5 改动文件

| 文件 | 改什么 |
|------|--------|
| `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java` | 新建：WHEP 信令反代端点 |
| `backend/src/main/java/com/springboot/config/AppAiEngineProperties.java` | 确认 baseUrl 可复用 |

---

## 6. 整体数据流

```
ESP32-CAM (MJPEG 源)
  │
  ▼
video_hub.VideoHubSession (采集线程, 四态状态机)
  │
  ├─→ frame_cache (最新帧缓存, last_frame_at)
  │     │
  │     ├─→ MJPEG 输出 (GET /stream, 阶段一已有)
  │     ├─→ 快照输出 (GET /snapshot, 阶段一已有)
  │     └─→ WebRTC 输出 (VideoStreamTrack → aiortc PeerConnection)
  │           │
  │           ▼
  │         POST /video-hub/cameras/{id}/whip (WHEP 信令)
  │
  └─→ YOLO 推理 (frame_cache.wait_for_frame)
        │
        ├─→ 结构化检测结果 (bboxNorm + frameWidth/Height/Ts)
        │     │
        │     ▼
        │   AiWsPushService → Java /ws/ai-push → 前端 WebSocket
        │     │
        │     ▼
        │   CameraOverlayLayer.vue (缩放 + 偏移 + 超时丢弃)
        │
        └─→ 服务端叠框 (可选降级, OVERLAY_SERVER_SIDE_ENABLED)

Java 后端
  ├─→ /api/streams/cameras/{id}/preview (MJPEG 反代, 阶段一已有)
  ├─→ /api/video-hub/cameras/{id}/whip (WHEP 反代, 阶段二新增)
  └─→ /ws/ai-push (检测结果转发, 已有)
```

---

## 7. 风险与约束

1. **aiortc 依赖**：aiortc 在 Windows 上安装可能需要 C 编译器（cffi），需提前验证 `pip install aiortc` 在部署环境可用
2. **JPEG 解码性能**：WebRTC 输出需要每帧 JPEG→ndarray→VideoFrame，ESP32 低分辨率下开销可接受，但需监控 CPU 占用
3. **WebRTC 连接数**：每个前端 viewer 占用一个 PeerConnection + VideoStreamTrack，需限制单设备最大 WebRTC 连接数（建议 10）
4. **前端切换**：MJPEG 和 WebRTC 模式通过 `VITE_CAMERA_PREVIEW_MODE` 切换，需确保两种模式可平滑回退
5. **叠框 object-fit 偏移**：contain 模式下的黑边计算需精确，否则框会偏移
