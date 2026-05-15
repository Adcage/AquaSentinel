# video_hub 独立部署与服务解耦规划文档

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前内置在 `yolo-service` 中的 `video_hub` 彻底迁出为独立部署的 `video-hub-service`，并完成 backend、frontend、yolo-service 三侧调用链解耦。

**Architecture:** `video-hub-service` 独占视频拉流、缓存、MJPEG/WebRTC 输出、状态机与管理接口；`yolo-service` 只保留推理能力，通过独立 client 从 `video-hub-service` 取帧；`backend` 继续承担统一代理、鉴权和平台编排；`frontend` 仍只访问 backend，不直接暴露新视频服务地址。

**Tech Stack:** Spring Boot / Flask / aiortc / MJPEG / WebRTC WHEP / Vue 3 / HTTP API

---

## 1. 背景与现状

### 1.1 当前实际耦合状态

虽然阶段一和阶段二已经把视频链路能力补齐，但 `video_hub` 仍然物理内置在 `yolo-service` 中，形成了三类耦合：

1. **backend 对 yolo-service 内置 video_hub 的 HTTP 耦合**
   - `CameraPreviewRouteService` 当前直接把平台 MJPEG 预览路由到 `app.ai.engine.base-url + /video-hub/cameras/{id}/stream`
   - `VideoHubProxyController` 当前直接把 WHEP 信令转发到 `app.ai.engine.base-url + /video-hub/...`
   - `AppAiEngineProperties.baseUrl` 既承担 AI 引擎地址，又承担 video_hub 地址，配置语义已混合

2. **frontend 对 backend 当前 video_hub 代理路径的协议耦合**
   - `.env.development` / `.env.production` 当前把 WebRTC 地址模板写死为 `/api/video-hub/cameras/{cameraId}/whip`
   - `WebRtcWhepPlayer.vue` 依赖当前 `POST /whip -> Location -> DELETE /sessions/{id}` 的会话语义
   - `streamPreview.ts` 同时支持 backend proxy 和绝对 `video-hub` 地址，但 dev/prod 运行时已经默认依赖现有 backend 代理

3. **yolo-service 对进程内 video_hub 共享内存的强耦合**
   - `engine_task_service.py` 直接 `import video_hub_registry`
   - 推理主循环 `_run_loop_with_video_hub()` 直接 `ensure_session()`，再直接读取 `session.frame_cache.wait_for_frame()`
   - `api/video_hub.py`、`api/video_hub_webrtc.py`、`app/__init__.py` 都把 video_hub 当成本进程原生模块注册

### 1.2 现状的核心问题

当前最大的问题已经不是“video_hub 有没有能力”，而是“video_hub 的部署边界和职责边界仍未立正”。

这会带来以下风险：

1. **部署耦合风险**
   - 只要 `yolo-service` 重启、扩容或升级，video_hub 也一起受影响
   - 视频分发和 AI 推理无法独立扩缩容

2. **配置耦合风险**
   - backend 目前只有一个 `app.ai.engine.base-url`
   - 一旦 video_hub 独立部署，当前配置无法同时表达“AI 引擎地址”和“video_hub 地址”

3. **实现耦合风险**
   - yolo-service 现在通过进程内 `frame_cache` 直接读帧
   - 一旦 video_hub 拆出去，这条链路必须改成远程 client，否则只是“对外拆了，内部没拆”

4. **验收口径风险**
   - 如果只把 frontend/backend 改到新服务，而 yolo 内部继续保留旧 video_hub，系统会长期处于“双实现、双语义、双真相”的状态

### 1.3 本文档的目标边界

本文档聚焦 **video_hub 的独立部署与彻底解耦**，不重复描述阶段一与阶段二已经完成的能力建设。

本文档覆盖：

1. 新建独立 `video-hub-service` 的目标架构
2. backend、frontend、yolo-service 当前所有指向 video_hub 的调用链迁移方案
3. `yolo-service` 从进程内共享 `frame_cache` 改为远程取帧 client 的改造路径
4. 配置拆分、兼容策略、回退策略与验收标准

本文档不覆盖：

1. WebRTC 低延迟能力本身的实现细节（已在阶段二能力文档中定义）
2. ESP32 固件能力扩展
3. 录像、回放、分布式媒体集群
4. 直接让 frontend 直连 `video-hub-service`（本文坚持前端仍只访问 backend）

---

## 2. 目标架构

### 2.1 迁移后的正式架构

```text
ESP32-CAM
  -> 单路原始 MJPEG 源
  -> /api/ptz/* 控制接口

video-hub-service
  -> 唯一上游拉流者
  -> 最新帧缓存 / 状态缓存
  -> MJPEG 平台预览输出
  -> WebRTC WHEP 输出
  -> snapshot / status / reconnect / session 管理

yolo-service
  -> AI 推理与规则评估
  -> 通过 video-hub client 远程取帧
  -> 推送结构化识别结果给 backend

backend
  -> cameraId 与平台预览地址编排
  -> PTZ 控制代理
  -> video-hub HTTP / WHEP 统一代理
  -> 鉴权、设备状态与业务编排

frontend
  -> 正式页面只访问 backend
  -> backend 再代理 video-hub 的 MJPEG / WebRTC 能力
  -> 前端叠框
```

### 2.2 必须坚持的约束

1. **frontend 不直接访问 `video-hub-service`**
   - 仍然只跟 backend 交互
   - video-hub 地址不暴露给浏览器配置或业务代码

2. **yolo-service 不再保留进程内正式 video_hub 实现**
   - 不再注册 `/video-hub/*` API
   - 不再 import `video_hub_registry` / `frame_cache`
   - 不再把 `video_hub` 当成进程内共享模块

3. **backend 保持统一平台入口**
   - MJPEG 预览继续由 `/streams/cameras/{id}/preview` 暴露
   - WHEP 继续由 `/video-hub/cameras/{id}/whip` 暴露
   - backend 负责把请求代理到 `video-hub-service`

4. **独立服务优先兼容现有 API 语义**
   - 以减少 frontend/backend 改造量
   - 尽量保留 `/video-hub/cameras/{id}/stream|snapshot|status|ensure|reconnect|session|whip` 这套路径与语义

---

## 3. 当前影响面清单

### 3.1 backend 当前耦合点

1. `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java`
   - 当前把 `POST /video-hub/cameras/{id}/whip` 与 `DELETE /video-hub/sessions/{id}` 都转发到 `AppAiEngineProperties.baseUrl`
   - 迁移后必须改为独立 `videoHubBaseUrl`

2. `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java`
   - 当前把平台 MJPEG 预览路由到 `baseUrl + /video-hub/cameras/{id}/stream`
   - 迁移后必须改成独立 video_hub client 或独立 baseUrl

3. `backend/src/main/java/com/springboot/config/AppAiEngineProperties.java`
   - 当前 `baseUrl` 同时承担 AI 引擎与 video_hub 地址
   - 迁移后必须拆成独立配置对象，例如 `AppVideoHubProperties`

4. `backend/src/main/resources/application.yml`
   - 当前 `app.ai.engine.base-url=http://127.0.0.1:5000`
   - 迁移后必须新增 `app.video-hub.base-url`

5. `backend/src/main/java/com/springboot/service/impl/AiStreamTaskServiceImpl.java`
   - 当前 AI 任务的显示流和内部代理流都走 backend `/streams/.../preview`
   - 迁移后它的对外行为可以保持不变，但底层 preview 路由必须改指向独立服务

### 3.2 frontend 当前耦合点

1. `frontend/src/utils/streamPreview.ts`
   - 当前 webrtc 模式会拼接 `/api/video-hub/cameras/{cameraId}/whip`
   - 迁移后这条路径可以继续保留，但 backend 后面必须代理到新服务

2. `frontend/src/components/business/WebRtcWhepPlayer.vue`
   - 依赖 `POST WHIP -> Location -> DELETE /sessions/{id}` 语义
   - 独立服务必须继续遵循这套 WHEP 会话语义，避免前端组件返工

3. `frontend/.env.development` 与 `frontend/.env.production`
   - 当前写死 `VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip`
   - 若 backend 代理入口不变，则前端环境无需大改；否则必须同步调整

4. `frontend/src/services/dashboardService.ts` / `deviceService.ts`
   - 当前依赖 backend 返回 `previewUrl`
   - 只要 backend 平台预览入口不变，这两层可以保持稳定

### 3.3 yolo-service 当前耦合点

1. `yolo-service/app/services/engine_task_service.py`
   - 当前直接 `import video_hub_registry`
   - 当前 `_run_loop_with_video_hub()` 直接 `ensure_session()` 并读取 `session.frame_cache.wait_for_frame()`
   - 这是整个解耦迁移中最重的改造点

2. `yolo-service/app/__init__.py`
   - 当前注册 `video_hub` 与 `video_hub_webrtc` blueprint
   - 迁移后应删除这些注册，不再由 yolo-service 暴露 video_hub API

3. `yolo-service/app/api/video_hub.py` 与 `app/api/video_hub_webrtc.py`
   - 当前全部是本进程内 video_hub API
   - 迁移后应整体迁移到新服务，不再留在 yolo-service

4. `yolo-service/app/video_hub/*`
   - 当前是正式实现
   - 迁移后应迁入 `video-hub-service`，yolo-service 不再持有这套正式实现

---

## 4. 目标拆分方案（方案 A）

### 4.1 总体策略

采用 **同仓拆服务，先抽 client，再迁模块** 的方式推进：

1. 在当前仓库中新增独立目录 `video-hub-service/`
2. 先把当前 `yolo-service/app/video_hub/*` 的正式实现迁入新服务
3. 再让 backend 和 frontend 的代理链路指向新服务
4. 最后改造 yolo-service，用远程 client 取帧，删除进程内依赖

### 4.2 为什么选方案 A

相比“直接复制然后一次性切全链路”的方案，方案 A 更适合当前代码现状：

1. `engine_task_service.py` 当前对 `video_hub_registry/frame_cache` 是强进程内耦合，无法安全一次性硬切
2. backend 和 frontend 已经通过稳定的 HTTP 代理入口工作，保持这些入口不变可以显著降低联动风险
3. 独立服务迁移的真正难点不在 API，而在 **yolo-service 从共享内存读帧改成远程取帧**，这需要单独治理

---

## 5. 新服务职责与目录建议

### 5.1 新服务职责

`video-hub-service` 应承担以下全部正式职责：

1. `ensure/status/reconnect/session` 会话管理
2. 单设备唯一上游拉流
3. 最新帧缓存
4. MJPEG 平台输出
5. snapshot 输出
6. WebRTC WHEP 输出
7. 状态机、熔断、退避、可观测性

### 5.2 目录建议

```text
video-hub-service/
  app/
    __init__.py
    api/
      video_hub.py
      video_hub_webrtc.py
      health.py
    core/
      config.py
    video_hub/
      __init__.py
      frame_cache.py
      registry.py
      source_worker.py
      webrtc_session.py
      webrtc_signaling.py
    common/
      response.py
      errors.py
  tests/
    test_video_hub_source_worker.py
    test_video_hub_stream_api.py
    test_video_hub_webrtc.py
  main.py
  requirements.txt
```

### 5.3 不建议保留在 yolo-service 的实现

以下内容在迁移完成后不应继续作为 `yolo-service` 的正式模块保留：

1. `app/api/video_hub.py`
2. `app/api/video_hub_webrtc.py`
3. `app/video_hub/__init__.py`
4. `app/video_hub/frame_cache.py`
5. `app/video_hub/registry.py`
6. `app/video_hub/source_worker.py`
7. `app/video_hub/webrtc_session.py`
8. `app/video_hub/webrtc_signaling.py`

迁移完成后，若需要短期保留，也只能作为 **兼容层**，不能继续是正式流量入口。

---

## 6. 分阶段迁移计划

### Phase A：抽出独立服务骨架

目标：先把独立服务建立起来，但不立即切业务流量。

步骤：

1. 新建 `video-hub-service/` 目录和独立 Flask 应用骨架
2. 迁移 `frame_cache.py`、`source_worker.py`、`registry.py`、`video_hub.py`、`video_hub_webrtc.py`、`webrtc_session.py`、`webrtc_signaling.py`
3. 补齐配置、响应封装、错误处理与健康检查
4. 在新服务内跑通当前阶段二相关单元测试

这一步完成后，与修改前的区别：

1. 修改前：video_hub 只能跟着 yolo-service 一起启动
2. 修改后：video_hub 已经能作为独立进程单独启动和测试

### Phase B：backend 改指向独立服务

目标：保持 frontend 不变，先把 backend 底层转发链路切到新服务。

步骤：

1. 新增 `AppVideoHubProperties` 或等价配置类
2. 在 `application.yml` 新增：
   - `app.video-hub.base-url`
   - `app.video-hub.timeout-ms`
3. `VideoHubProxyController` 从 `AppAiEngineProperties.baseUrl` 改为 `AppVideoHubProperties.baseUrl`
4. `CameraPreviewRouteService` 从 `AppAiEngineProperties.baseUrl` 改为 `AppVideoHubProperties.baseUrl`
5. 补齐 backend 相关测试，明确验证它已经不再依赖 AI 引擎地址

这一步完成后：

1. frontend 仍然访问 backend `/api/video-hub/.../whip` 和 `/api/streams/.../preview`
2. 但 backend 实际已经改为代理独立 `video-hub-service`

### Phase C：yolo-service 改为远程取帧 client

目标：拆掉最重的进程内耦合。

步骤：

1. 在 `yolo-service` 中新建 `video_hub_client.py`
2. client 负责：
   - `ensure_session(camera_id, source_url)`
   - `get_status(camera_id)`
   - `fetch_snapshot(camera_id)` 或内部低频拉帧接口
   - `reconnect(camera_id)`
3. `engine_task_service.py` 改造：
   - 不再 `import video_hub_registry`
   - 不再读取 `session.frame_cache`
   - 改为通过 `video_hub_client` 远程获取最新帧
4. 明确新的推理取帧策略：
   - 第一优先：新增内部帧订阅/低频 JPEG 接口
   - 若暂时没有内部帧总线，则先通过 snapshot 拉取最新 JPEG
5. 补齐回归测试，验证推理链路在“video_hub 独立服务”下仍工作

这一步是整个解耦里最关键的一步，因为它意味着：

1. 修改前：推理循环依赖进程内共享内存
2. 修改后：推理循环只依赖远程服务契约

### Phase D：移除 yolo-service 内置 video_hub 入口

目标：完成“彻底迁出”。

步骤：

1. 删除 `app/__init__.py` 中对 `video_hub` / `video_hub_webrtc` blueprint 的注册
2. 删除或废弃 `app/api/video_hub.py`、`app/api/video_hub_webrtc.py`
3. 删除或迁走 `app/video_hub/*`
4. 清理所有直接 `import video_hub_registry` 的调用点
5. 清理与内置 video_hub 相关的配置假设

这一步完成后，系统正式达到目标态：

1. `video_hub` 只存在于 `video-hub-service`
2. `yolo-service` 只负责 AI 推理

---

## 7. backend / frontend / yolo-service 逐项迁移要求

### 7.1 backend 迁移要求

必须完成：

1. `AppAiEngineProperties.baseUrl` 不再承载 video_hub 地址
2. `VideoHubProxyController` 和 `CameraPreviewRouteService` 全部切到独立 `video-hub-service`
3. 测试中不再把 `http://127.0.0.1:5000/video-hub/...` 默认视为 AI 引擎地址，而是单独断言 video_hub 地址

### 7.2 frontend 迁移要求

frontend 不应感知“新服务已拆分”，只需满足：

1. `VITE_WEBRTC_WHEP_BASE_URL=/api` 仍保持不变
2. `VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip` 可保持不变
3. `streamPreview.ts` 和 `WebRtcWhepPlayer.vue` 不应该改成直连新服务

换言之：frontend 侧的成功标准不是“直接打新服务”，而是“无需感知新服务”。

### 7.3 yolo-service 迁移要求

必须完成：

1. `engine_task_service.py` 不再进程内读 `session.frame_cache`
2. `app/__init__.py` 不再注册 `video_hub` blueprint
3. `video_hub` 正式实现迁出后，yolo-service 内不再保留对其的正式流量依赖

---

## 8. 配置拆分方案

### 8.1 backend 配置

新增独立配置：

```yaml
app:
  ai:
    engine:
      base-url: http://127.0.0.1:5000
  video-hub:
    base-url: http://127.0.0.1:5100
    timeout-ms: 5000
```

说明：

1. `app.ai.engine.base-url` 只给 AI 任务和推理回调使用
2. `app.video-hub.base-url` 只给预览流、WHEP、snapshot、status、reconnect、session 管理使用

### 8.2 frontend 配置

原则：

1. 不新增独立 video_hub 域名配置给 frontend
2. frontend 仍只使用 `/api` 统一网关

### 8.3 yolo-service 配置

新增独立 client 配置：

```env
VIDEO_HUB_BASE_URL=http://127.0.0.1:5100
VIDEO_HUB_TIMEOUT_MS=5000
```

用于 `video_hub_client.py` 访问独立服务。

---

## 9. 兼容与回退策略

### 9.1 兼容窗口

迁移期间允许短期双跑，但必须满足：

1. **正式流量只允许一条主链路**
2. 双跑只用于迁移验证，不允许长期共存

建议顺序：

1. 新服务启动并通过独立测试
2. backend 先切新服务
3. 验证 frontend 无感
4. yolo-service 再切远程 client
5. 最后删除 yolo 内置 video_hub

### 9.2 回退策略

如果新服务出现问题：

1. backend 的 `app.video-hub.base-url` 临时切回旧 yolo-service video_hub 地址
2. yolo-service 若尚未完成 Phase D，可临时恢复进程内取帧路径
3. frontend 无需改动，因为它始终打 backend

这意味着最稳妥的回退锚点是 backend 配置层，而不是 frontend。

---

## 10. 验收标准

以下全部满足，才算“独立部署与解耦完成”：

1. `video-hub-service` 可独立启动、独立测试、独立部署
2. backend 的 MJPEG 预览与 WHEP 代理都已改指向 `video-hub-service`
3. frontend 无需感知新服务，页面功能保持不变
4. `yolo-service` 不再 import `video_hub_registry` / `frame_cache`
5. `yolo-service` 不再注册 `/video-hub/*` API
6. AI 推理仍能稳定取帧、推送结果、触发告警
7. ESP32 仍然只有平台这一条正式上游
8. `video_hub` 的熔断、退避、状态机、WebRTC、snapshot、MJPEG 能力在独立服务中全部保留

---

## 11. 最终结论

这次解耦不是简单“把几份文件搬个目录”，而是要把当前隐含在 `yolo-service` 里的三层耦合同时拆开：

1. **部署耦合**：video_hub 不能再跟着 AI 推理服务一起上线和重启
2. **配置耦合**：backend 不能再用同一个 `baseUrl` 同时表示 AI 引擎和 video_hub
3. **实现耦合**：yolo-service 不能再直接读 `session.frame_cache`

方案 A 的本质是：

1. 先把 `video_hub` 抽成真正独立的视频服务
2. 再让 backend 统一代理它
3. 最后把 yolo-service 从进程内共享内存迁移到远程 client 契约

这样做的结果是：

1. frontend 仍保持简单，不暴露内部视频服务
2. backend 继续保持平台统一入口
3. `video-hub-service` 可以独立扩缩容和运维
4. `yolo-service` 的职责收敛为真正的 AI 推理服务

这是当前“两阶段规划”下，阶段二最终收口为完整架构解耦的正确方向。
