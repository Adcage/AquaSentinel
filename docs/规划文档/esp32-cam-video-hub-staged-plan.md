# ESP32-CAM 平台接入与分阶段视频分发 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 ESP32-CAM 从“设备直连调试模式”推进到“平台统一接入模式”，先完成单路视频源接入、平台侧唯一拉流与复用、前端正式预览和 YOLO 统一取帧，再为后续低延迟升级到 WebRTC 保留清晰边界。

**Architecture:** ESP32-CAM 只提供单路原始视频源和 PTZ 控制接口，不承担多客户端视频分发职责；`yolo-service` 内新增 `video_hub` 作为唯一上游拉流者，负责拆帧、缓存、MJPEG 输出和快照输出；`backend` 负责设备管理、权限、PTZ 控制与平台预览地址编排；`frontend` 正式页面仅消费平台流，不再直连设备 IP。第二阶段在不改变上游接入方式的前提下，将 `video_hub` 的前端输出从 MJPEG 升级到低延迟协议，优先考虑 WebRTC。

**Tech Stack:** ESP32-CAM / Arduino / Spring Boot / Flask / MJPEG / WebSocket（识别结果）/ WebRTC（第二阶段预留）

---

## 1. 背景与当前现状

### 1.1 当前代码状态

1. `firmware/esp32-cam/src/main.cpp` 中已经具备 WiFi、UART 桥接与 `ControlServer` 启动逻辑，但摄像头启动被手动注释，当前固件日志明确写着“视频流已禁用”。
2. `firmware/esp32-cam/lib/network/ControlServer.cpp` 中 `/api/ptz/*` 控制链路已可用，`/stream` 目前固定返回 `503 Video stream temporarily disabled`。
3. `backend/src/main/java/com/springboot/service/Esp32PtzControlService.java` 已经可以根据设备地址代理调用 ESP32 的 PTZ 接口，说明控制链路基础已经具备。
4. `backend/src/main/java/com/springboot/controller/CameraStreamController.java` 已经有一套视频预览代理框架，但当前更偏向“把设备流透出给业务层”，还没有接入“平台唯一拉流并复用”的架构。
5. `frontend/src/utils/streamPreview.ts` 默认已经倾向 `backend_proxy` 模式，这说明前端正式方向本来就更适合走平台统一预览入口，而不是长期保留直连设备的方式。
6. `yolo-service` 当前已有推理、回调、WebSocket 推送、叠框服务等能力，但还没有一个明确的平台级 `video_hub` 模块负责“唯一拉流 + 平台输出 + YOLO 统一取帧”。

### 1.2 当前问题本质

当前最关键的问题不是“PTZ 控制没有打通”，而是“视频正式接入架构还没有收口”。

现状存在以下风险：

1. 如果前端、YOLO、调试页分别直接拉 ESP32 视频，ESP32-CAM 会同时承担摄像头采集、JPEG 输出、HTTP 服务、WiFi 发送和 UART 控制，负载过高且不稳定。
2. `CameraDevice.stream_url` 当前同时被当作“预览地址”和“设备基地址”使用，后续一旦平台流地址和设备源地址分离，字段语义会混乱。
3. 如果第一阶段为了快而把 MJPEG 输出、设备拉流和前端预览耦死，第二阶段升级低延迟时会整体返工。

### 1.3 本计划的目标边界

本计划只负责推进到“平台统一视频接入”的最小可用版本，并明确第二阶段的升级边界。

本计划覆盖：

1. ESP32-CAM 恢复单路原始视频源输出。
2. `yolo-service` 中新增 `video_hub`，实现单设备单上游拉流、平台 MJPEG 输出、快照输出和状态输出。
3. `backend` 提供平台预览地址编排能力，并继续承担 PTZ 控制代理职责。
4. `frontend` 正式页面切换到平台流。
5. YOLO 从平台侧统一取帧。

本计划不覆盖：

1. 设备主动注册/心跳上报。
2. ESP32 多码流输出。
3. 服务端把识别框烧录进视频。
4. 第一阶段就直接上 WebRTC。
5. 大规模流媒体集群、录像、回放、切片等扩展系统。
6. video_hub 拉流熔断、退避策略、可观测性与后端管理接口（纳入阶段二 5.7 节）。
7. 前端叠框组件（CameraOverlayLayer.vue）与识别结果元数据契约（frameWidth/frameHeight/timestamp/frameId）（纳入阶段二 5.8 节）。

---

## 2. 总体架构与分阶段路线

## 2.1 总体目标架构

```text
ESP32-CAM
  -> 单路原始 MJPEG 源
  -> /api/ptz/* 控制接口

yolo-service.video_hub
  -> 唯一上游拉流者
  -> 最新帧缓存 / 状态缓存
  -> 平台 MJPEG 预览输出
  -> 平台快照输出
  -> 第二阶段低延迟输出

backend
  -> cameraId 与设备地址/平台预览地址编排
  -> PTZ 控制代理
  -> 权限、鉴权、设备状态入口

frontend
  -> 正式页面只看平台流
  -> PTZ 控制只打 backend
  -> 前端叠框，识别结果与视频分离传输
```

### 2.2 阶段路线

#### 阶段一：最小可用正式接入

目标：先把 `ESP32 单路视频源 -> 平台唯一拉流 -> 前端正式预览 -> YOLO 统一取帧 -> PTZ 正常工作` 跑通。

输出特征：

1. 前端预览先使用平台侧 MJPEG。
2. YOLO 通过 `snapshot` 或内部缓存取图。
3. ESP32 只暴露一条正式上游源链路给平台。

#### 阶段二：低延迟优化

目标：在不改变上游 `ESP32 -> video_hub` 接入方式的前提下，将前端播放体验从“先可用”提升到“低延迟、低卡顿、适合 PTZ 联动”。

输出特征：

1. 视频主输出协议从 MJPEG 升级为低延迟方案。
2. 优先考虑 WebRTC；若需要成本更低的过渡，可先补 `WS-JPEG`。
3. 识别框继续走结构化数据通道，不与视频耦死。

### 2.3 为什么先用 MJPEG，再演进到 WebRTC

第一阶段使用 MJPEG 的原因：

1. 与当前 ESP32-CAM 能力最接近，恢复快，联调简单。
2. `video_hub` 可以较快完成“唯一拉流 + 平台复用”的核心架构验证。
3. 浏览器直接访问 URL 即可验证，适合早期排障。

第二阶段切向 WebRTC 的原因：

1. 监控系统对端到端延迟敏感，PTZ 控制时画面反馈必须更及时。
2. WebRTC 天生更适合低延迟视频分发。
3. 后续前端叠框、告警联动、云台控制体验都更匹配低延迟主链路。

---

## 3. 文件结构与职责映射

## 3.1 ESP32-CAM 相关文件

**Files:**
- Modify: `firmware/esp32-cam/src/main.cpp`
- Modify: `firmware/esp32-cam/lib/network/ControlServer.cpp`
- Modify: `firmware/esp32-cam/lib/camera/CameraStreamer.cpp`
- Modify: `firmware/esp32-cam/include/config.h`
- Verify: `firmware/esp32-cam/platformio.ini`

职责：

1. `main.cpp` 负责恢复摄像头初始化、保留 WiFi 与 PTZ 控制启动顺序，并在日志中准确区分“视频已开启”和“视频禁用”两种运行态。
2. `ControlServer.cpp` 负责把 `/stream` 从固定 `503` 改成真正的视频输出入口，并保留 `/api/ptz/*` 和 `/status` 的既有能力。
3. `CameraStreamer.cpp` 负责具体摄像头配置与 MJPEG 输出，第一阶段需要优先选择稳定参数，而不是追求最高分辨率。
4. `config.h` 负责把帧率、分辨率、JPEG 质量等参数固定在可控范围内，避免在早期联调时出现“功能一开就资源打满”的现象。

## 3.2 yolo-service 相关文件

**Files:**
- Modify: `yolo-service/app/__init__.py`
- Modify: `yolo-service/app/api/__init__.py`
- Modify: `yolo-service/app/core/config.py`
- Create: `yolo-service/app/video_hub/__init__.py`
- Create: `yolo-service/app/video_hub/registry.py`
- Create: `yolo-service/app/video_hub/source_worker.py`
- Create: `yolo-service/app/video_hub/frame_cache.py`
- Create: `yolo-service/app/video_hub/mjpeg_proxy.py`
- Create: `yolo-service/app/video_hub/snapshot_service.py`
- Create: `yolo-service/app/video_hub/status_service.py`
- Create: `yolo-service/app/api/video_hub.py`
- Create: `yolo-service/tests/test_video_hub_registry.py`
- Create: `yolo-service/tests/test_video_hub_snapshot_api.py`
- Create: `yolo-service/tests/test_video_hub_stream_api.py`

职责：

1. `registry.py` 负责全局管理 `cameraId -> 会话`，保证同一设备不会因为多个消费者而重复建立上游连接。
2. `source_worker.py` 负责连接设备原始 MJPEG 流、解析边界、抽取 JPEG 帧、记录分辨率和时间戳、断线后自动重连。
3. `frame_cache.py` 负责缓存最新一帧 JPEG 数据、帧宽高、最近成功时间、最近错误信息、查看者数量等状态。
4. `mjpeg_proxy.py` 负责把缓存帧重新组织成平台侧 MJPEG 输出给前端。
5. `snapshot_service.py` 负责返回最新帧给 YOLO 和调试接口使用。
6. `status_service.py` 负责汇总当前拉流状态，方便排查设备是否在线、最近是否有帧、是否出现上游异常。
7. `api/video_hub.py` 负责对外暴露 `ensure/stream/snapshot/status` 四类接口。

## 3.3 backend 相关文件

**Files:**
- Modify: `backend/src/main/java/com/springboot/controller/CameraStreamController.java`
- Modify: `backend/src/main/java/com/springboot/service/Esp32PtzControlService.java`
- Modify: `backend/src/main/java/com/springboot/controller/CameraDeviceController.java`
- Create: `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java`
- Create: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`
- Create: `backend/src/test/java/com/springboot/controller/CameraStreamControllerVideoHubRouteTest.java`

职责：

1. `CameraStreamController.java` 负责把“正式外部预览地址”编排到平台流，而不是继续把原始设备源作为最终业务入口直接暴露出去。
2. `Esp32PtzControlService.java` 继续负责 PTZ 控制代理，但要明确“设备基地址”是 PTZ 控制使用的设备 HTTP 根地址，而不是平台分发后的预览地址。
3. `CameraPreviewRouteService.java` 负责给某台设备生成平台预览地址，统一后端和前端对“正式预览入口”的认知。
4. `CameraDeviceController.java` 在设备详情或控制相关返回中，需要开始向前端提供平台预览入口所需的数据，而不是鼓励前端继续解析设备原始 URL。

## 3.4 frontend 相关文件

**Files:**
- Modify: `frontend/src/utils/streamPreview.ts`
- Modify: `frontend/src/services/deviceService.ts`
- Modify: `frontend/src/services/ptzControlService.ts`
- Modify: `frontend/src/components/business/CameraGridCard.vue`
- Modify: `frontend/src/views/admin/dashboard/AdminDashboardView.vue`
- Create: `frontend/src/components/business/CameraOverlayLayer.vue`
- Create: `frontend/src/types/videoHub.ts`
- Modify: `frontend/src/tests/streamPreview.test.ts`
- Create: `frontend/src/tests/cameraOverlayLayer.test.ts`

职责：

1. `streamPreview.ts` 负责把预览目标从“设备原始地址或后端旧代理”统一收敛到平台流入口。
2. `CameraGridCard.vue` 或实际预览组件负责展示平台 MJPEG，并为第二阶段替换成 WebRTC 播放器留出组件边界。
3. `CameraOverlayLayer.vue` 负责将识别结果叠加到视频之上，要求从第一阶段开始就按“原始帧尺寸 + 当前显示尺寸 + 时间戳”的思路设计。
4. `videoHub.ts` 负责定义平台预览状态、识别框元数据和后续低延迟播放切换所需的类型契约。

---

## 4. 阶段一实施计划：先做单路视频接入与平台 MJPEG 输出

### Task 1: 恢复 ESP32-CAM 单路视频源

**Files:**
- Modify: `firmware/esp32-cam/src/main.cpp`
- Modify: `firmware/esp32-cam/lib/network/ControlServer.cpp`
- Modify: `firmware/esp32-cam/lib/camera/CameraStreamer.cpp`
- Modify: `firmware/esp32-cam/include/config.h`

- [x] **Step 1: 恢复摄像头初始化并保留现有 PTZ 启动链路**

修改要求：

1. `main.cpp` 中恢复 `g_cameraStreamer.begin()`。
2. 摄像头初始化失败时，日志必须明确打印失败原因，不能默默继续运行成“无视频但无提示”的状态。
3. 无论视频初始化成功与否，PTZ 控制链路都应继续工作，这样便于区分“视频问题”和“控制问题”。

- [x] **Step 2: 将 `/stream` 从固定 503 改成真实输出**

修改要求：

1. `ControlServer.cpp` 中 `/stream` 不再固定返回 `503`。
2. 直接复用 `CameraStreamer` 的输出逻辑提供原始 MJPEG 源。
3. 保持 CORS 头与现有 `/api/ptz/*` 行为一致，方便浏览器阶段性联调。

- [x] **Step 3: 优先选稳定参数，而不是追求清晰度**

建议起步参数：

1. 分辨率从 `SVGA` 下调到更轻量级，例如 `QVGA` 或 `CIF`。
2. 降低帧率上限，减少 WiFi 和编码压力。
3. JPEG 质量先选一个中间值，确保单路输出稳定。

这样做与修改前的区别：

1. 修改前：为了省内存，直接禁用了整条视频链路。
2. 修改后：恢复视频，但通过保守参数把负载控制在 ESP32-CAM 可承受范围内。

- [x] **Step 4: 验证 PTZ 与视频共存稳定性**

Run:

```bash
pio run
```

Expected: 固件编译成功。

板上验证要求：

1. 打开 `/stream` 能持续出图。
2. 打开 `/stream` 时访问 `/api/ptz/status` 仍能返回正常 JSON。
3. 打开 `/stream` 时执行 `/api/ptz/nudge`、`/api/ptz/home` 不应频繁超时。

### Task 2: 在 yolo-service 中建立 video_hub 最小骨架

**Files:**
- Modify: `yolo-service/app/__init__.py`
- Modify: `yolo-service/app/api/__init__.py`
- Modify: `yolo-service/app/core/config.py`
- Create: `yolo-service/app/video_hub/__init__.py`
- Create: `yolo-service/app/video_hub/registry.py`
- Create: `yolo-service/app/video_hub/frame_cache.py`
- Create: `yolo-service/app/video_hub/source_worker.py`
- Create: `yolo-service/app/api/video_hub.py`

- [x] **Step 1: 先定义会话模型和缓存模型**

设计要求：

1. `registry` 层至少要能按 `cameraId` 唯一索引会话。
2. `frame_cache` 至少保存：`jpeg_bytes`、`frame_width`、`frame_height`、`timestamp`、`last_error`、`viewer_count`、`connected`。
3. 不要一开始就设计复杂历史缓存，第一阶段只保留“最新帧”和必要状态。

- [x] **Step 2: 实现唯一拉流约束**

行为要求：

1. 同一 `cameraId` 被多次 `ensure` 时，只能复用同一个 `source_worker`。
2. 前端打开多个预览、YOLO 发起快照时，都不能触发多条上游设备连接。
3. 若上游断开，由 `source_worker` 自己重连，不能让每个消费者各自重建连接。

- [x] **Step 3: 实现 MJPEG 上游拆帧**

要求：

1. `source_worker` 负责连接设备原始 `/stream`。
2. 解析 multipart MJPEG 边界，抽取每一张 JPEG。
3. 每接收到一帧时，将其写入 `frame_cache`，并更新尺寸和时间戳。
4. 发生错误时记录 `last_error`，并带退避策略重连。

- [x] **Step 4: 暴露 ensure/status 基础接口**

第一批接口：

1. `POST /video-hub/cameras/{cameraId}/ensure`
2. `GET /video-hub/cameras/{cameraId}/status`

响应要求：

1. 使用当前项目统一响应信封。
2. 明确返回 `connected`、`lastFrameAt`、`sourceWidth`、`sourceHeight`、`lastError`。

- [x] **Step 5: 添加基础测试**

Run:

```bash
pytest tests/test_video_hub_registry.py tests/test_video_hub_snapshot_api.py -v
```

Expected: 会话唯一性、状态更新、快照接口的基本行为通过。

### Task 3: 对前端输出平台 MJPEG，对 YOLO 输出快照

**Files:**
- Create: `yolo-service/app/video_hub/mjpeg_proxy.py`
- Create: `yolo-service/app/video_hub/snapshot_service.py`
- Create: `yolo-service/app/video_hub/status_service.py`
- Modify: `yolo-service/app/api/video_hub.py`
- Create: `yolo-service/tests/test_video_hub_stream_api.py`

- [x] **Step 1: 增加平台侧 MJPEG 输出接口**

接口：

1. `GET /video-hub/cameras/{cameraId}/stream`

行为要求：

1. 前端访问该接口时，不直接重新连接设备，而是从 `frame_cache` 按 MJPEG 形式重组输出。
2. 如果当前设备会话不存在，可由接口触发 `ensure` 或返回明确错误，两种策略要统一，不可一部分接口自动拉起，一部分要求手动确保。
3. 输出协议仍使用 `multipart/x-mixed-replace`，保证浏览器 `<img>` 或简单预览组件可直接使用。

- [x] **Step 2: 增加平台快照接口**

接口：

1. `GET /video-hub/cameras/{cameraId}/snapshot`

行为要求：

1. 返回最新 JPEG 二进制，而不是让 YOLO 再去解析 MJPEG。
2. 如果当前没有缓存帧，返回明确错误码和中文提示，避免算法端收到空图却难以定位问题。

- [x] **Step 3: 为 YOLO 内部消费定义统一入口**

要求：

1. 第一阶段先允许 YOLO 通过 HTTP 快照方式集成。
2. 第二阶段如果推理需要更高频率，可再增加内部订阅接口，但当前不要过度设计。

- [x] **Step 4: 增加平台级联调验证**

Run:

```bash
pytest tests/test_video_hub_stream_api.py -v
```

Expected: MJPEG 输出接口与快照接口可用，且不产生重复上游连接。

### Task 4: backend 收口预览地址编排与 PTZ 设备寻址

**Files:**
- Create: `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java`
- Modify: `backend/src/main/java/com/springboot/controller/CameraStreamController.java`
- Modify: `backend/src/main/java/com/springboot/service/Esp32PtzControlService.java`
- Create: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`

- [x] **Step 1: 引入“平台预览地址”概念**

要求：

1. 新增 `CameraPreviewRouteService`，统一生成某台设备的正式平台预览地址。
2. 平台预览地址不再等同于设备原始 `stream_url`。
3. `backend` 内部要开始把“设备基地址”和“平台预览地址”区分开来。

这与修改前的区别：

1. 修改前：字段语义混合，前端容易直连设备。
2. 修改后：后端承担编排职责，设备地址成为平台内部细节。

- [x] **Step 2: 保留 PTZ 控制代理，但明确设备基地址来源**

要求：

1. `Esp32PtzControlService` 不能误把未来的平台流地址当成 PTZ 控制基地址。
2. 如果短期仍复用 `stream_url` 存设备基地址，需要在代码和文档中明确当前假设，防止后续字段演化时埋坑。

- [x] **Step 3: 让前端能拿到平台预览入口**

要求：

1. 设备列表、设备详情或监控页所需数据中，要能拿到平台预览地址。
2. 前端不再需要通过 `cameraId + 旧逻辑` 自行猜测设备流入口。

- [x] **Step 4: 增加回归测试**

Run:

```bash
mvn test -Dtest=CameraPreviewRouteServiceTest,StreamProviderRouterTest
```

Expected: 平台预览地址编排逻辑和现有流路由测试均通过。

### Task 5: frontend 切换到平台侧正式 MJPEG 预览

**Files:**
- Modify: `frontend/src/utils/streamPreview.ts`
- Modify: `frontend/src/services/deviceService.ts`
- Modify: `frontend/src/components/business/CameraGridCard.vue`
- Modify: `frontend/src/tests/streamPreview.test.ts`

- [x] **Step 1: 统一预览入口为平台流**

要求：

1. 正式页面默认只使用平台预览 URL。
2. 保留旧直连模式仅用于临时调试开关，且不得作为正式业务默认路径。

- [x] **Step 2: 调整预览组件边界**（推迟至阶段二 5.9 节）

~~要求：~~

~~1. 预览组件要把"视频显示"和"叠框显示"两个关注点分开。~~
~~2. 第一阶段可以先只挂上视频显示壳子，叠框组件单独实现，避免未来切到 WebRTC 时耦死。~~

> 阶段一已完成视频显示壳子（CameraGridCard.vue 按 protocol 分流渲染），叠框组件 CameraOverlayLayer.vue 推迟至阶段二统一实现。

- [x] **Step 3: 验证预览与 PTZ 并存行为**

要求：

1. 页面开着视频时，PTZ 操作仍能成功。
2. 页面刷新、多个组件复用同一设备时，不应导致 ESP32 重复被直连。

- [x] **Step 4: 运行前端验证**

Run:

```bash
npm test -- streamPreview.test.ts
```

Expected: 预览地址解析逻辑符合平台流方案。

### Task 6: YOLO 改为从平台统一取帧

**Files:**
- Modify: `yolo-service/app/services/model_inference_service.py`
- Modify: `yolo-service/app/services/engine_task_service.py`
- Modify: `yolo-service/app/services/video_overlay_service.py`
- Create: `yolo-service/tests/test_video_hub_frame_consumer.py`

- [x] **Step 1: 把取帧入口改成平台快照或内部缓存**

要求：

1. 推理逻辑不再自己连设备原始视频源。
2. 对外统一由 `video_hub` 提供最新帧。
3. 对内如果同进程可直接访问缓存服务，应优先复用内部模块，避免平白增加 HTTP 开销。

- [x] **Step 2: 为叠框结果补齐元数据**（推迟至阶段二 5.9 节）

~~要求：~~

~~1. 每条识别结果都要带 `frameWidth`、`frameHeight`、`timestamp` 或 `frameId`。~~
~~2. 这样即便第一阶段视频还是 MJPEG，前端也能先按正确坐标系绘制，第二阶段切 WebRTC 时不用重做数据契约。~~

> 识别结果元数据契约推迟至阶段二，与叠框组件统一实现。

- [x] **Step 3: 增加基础回归**

Run:

```bash
pytest tests/test_video_hub_frame_consumer.py tests/test_model_inference_service.py -v
```

Expected: 推理侧能稳定拿到最新帧，并保留现有核心推理行为。

---

## 5. 阶段二实施计划：低延迟升级与前端叠框正式化

### 5.1 总体原则

阶段二不改变这三个核心前提：

1. `ESP32-CAM` 仍然只是单路原始视频源提供者。
2. `video_hub` 仍然是唯一上游拉流者。
3. 识别框仍然与视频分离传输，前端负责叠框。

阶段二只做“平台输出层升级”，不推翻阶段一的采集和缓存结构。

### 5.2 目标形态

目标形态建议为：

```text
ESP32-CAM MJPEG 源
  -> video_hub 采集层
  -> frame_cache / frame_bus
  -> WebRTC 输出层（前端主播放）
  -> WebSocket 识别结果通道（框、类别、分数、跟踪 ID）
  -> frontend: video + overlay
```

### 5.3 第二阶段新增文件建议

**Files:**
- Create: `yolo-service/app/video_hub/webrtc_session.py`
- Create: `yolo-service/app/video_hub/webrtc_signaling.py`
- Create: `yolo-service/app/api/video_hub_webrtc.py`
- Modify: `frontend/src/components/business/WebRtcWhepPlayer.vue`
- Modify: `frontend/src/components/business/CameraOverlayLayer.vue`
- Create: `frontend/src/services/videoHubWsService.ts`
- Create: `frontend/src/tests/cameraOverlaySync.test.ts`

说明：

1. 前端已有 `WebRtcWhepPlayer.vue`，这说明项目里已经存在 WebRTC 播放方向的基础组件，可以优先评估是否可复用，而不是从零再造新的播放组件。
2. 如果当前前端现有 WebRTC 组件与 `video_hub` 新协议不完全匹配，应优先改造其输入方式，而不是复制出第二套播放器。

### 5.4 第二阶段具体目标

1. 视频主播放改成低延迟方案，优先 WebRTC。
2. 识别结果通过 WebSocket 或其他实时数据通道单独推送。
3. 前端叠框按时间戳和原始帧尺寸进行同步，不再只是静态坐标叠加。

### 5.5 叠框为什么继续坚持前端实现

即使存在同步风险，也仍然推荐前端叠框，原因是：

1. 框是业务层结构化数据，不是视频本体。
2. 前端可以自由高亮、点击、过滤、隐藏、联动告警。
3. 第二阶段只要加好 `timestamp/frameId` 同步策略，错位问题是可控的。
4. 如果把框画死在服务端视频里，后续切换原图/AI 图、做交互、调试错位都会更麻烦。

### 5.6 第二阶段前端叠框契约

识别结果建议固定包含以下字段：

```json
{
  "cameraId": 12,
  "frameWidth": 320,
  "frameHeight": 240,
  "timestamp": 1715488800123,
  "detections": [
    {
      "x": 80,
      "y": 42,
      "width": 54,
      "height": 66,
      "label": "person",
      "score": 0.93,
      "trackId": 7
    }
  ]
}
```

前端绘制时必须执行：

1. 按 `frameWidth/frameHeight` 与视频实际显示尺寸做缩放。
2. 如果视频使用 `object-fit: contain` 或 `cover`，需要显式处理黑边或裁剪偏移。
3. 结果超时则直接丢弃，避免把旧框叠到新画面上。

### 5.7 video_hub 拉流健壮性与可观测性

阶段一已经验证了"ESP32 单路源 → 平台唯一拉流 → 前端可看"这条链路，但存在以下已识别的运营问题：

1. ESP32 重启后，`video_hub` 的恢复窗口受 `read_timeout` 限制，默认 300 秒，已暂时降至 10 秒，但仍只依赖底层 HTTP 超时，无法区分"上游短暂卡顿"和"上游彻底断开"。
2. `video_hub` 拉流线程一旦启动就不会停止，即使无人观看也会持续占用上游连接和资源。
3. 没有熔断机制：连续连接失败时会以固定间隔无限重试，浪费资源且无状态可见。
4. `status` 接口缺少关键运维字段：无法区分"正在连接"、"已连接但没有帧"、"熔断暂停"等状态。
5. SpringBoot 后端无法管理 `video_hub` 会话：不能主动触发重连、不能主动停止拉流、不能查看熔断原因。

这些问题在 `video_hub` 独立部署后会更加突出——如果没有可观测性和管理接口，这个服务就是黑箱。

因此，将以下能力纳入阶段二（或 video_hub 独立服务拆分时）的实施范围：

#### 5.7.1 拉流状态机

VideoHubSession 引入显式四态模型：

| 状态 | 含义 | 行为 |
|------|------|------|
| `CONNECTING` | 正在建立上游连接 | 快速重连退避：1.5s → 3s → 5s → 10s |
| `CONNECTED` | 上游已连接，持续收帧 | 正常推帧到缓存 |
| `STALE` | 已连接但超过 `stale_frame_timeout_sec` 没收到新帧 | 主动断开并进入重连 |
| `CIRCUIT_OPEN` | 连续失败超过阈值，熔断暂停 | 低频探测（60s），不主动推帧 |

激活条件（任一满足即可从 `CIRCUIT_OPEN` 立即跳出并重连）：

1. 有新的平台 viewer 打开 `/stream`。
2. 收到 `/ensure` 请求。
3. `source_url` 变更。
4. 收到 `/reconnect` 手动触发。

#### 5.7.2 无帧超时检测

- 参数：`stale_frame_timeout_sec = 5.0`
- 已连接状态下，如果连续 5 秒没有收到任何新帧，主动判定连接失活，断开后立即重连。
- 与修改前的区别：修改前只依赖底层 HTTP 读超时（10s），无法区分"上游还在但断流"和"上游彻底重启"。

#### 5.7.3 熔断与退避策略

重连间隔随连续失败次数逐步退避：

| 连续失败次数 | 重连间隔 |
|-------------|---------|
| 1-2 | 1.5s |
| 3-4 | 3s |
| 5-6 | 5s |
| 7-9 | 10s |
| ≥10（进入熔断） | 60s 低频探测 |

熔断后不会永久停止重连，而是以 60 秒间隔低频探测上游是否恢复。一旦探测成功，失败计数清零，恢复正常连接。

#### 5.7.4 熔断原因记录

`status` 接口新增字段：

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

| 字段 | 说明 |
|------|------|
| `state` | 当前状态，四选一：`CONNECTING` / `CONNECTED` / `STALE` / `CIRCUIT_OPEN` |
| `circuit_open_reason` | 熔断原因摘要 |
| `consecutive_failures` | 连续失败次数 |
| `last_failure_at` | 最近一次失败的时间戳(ms) |
| `last_failure_detail` | 最近一次异常的原始信息 |
| `stale_frame_timeout_sec` | 无帧超时阈值 |
| `retry_delay_sec` | 当前重连间隔（随退避变化） |

#### 5.7.5 管理接口

新增接口供 SpringBoot 后端管理 video_hub 会话：

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/video-hub/cameras/{id}/reconnect` | 手动强制重连：无论当前状态，立即断开现有连接，清零失败计数，立即重连 |
| DELETE | `/video-hub/cameras/{id}/session` | 主动销毁会话：停止拉流线程，清除缓存，下次有人访问时才重新建立 |

已有接口的增强：

| 方法 | 路径 | 变化 |
|------|------|------|
| POST | `/video-hub/cameras/{id}/ensure` | 新增行为：如当前处于熔断状态，收到 ensure 后立即跳出熔断并重连 |
| GET | `/video-hub/cameras/{id}/status` | 响应体新增 `state`、`circuit_open_reason`、`consecutive_failures` 等字段 |

#### 5.7.6 改动文件范围

| 文件 | 改什么 |
|------|--------|
| `yolo-service/app/video_hub/source_worker.py` | 状态机、熔断逻辑、退避策略、无帧超时、原因记录 |
| `yolo-service/app/video_hub/registry.py` | 新增 `remove_session()` 方法 |
| `yolo-service/app/video_hub/frame_cache.py` | 记录最后收到帧的时间，支持无帧超时判定 |
| `yolo-service/app/api/video_hub.py` | 新增 `/reconnect` 和 `DELETE /session`，`status` 响应扩展 |
| `yolo-service/tests/test_video_hub_source_worker.py` | 熔断、退避、无帧超时测试 |
| `yolo-service/tests/test_video_hub_stream_api.py` | `/reconnect`、`DELETE /session` 测试 |

### 5.8 前端叠框组件与识别结果元数据契约

阶段一已实现视频显示壳子（CameraGridCard.vue 按 protocol 分流渲染），但叠框组件和识别结果元数据契约推迟至阶段二统一实现。

#### 5.8.1 CameraOverlayLayer.vue

独立叠框组件，负责将识别结果叠加到视频之上：

1. 接收识别结果（框坐标、类别、分数、跟踪 ID）和视频显示尺寸。
2. 按 `frameWidth/frameHeight` 与视频实际显示尺寸做缩放。
3. 如果视频使用 `object-fit: contain` 或 `cover`，显式处理黑边或裁剪偏移。
4. 结果超时则直接丢弃，避免把旧框叠到新画面上。

#### 5.8.2 识别结果元数据契约

每条识别结果固定包含以下字段：

```json
{
  "cameraId": 12,
  "frameWidth": 320,
  "frameHeight": 240,
  "timestamp": 1715488800123,
  "detections": [
    {
      "x": 80,
      "y": 42,
      "width": 54,
      "height": 66,
      "label": "person",
      "score": 0.93,
      "trackId": 7
    }
  ]
}
```

#### 5.8.3 改动文件范围

| 文件 | 改什么 |
|------|--------|
| `frontend/src/components/business/CameraOverlayLayer.vue` | 新建叠框组件 |
| `frontend/src/types/videoHub.ts` | 新建平台预览状态与识别框元数据类型契约 |
| `frontend/src/components/business/CameraGridCard.vue` | 接入叠框组件 |
| `yolo-service/app/services/video_overlay_service.py` | 识别结果补齐 frameWidth/frameHeight/timestamp |
| `frontend/src/tests/cameraOverlayLayer.test.ts` | 叠框缩放与超时丢弃测试 |

### 5.9 第二阶段风险点

1. WebRTC 引入的信令、ICE、浏览器兼容与 NAT 场景复杂度会明显高于 MJPEG。
2. 如果阶段一没有把采集层与输出层拆开，阶段二会被迫重写 `video_hub`。
3. 如果阶段一没有统一识别结果的 `timestamp/frameId`，阶段二叠框很容易漂移。
4. 熔断与退避策略需要根据实际设备类型和网络环境调参，阈值不宜过早硬编码，应支持配置化。

---

## 6. 分阶段测试与验证计划

### 6.1 阶段一测试矩阵

#### ESP32-CAM

Run:

```bash
pio run
```

验证：

1. 固件编译成功。
2. 设备上 `/stream` 有连续输出。
3. `/api/ptz/status`、`/api/ptz/home`、`/api/ptz/nudge` 在开流时仍能用。

#### yolo-service

Run:

```bash
pytest tests/test_video_hub_registry.py tests/test_video_hub_snapshot_api.py tests/test_video_hub_stream_api.py -v
```

验证：

1. 单设备只会创建一个会话。
2. 状态接口能反映最近成功帧时间与错误信息。
3. 平台 MJPEG 和快照接口能工作。

#### backend

Run:

```bash
mvn test -Dtest=CameraPreviewRouteServiceTest,StreamProviderRouterTest
```

验证：

1. 平台预览地址编排逻辑正确。
2. 现有流路由和 PTZ 相关逻辑未被破坏。

#### frontend

Run:

```bash
npm test -- streamPreview.test.ts
npx vue-tsc --noEmit
```

验证：

1. 预览地址解析逻辑符合平台流方案。
2. 类型检查通过。

### 6.2 阶段一联调验收

1. 一台设备在前端正式页面能持续预览。
2. 打开预览同时进行 PTZ 操作，体感延迟可接受且无明显串口控制失灵。
3. YOLO 通过平台取帧不会额外增加 ESP32 上游连接。
4. 多个前端页面看同一设备时，平台侧可以多消费，但 ESP32 仍只有平台这一条正式连接。

### 6.3 阶段二联调验收

1. 低延迟播放时 PTZ 控制反馈明显快于阶段一。
2. 叠框在连续移动场景中没有明显漂移。
3. 视频主链路切换为低延迟输出后，YOLO 仍沿用阶段一的取帧契约，不需重写核心推理入口。

---

## 7. 风险、回退与约束

### 7.1 核心风险

1. ESP32-CAM 恢复视频后内存与 WiFi 压力过大，导致频繁掉流或重启。
2. `yolo-service` 同时承担推理与 `video_hub`，若代码边界处理不好，会变成“大而乱”的 Python 服务。
3. `backend` 若继续把 `stream_url` 当万能字段，会在平台流地址引入后出现语义冲突。
4. 前端如果先用简单画框但不携带时间和尺寸元数据，第二阶段将很难修正错位问题。

### 7.2 回退策略

1. 如果 ESP32-CAM 开流不稳定，第一优先级是继续降分辨率、降帧率，而不是立刻扩展平台复杂度。
2. 如果 `video_hub` 对外 MJPEG 重组压力过大，可先只保留快照接口给 YOLO，前端暂时保持单路预览验证，但平台唯一拉流的架构不能回退。
3. 如果后端短期无法改完设备数据模型，至少要先在服务层把“设备基地址”和“平台预览地址”隔离开，避免新逻辑继续污染字段语义。

### 7.3 必须坚持的约束

1. 任何正式消费者都不能直接拉 ESP32 原始视频。
2. PTZ 控制入口始终走后端，不允许前端正式页面绕过平台直接控设备。
3. `video_hub` 采集层与输出层必须分开，实现阶段一时就要预留第二阶段升级路径。
4. 识别框数据契约须在阶段二叠框组件实现时同步补齐 `frameWidth/frameHeight/timestamp/frameId`（从阶段一推迟，纳入 5.8 节）。

---

## 8. 推荐执行顺序

1. 先恢复 ESP32-CAM `/stream`，验证开流与 PTZ 共存。
2. 在 `yolo-service` 中建立 `video_hub` 最小骨架，实现唯一拉流和状态接口。
3. 补齐 `stream/snapshot` 平台输出。
4. 调整 `backend` 的平台预览地址编排与 PTZ 设备寻址。
5. 切换 `frontend` 到平台侧 MJPEG。
6. 切换 YOLO 到平台统一取帧。
7. 阶段一联调整体通过后，再启动第二阶段低延迟方案设计与验证。

---

## 9. 最终结论

这次规划的核心不是“把视频先弄能看就行”，而是要在第一阶段就把视频链路的职责边界立正：

1. **ESP32-CAM 只做单路源，不做分发。**
2. **`yolo-service.video_hub` 做唯一拉流与平台复用。**
3. **`backend` 做业务编排与 PTZ 控制，不承担重视频分发职责。**
4. **`frontend` 正式页面只消费平台流，并从第一阶段开始为前端叠框和后续 WebRTC 升级预留结构。**

这样做的结果是：

1. 当前能以最小代价尽快落地正式接入。
2. 后续要提升低延迟体验时，不需要推翻第一阶段，只需要升级 `video_hub` 的下游输出层。
3. 识别框、告警联动、PTZ 操作和视频预览可以逐步收敛到同一套平台化架构中。
