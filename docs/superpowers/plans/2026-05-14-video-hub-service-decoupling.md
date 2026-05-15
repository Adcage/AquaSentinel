# video_hub 独立部署与服务解耦 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前内置在 `yolo-service` 中的 `video_hub` 迁出为独立部署的 `video-hub-service`，并完成 backend、frontend、yolo-service 三侧调用链彻底解耦。

**Architecture:** 在同一仓库中新增 `video-hub-service/`，先迁移现有 `video_hub` 实现与测试，再把 backend 的 MJPEG/WHEP 代理切到新服务，之后把 `yolo-service` 从进程内 `frame_cache` 共享改为远程 `video_hub_client` 取帧，最后移除 yolo 内置 `video_hub` API 与模块注册。frontend 继续只访问 backend，不直连新服务。

**Tech Stack:** Flask / aiortc / requests / Spring Boot / Vue 3 / HTTP API / pytest / Maven

**Spec:** `docs/规划文档/video-hub-service-decoupling-plan.md`

---

## File Structure

### 新增 video-hub-service

| 文件 | 操作 | 职责 |
|------|------|------|
| `video-hub-service/app/__init__.py` | Create | Flask 应用工厂，注册 blueprint |
| `video-hub-service/app/api/video_hub.py` | Create | ensure/status/reconnect/session/snapshot/stream |
| `video-hub-service/app/api/video_hub_webrtc.py` | Create | WHEP `POST /whip`、`DELETE /sessions/{id}` |
| `video-hub-service/app/api/health.py` | Create | 健康检查 |
| `video-hub-service/app/core/config.py` | Create | 独立服务配置 |
| `video-hub-service/app/common/response.py` | Create | 统一响应封装 |
| `video-hub-service/app/common/errors.py` | Create | BusinessError 与错误处理 |
| `video-hub-service/app/video_hub/frame_cache.py` | Create | 最新帧缓存 |
| `video-hub-service/app/video_hub/registry.py` | Create | session registry |
| `video-hub-service/app/video_hub/source_worker.py` | Create | 拉流状态机与熔断退避 |
| `video-hub-service/app/video_hub/webrtc_session.py` | Create | VideoStreamTrack + WebrtcSessionManager |
| `video-hub-service/app/video_hub/webrtc_signaling.py` | Create | Flask 同步/async 桥接 |
| `video-hub-service/app/video_hub/__init__.py` | Create | 单例导出 |
| `video-hub-service/tests/test_video_hub_source_worker.py` | Create | source_worker 回归测试 |
| `video-hub-service/tests/test_video_hub_stream_api.py` | Create | stream API 回归测试 |
| `video-hub-service/tests/test_video_hub_webrtc.py` | Create | WebRTC 回归测试 |
| `video-hub-service/main.py` | Create | 启动入口 |
| `video-hub-service/requirements.txt` | Create | 依赖清单 |

### backend 改造

| 文件 | 操作 | 职责 |
|------|------|------|
| `backend/src/main/java/com/springboot/config/AppVideoHubProperties.java` | Create | video_hub 独立配置 |
| `backend/src/main/resources/application.yml` | Modify | 新增 `app.video-hub.*` 配置 |
| `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java` | Modify | WHEP 转发改走新服务 |
| `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java` | Modify | MJPEG 预览流改走新服务 |
| `backend/src/test/java/com/springboot/controller/VideoHubProxyControllerTest.java` | Modify | 改断言为独立服务地址 |
| `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java` | Modify | 改断言为独立服务地址 |

### yolo-service 改造

| 文件 | 操作 | 职责 |
|------|------|------|
| `yolo-service/app/services/video_hub_client.py` | Create | 远程调用独立 `video-hub-service` |
| `yolo-service/app/core/config.py` | Modify | 新增 `VIDEO_HUB_BASE_URL` / timeout 配置 |
| `yolo-service/app/services/engine_task_service.py` | Modify | 用 remote client 代替进程内 registry/frame_cache |
| `yolo-service/tests/test_video_hub_frame_consumer.py` | Modify | 改为验证远程取帧 client |
| `yolo-service/app/__init__.py` | Modify | 删除 video_hub blueprint 注册 |
| `yolo-service/app/api/video_hub.py` | Delete | 移除旧内置 video_hub API |
| `yolo-service/app/api/video_hub_webrtc.py` | Delete | 移除旧内置 WebRTC API |
| `yolo-service/app/video_hub/*` | Delete | 移除旧内置正式实现 |

### frontend 影响

| 文件 | 操作 | 职责 |
|------|------|------|
| `frontend/.env.development` | Verify | 保持 `/api/video-hub/.../whip` 不变 |
| `frontend/.env.production` | Verify | 保持 `/api/video-hub/.../whip` 不变 |
| `frontend/src/utils/streamPreview.ts` | Verify | 保持只打 backend |
| `frontend/src/components/business/WebRtcWhepPlayer.vue` | Verify | 继续使用现有 Location / DELETE 语义 |

---

## Task 1: 建立独立 video-hub-service 骨架

**Files:**
- Create: `video-hub-service/app/__init__.py`
- Create: `video-hub-service/app/api/health.py`
- Create: `video-hub-service/app/core/config.py`
- Create: `video-hub-service/app/common/response.py`
- Create: `video-hub-service/app/common/errors.py`
- Create: `video-hub-service/main.py`
- Create: `video-hub-service/requirements.txt`

- [ ] **Step 1: 创建独立目录结构**

创建如下目录：

```text
video-hub-service/
  app/
    api/
    common/
    core/
    video_hub/
  tests/
```

- [ ] **Step 2: 写健康检查失败测试**

创建 `video-hub-service/tests/test_health_api.py`：

```python
from app import create_app


def test_health_returns_ok():
    app = create_app()
    client = app.test_client()
    response = client.get("/health")
    assert response.status_code == 200
    payload = response.get_json()
    assert payload["code"] == "OK"
    assert payload["data"]["service"] == "video-hub-service"
```

- [ ] **Step 3: 运行测试确认失败**

Run: `pytest tests/test_health_api.py -v`
Expected: FAIL（`ModuleNotFoundError` 或 `/health` 不存在）

- [ ] **Step 4: 写最小实现**

在 `video-hub-service/app/__init__.py` 实现 `create_app()`，注册 `health` blueprint；在 `app/api/health.py` 实现：

```python
from flask import Blueprint, jsonify

blp = Blueprint("health", __name__)


@blp.get("/health")
def health():
    return jsonify({
        "code": "OK",
        "message": "ok",
        "data": {"service": "video-hub-service"},
    })
```

`main.py` 启动 `create_app()`，`requirements.txt` 至少包含 `Flask`, `Flask-Smorest`, `requests`, `aiortc`, `av`。

- [ ] **Step 5: 运行测试确认通过**

Run: `pytest tests/test_health_api.py -v`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add video-hub-service
```

---

## Task 2: 迁移 video_hub 核心实现与测试到新服务

**Files:**
- Create: `video-hub-service/app/video_hub/frame_cache.py`
- Create: `video-hub-service/app/video_hub/registry.py`
- Create: `video-hub-service/app/video_hub/source_worker.py`
- Create: `video-hub-service/app/video_hub/webrtc_session.py`
- Create: `video-hub-service/app/video_hub/webrtc_signaling.py`
- Create: `video-hub-service/app/video_hub/__init__.py`
- Create: `video-hub-service/tests/test_video_hub_source_worker.py`
- Create: `video-hub-service/tests/test_video_hub_stream_api.py`
- Create: `video-hub-service/tests/test_video_hub_webrtc.py`

- [ ] **Step 1: 复制现有实现与测试到新服务路径**

来源：

```text
yolo-service/app/video_hub/*
yolo-service/tests/test_video_hub_source_worker.py
yolo-service/tests/test_video_hub_stream_api.py
yolo-service/tests/test_video_hub_webrtc.py
```

目标：

```text
video-hub-service/app/video_hub/*
video-hub-service/tests/*
```

- [ ] **Step 2: 修改 import 路径适配新服务**

例如把：

```python
from app.video_hub import video_hub_registry
```

改为新服务内对应 import，确保所有测试都能在新服务目录下独立运行。

- [ ] **Step 3: 创建独立 video_hub API**

创建 `video-hub-service/app/api/video_hub.py` 与 `video-hub-service/app/api/video_hub_webrtc.py`，把当前 yolo-service 中已经验证通过的 API 完整迁入。

- [ ] **Step 4: 运行 source_worker 测试**

Run: `pytest tests/test_video_hub_source_worker.py -v`
Expected: PASS

- [ ] **Step 5: 运行 stream API 测试**

Run: `pytest tests/test_video_hub_stream_api.py -v`
Expected: PASS

- [ ] **Step 6: 运行 WebRTC 测试**

Run: `pytest tests/test_video_hub_webrtc.py -v`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add video-hub-service/app/video_hub video-hub-service/app/api/video_hub.py video-hub-service/app/api/video_hub_webrtc.py video-hub-service/tests
```

---

## Task 3: backend 新增独立 video_hub 配置

**Files:**
- Create: `backend/src/main/java/com/springboot/config/AppVideoHubProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`

- [ ] **Step 1: 写配置绑定测试**

在 `CameraPreviewRouteServiceTest.java` 增加断言：当 `AppVideoHubProperties.baseUrl = "http://127.0.0.1:5100"` 时，生成的 video_hub URI 使用 `5100` 端口而不是 `5000`。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest`
Expected: FAIL（当前仍从 `AppAiEngineProperties` 取地址）

- [ ] **Step 3: 新增配置类与 application.yml 配置**

创建 `AppVideoHubProperties.java`，字段至少包含：

```java
private String baseUrl = "http://127.0.0.1:5100";
private long timeoutMs = 5000;
```

在 `application.yml` 增加：

```yaml
app:
  video-hub:
    base-url: http://127.0.0.1:5100
    timeout-ms: 5000
```

- [ ] **Step 4: 让 CameraPreviewRouteService 改用新配置**

把 `CameraPreviewRouteService` 的构造依赖从 `AppAiEngineProperties` 改为 `AppVideoHubProperties`，只在 video_hub 相关 URI 构造处使用新地址。

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/springboot/config/AppVideoHubProperties.java backend/src/main/resources/application.yml backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java
```

---

## Task 4: backend 的 MJPEG / WHEP 代理切到新服务

**Files:**
- Modify: `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java`
- Modify: `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java`
- Modify: `backend/src/test/java/com/springboot/controller/VideoHubProxyControllerTest.java`
- Modify: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`

- [ ] **Step 1: 写失败测试**

在 `VideoHubProxyControllerTest.java` 把目标地址断言从：

```java
http://127.0.0.1:5000/video-hub/...
```

改为：

```java
http://127.0.0.1:5100/video-hub/...
```

在 `CameraPreviewRouteServiceTest.java` 同步改断言。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -Dtest=VideoHubProxyControllerTest,CameraPreviewRouteServiceTest`
Expected: FAIL

- [ ] **Step 3: 修改代理实现**

1. `VideoHubProxyController` 注入 `AppVideoHubProperties`，不再用 `AppAiEngineProperties.baseUrl`
2. `CameraPreviewRouteService.buildVideoHubStreamUri()` 改用 `AppVideoHubProperties.baseUrl`
3. 保持 frontend 可见入口不变：
   - `/api/video-hub/cameras/{id}/whip`
   - `/api/streams/cameras/{id}/preview`

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -Dtest=VideoHubProxyControllerTest,CameraPreviewRouteServiceTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/springboot/controller/VideoHubProxyController.java backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java backend/src/test/java/com/springboot/controller/VideoHubProxyControllerTest.java backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java
```

---

## Task 5: 新增 yolo-service 远程 video_hub client

**Files:**
- Create: `yolo-service/app/services/video_hub_client.py`
- Modify: `yolo-service/app/core/config.py`
- Test: `yolo-service/tests/test_video_hub_frame_consumer.py`

- [ ] **Step 1: 写 client 测试**

在 `test_video_hub_frame_consumer.py` 新增：

```python
def test_video_hub_client_builds_snapshot_url():
    client = VideoHubClient(base_url="http://127.0.0.1:5100")
    assert client._snapshot_url(1001) == "http://127.0.0.1:5100/video-hub/cameras/1001/snapshot"
```

再新增：

```python
def test_video_hub_client_builds_ensure_url():
    client = VideoHubClient(base_url="http://127.0.0.1:5100")
    assert client._ensure_url(1001) == "http://127.0.0.1:5100/video-hub/cameras/1001/ensure"
```

- [ ] **Step 2: 运行测试确认失败**

Run: `pytest tests/test_video_hub_frame_consumer.py -v`
Expected: FAIL（模块不存在）

- [ ] **Step 3: 写最小 client**

创建 `video_hub_client.py`，实现：

```python
class VideoHubClient:
    def __init__(self, base_url: str, timeout_ms: int = 5000):
        self.base_url = base_url.rstrip("/")
        self.timeout_ms = timeout_ms

    def _ensure_url(self, camera_id: int) -> str: ...
    def _snapshot_url(self, camera_id: int) -> str: ...
    def _status_url(self, camera_id: int) -> str: ...
    def _reconnect_url(self, camera_id: int) -> str: ...
```

并在 `config.py` 增加：

```python
VIDEO_HUB_BASE_URL = "http://127.0.0.1:5100"
VIDEO_HUB_TIMEOUT_MS = 5000
```

- [ ] **Step 4: 运行测试确认通过**

Run: `pytest tests/test_video_hub_frame_consumer.py -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add yolo-service/app/services/video_hub_client.py yolo-service/app/core/config.py yolo-service/tests/test_video_hub_frame_consumer.py
```

---

## Task 6: yolo-service 推理主循环切到远程 client

**Files:**
- Modify: `yolo-service/app/services/engine_task_service.py`
- Modify: `yolo-service/tests/test_video_hub_frame_consumer.py`

- [ ] **Step 1: 写失败测试**

在 `test_video_hub_frame_consumer.py` 增加一个 fake client：

```python
class FakeVideoHubClient:
    def ensure_session(self, camera_id, source_url):
        self.ensure_called = (camera_id, source_url)

    def fetch_snapshot(self, camera_id):
        return {
            "jpeg_bytes": b"\xff\xd8\xff\xd9",
            "frame_width": 320,
            "frame_height": 240,
            "timestamp": 1710000000000,
        }
```

断言 `engine_task_service` 通过 client 调 `ensure_session()` 和 `fetch_snapshot()`，而不是再触碰 `video_hub_registry`。

- [ ] **Step 2: 运行测试确认失败**

Run: `pytest tests/test_video_hub_frame_consumer.py -v`
Expected: FAIL

- [ ] **Step 3: 修改实现**

在 `engine_task_service.py`：

1. 删除 `from app.video_hub import video_hub_registry`
2. 注入或模块级创建 `VideoHubClient`
3. 把 `_run_loop_with_video_hub()` 中的：

```python
session = video_hub_registry.ensure_session(camera_id, stream_url)
frame_data = session.frame_cache.wait_for_frame(...)
```

改成：

```python
video_hub_client.ensure_session(camera_id, stream_url)
frame_data = video_hub_client.fetch_snapshot(camera_id)
```

4. 保持后续推理、tracker、评估、WebSocket 推送逻辑不变

- [ ] **Step 4: 运行测试确认通过**

Run: `pytest tests/test_video_hub_frame_consumer.py -v`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add yolo-service/app/services/engine_task_service.py yolo-service/tests/test_video_hub_frame_consumer.py
```

---

## Task 7: 移除 yolo-service 内置 video_hub API 注册

**Files:**
- Modify: `yolo-service/app/__init__.py`
- Delete: `yolo-service/app/api/video_hub.py`
- Delete: `yolo-service/app/api/video_hub_webrtc.py`

- [ ] **Step 1: 写失败测试**

在 yolo-service API 测试中新增：

```python
def test_yolo_service_no_longer_registers_video_hub_routes(client):
    response = client.get("/video-hub/cameras/1001/status")
    assert response.status_code == 404
```

- [ ] **Step 2: 运行测试确认失败**

Run: `pytest tests/test_engine_task_api.py -v -k video_hub`
Expected: FAIL

- [ ] **Step 3: 修改实现**

在 `app/__init__.py` 删除：

```python
from app.api.video_hub import blp as video_hub_blp
from app.api.video_hub_webrtc import blp as video_hub_webrtc_blp
```

并删除 blueprint 注册语句。随后删除两个 API 文件。

- [ ] **Step 4: 运行测试确认通过**

Run: `pytest tests/test_engine_task_api.py -v -k video_hub`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add yolo-service/app/__init__.py yolo-service/tests
```

---

## Task 8: 删除 yolo-service 内置正式实现

**Files:**
- Delete: `yolo-service/app/video_hub/__init__.py`
- Delete: `yolo-service/app/video_hub/frame_cache.py`
- Delete: `yolo-service/app/video_hub/registry.py`
- Delete: `yolo-service/app/video_hub/source_worker.py`
- Delete: `yolo-service/app/video_hub/webrtc_session.py`
- Delete: `yolo-service/app/video_hub/webrtc_signaling.py`

- [ ] **Step 1: 搜索残余引用**

Run: `rg "video_hub_registry|app\.video_hub|frame_cache|WebrtcSessionManager" yolo-service/app yolo-service/tests`
Expected: 只剩 `video_hub_client` 和无关字符串，不再有正式实现引用

- [ ] **Step 2: 删除旧实现文件**

删除 `yolo-service/app/video_hub/*` 全部文件。

- [ ] **Step 3: 运行 yolo-service 回归测试**

Run: `pytest tests/test_video_hub_frame_consumer.py tests/test_engine_task_api.py tests/test_model_inference_service.py -v`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git rm yolo-service/app/video_hub/__init__.py yolo-service/app/video_hub/frame_cache.py yolo-service/app/video_hub/registry.py yolo-service/app/video_hub/source_worker.py yolo-service/app/video_hub/webrtc_session.py yolo-service/app/video_hub/webrtc_signaling.py
```

---

## Task 9: frontend 无感验证与配置校验

**Files:**
- Verify: `frontend/.env.development`
- Verify: `frontend/.env.production`
- Verify: `frontend/src/utils/streamPreview.ts`
- Verify: `frontend/src/components/business/WebRtcWhepPlayer.vue`

- [ ] **Step 1: 校验 frontend 配置不变**

确认以下值仍成立：

```env
VITE_WEBRTC_WHEP_BASE_URL=/api
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
```

- [ ] **Step 2: 手工联调**

1. 启动 `video-hub-service`
2. 启动 backend
3. 启动 frontend
4. 打开监控总览页
5. F12 Network 确认仍请求：
   - `POST /api/video-hub/cameras/{id}/whip`
   - `DELETE /api/video-hub/sessions/{id}`

Expected: frontend 不需要知道新服务地址，页面行为与拆分前一致。

- [ ] **Step 3: 记录验证结果**

在本计划执行记录或 PR 说明中写明：frontend 无需改动或仅做了 Verify。

---

## Task 10: 端到端回归与验收

- [ ] **Step 1: video-hub-service 测试**

Run: `pytest tests/ -v`
Expected: PASS

- [ ] **Step 2: backend 定向测试**

Run: `mvn test -Dtest=VideoHubProxyControllerTest,CameraPreviewRouteServiceTest`
Expected: PASS

- [ ] **Step 3: yolo-service 定向测试**

Run: `pytest tests/test_video_hub_frame_consumer.py tests/test_engine_task_api.py tests/test_model_inference_service.py -v`
Expected: PASS

- [ ] **Step 4: 联调验收**

人工验证：

1. frontend WebRTC 正常播放
2. backend `/streams/cameras/{id}/preview` 仍可看 MJPEG
3. yolo-service 能正常推理并推送识别结果
4. video-hub-service `/status`、`/reconnect`、`/session` 正常工作
5. ESP32 仍只有平台这一条正式上游

- [ ] **Step 5: 更新文档**

更新：

```text
docs/规划文档/video-hub-service-decoupling-plan.md
docs/规划文档/esp32-cam-video-hub-staged-plan.md
```

把独立解耦相关步骤标记为已完成。

- [ ] **Step 6: 提交**

```bash
git add docs/规划文档/video-hub-service-decoupling-plan.md docs/规划文档/esp32-cam-video-hub-staged-plan.md
```
