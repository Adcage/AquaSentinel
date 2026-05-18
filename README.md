# AquaSentinel

<p align="left">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.3-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20AI-OpenAI-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring AI" />
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.5-1F2937?style=flat-square" alt="MyBatis-Plus" />
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=flat-square&logo=rabbitmq&logoColor=white" alt="RabbitMQ" />
</p>

<p align="left">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3" />
  <img src="https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Vite-7-646CFF?style=flat-square&logo=vite&logoColor=white" alt="Vite" />
  <img src="https://img.shields.io/badge/Pinia-3-F7D336?style=flat-square" alt="Pinia" />
  <img src="https://img.shields.io/badge/Element%20Plus-2.11-409EFF?style=flat-square" alt="Element Plus" />
  <img src="https://img.shields.io/badge/ECharts-6-AA344D?style=flat-square&logo=apacheecharts&logoColor=white" alt="ECharts" />
</p>

<p align="left">
  <img src="https://img.shields.io/badge/Flask-3-000000?style=flat-square&logo=flask&logoColor=white" alt="Flask" />
  <img src="https://img.shields.io/badge/YOLOv8-Ultralytics-111111?style=flat-square" alt="YOLOv8" />
  <img src="https://img.shields.io/badge/DeepSort-Tracking-0F766E?style=flat-square" alt="DeepSort" />
  <img src="https://img.shields.io/badge/OpenCV-4.9-5C3EE8?style=flat-square&logo=opencv&logoColor=white" alt="OpenCV" />
  <img src="https://img.shields.io/badge/PyAV-Video-2563EB?style=flat-square" alt="PyAV" />
  <img src="https://img.shields.io/badge/WebRTC-aiortc-0F766E?style=flat-square" alt="WebRTC aiortc" />
  <img src="https://img.shields.io/badge/RTSP%20%7C%20MJPEG%20%7C%20FLV-Video%20Stream-334155?style=flat-square" alt="Video Stream Protocols" />
</p>

<p align="left">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Media3-ExoPlayer-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Media3 ExoPlayer" />
  <img src="https://img.shields.io/badge/STM32-03234B?style=flat-square&logo=stmicroelectronics&logoColor=white" alt="STM32" />
  <img src="https://img.shields.io/badge/ESP32--CAM-E7352C?style=flat-square&logo=espressif&logoColor=white" alt="ESP32-CAM" />
  <img src="https://img.shields.io/badge/PlatformIO-F5822A?style=flat-square&logo=platformio&logoColor=white" alt="PlatformIO" />
  <img src="https://img.shields.io/badge/KiCad-314CB0?style=flat-square&logo=kicad&logoColor=white" alt="KiCad" />
</p>

AquaSentinel 是面向泳池、游泳馆和水上场馆的实时 AI 安全监控系统。系统围绕“视频采集、AI 检测、告警生成、实时推送、现场处置、记录追踪”构建完整闭环，用于降低水域安全监控中的发现延迟、告警分发延迟和处置追踪成本。

## 核心亮点

| 亮点 | 说明 |
| --- | --- |
| 端到端安全监控闭环 | 从摄像头或 ESP32-CAM 视频采集开始，经过 AI 检测、告警落库、Web/Android 实时推送，最终进入确认、派发、处置和记录追踪流程。 |
| 视频网关与业务系统解耦 | `video-hub-service` 独立承担 RTSP、HTTP-MJPEG、HTTP-FLV、WebRTC、ESP32 WebSocket 推帧等视频接入和分发能力，Backend 聚焦权限、配置和业务编排。 |
| AI 检测与跟踪链路 | `yolo-service` 基于 YOLOv8 执行人体目标检测，结合 DeepSort 多目标跟踪和溺水规则判定，输出结构化检测事件和风险信息。 |
| 多端实时协同 | Backend 统一接收 AI 推送并进行告警落库、状态管理和权限过滤，再通过 WebSocket 分发给 Web 管理后台与 Android 救生员端。 |
| 软硬件联动 | ESP32-CAM 负责摄像头采集、网络推帧和 HTTP 控制入口，STM32 负责双轴云台、OLED 状态展示、按键交互、电池采样和校准参数保存。 |
| 工程化治理 | 系统包含 JWT 鉴权、注解式权限控制、HMAC AI 回调校验、RabbitMQ 异步告警扩展、Prometheus 指标、OpenAPI 类型生成和统一响应信封。 |

## 效果展示

### 系统架构

![AquaSentinel 系统架构](docs/assets/aquasentinel-architecture.png)

### 视频链路

![AquaSentinel 视频链路](docs/assets/aquasentinel-video-link.png)

## 系统架构

系统按职责拆分为多个可独立运行的子项目：

| 模块 | 技术栈 | 核心职责 |
| --- | --- | --- |
| `backend` | Java 17 / Spring Boot / MyBatis-Plus / MySQL / Redis / WebSocket | 用户认证、权限控制、设备管理、告警落库、统计聚合、实时推送、业务 API |
| `frontend` | Vue 3 / TypeScript / Vite / Pinia / Element Plus / ECharts | Web 管理后台、设备监控、告警处置、数据看板、地图展示、AI 助手入口 |
| `yolo-service` | Python / Flask / YOLOv8 / DeepSort / OpenCV / PyAV | 视频流推理、目标检测、轨迹跟踪、溺水规则判定、AI 结果推送 |
| `video-hub-service` | Python / Flask / PyAV / aiortc / Redis Pub/Sub | 视频源接入、帧缓存、协议适配、MJPEG/Snapshot/WebRTC 分发 |
| `android` | Kotlin / Jetpack Compose / Retrofit / OkHttp / Media3 | 救生员移动端、实时告警接收、视频预览、现场处置、定位能力 |
| `firmware` | STM32 / ESP32-CAM / PlatformIO | 摄像头采集、云台控制、OLED 显示、设备侧交互和串口协议 |

详细架构说明见 [系统架构文档](docs/architecture/system-architecture.md)。

## 核心业务链路

### 视频接入与预览

摄像头、ESP32-CAM 或 RTSP 源输出视频后，由 `video-hub-service` 负责统一接入和协议适配。客户端不直接依赖底层视频源地址，而是通过 Backend 编排后的预览入口获取可播放的视频流。

```text
摄像头 / ESP32-CAM / RTSP 源
  -> video-hub-service
  -> MJPEG / Snapshot / WebRTC / 预览地址
  -> Frontend / Android
```

### AI 推理与溺水判定

`yolo-service` 读取视频帧后执行 YOLOv8 目标检测，通过 DeepSort 为目标分配稳定轨迹，再由规则服务根据持续时间、目标状态和位置变化判断风险。

```text
视频帧
  -> YOLOv8 人体检测
  -> DeepSort 多目标跟踪
  -> 溺水规则判定
  -> 检测事件 / 告警候选
```

### 实时告警分发

AI 服务通过 WebSocket 将检测结果推送到 Backend。Backend 统一进行告警记录、状态维护、权限过滤和多端分发，保证 Web 管理后台和 Android 端看到一致的告警状态。

```text
YOLO Service
  -> WebSocket
Backend
  -> 落库 + 状态管理 + 权限过滤
Frontend 管理后台 / Android 救生员端
  -> 确认、派发、处置、追踪
```

## 技术难点与设计取舍

### 视频流能力独立部署

视频预览如果直接放在 Backend 中处理，会让业务服务同时承担鉴权、CRUD、告警、转码和媒体分发职责。项目将视频流能力拆分到 `video-hub-service`，让 Backend 只负责配置、权限、路由和业务编排，视频网关负责拉流、缓存、协议适配和预览分发。

### 持续推理任务管理

AI 检测不是一次性模型调用，而是持续运行的视频任务。`yolo-service` 对推理任务进行启动、运行、停止、异常等状态管理，并配套读帧、推理、推帧和状态维护逻辑，以适配长时间监控场景。

### WebSocket 分层推送

告警链路同时涉及 AI 服务、业务后端、Web 管理端和 Android 端。项目将 AI 结果接入与用户端告警分发拆为不同 WebSocket 通道，避免前端直接依赖 AI 服务，也让 Backend 可以统一处理告警状态、权限过滤和多端同步。

### 接口一致性与类型安全

后端通过 Knife4j/OpenAPI 暴露接口规范，前端使用 openapi2ts 生成 API 调用代码和类型定义。业务页面通过 `services/` 层消费生成接口，减少手写请求造成的字段不一致、路径漂移和响应结构误用。

### 多端与硬件协同

Web 管理端适合场馆、设备、地图和统计管理，Android 端适合救生员现场接收和处置告警，硬件端负责视频采集与云台动作。系统通过统一 Backend 聚合设备、告警和处置状态，避免各端维护割裂的数据副本。

## 功能模块

| 模块 | 功能 |
| --- | --- |
| 用户与权限 | 登录注册、JWT 鉴权、刷新令牌、角色权限、接口级权限校验 |
| 场馆管理 | 场馆、区域、摄像头、救生员等基础数据维护 |
| 视频监控 | 摄像头列表、实时预览、视频流路由、云台控制入口 |
| AI 检测 | 推理任务管理、检测结果上报、溺水规则判定 |
| 告警中心 | 告警生成、实时推送、确认、派发、处置、记录追踪 |
| 数据看板 | 告警统计、设备状态、场馆态势、图表展示 |
| 移动端 | 救生员登录、告警接收、视频查看、定位与现场处置 |
| 硬件联动 | ESP32-CAM 视频采集、STM32 云台控制、OLED 信息显示、电池状态采样 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| Backend | Java 17 / Spring Boot 3.2.3 / MyBatis-Plus 3.5.5 / MySQL / Redis / JWT / WebSocket / RabbitMQ / Spring AI / Knife4j |
| Frontend | Vue 3 / TypeScript / Vite 7 / Pinia / Element Plus / ECharts / 高德地图 / openapi2ts |
| YOLO Service | Python / Flask / Flask-Smorest / SQLAlchemy / YOLOv8 / DeepSort / OpenCV / PyAV |
| Video Hub | Python / Flask / PyAV / aiortc / Redis Pub/Sub / MJPEG / Snapshot / WebRTC |
| Android | Kotlin / Jetpack Compose / Retrofit / OkHttp / Media3 ExoPlayer / Paging 3 / 高德 3D Map SDK |
| Firmware | STM32 / ESP32-CAM / PlatformIO / OLED / UART / PWM 云台控制 / KiCad |

完整技术栈说明见 [技术栈说明](docs/architecture/tech-stack.md)。

## 快速开始

### Docker Compose 启动核心服务

```bash
docker compose up -d
```

### 验证核心服务状态

```bash
pwsh ./scripts/verify_core_stack.ps1
```

详细启动步骤见 [核心服务启动指南](docs/deployment/core-stack-quickstart.md)。

## 本地开发命令

### Backend

```bash
cd backend
mvn compile
mvn test
```

### Frontend

```bash
cd frontend
npm install
npm run dev
npm run build
```

### YOLO Service

```bash
cd yolo-service
pip install -r requirements.txt
python main.py --dev
pytest
```

### Android

```bash
cd android
./gradlew assembleDebug
./gradlew test
```

## 仓库结构

```text
AquaSentinel/
├── backend/              # Spring Boot 业务后端
├── frontend/             # Vue 3 管理后台
├── yolo-service/         # Flask + YOLOv8 AI 推理服务
├── video-hub-service/    # 视频流接入与协议适配服务
├── android/              # Jetpack Compose 移动端
├── firmware/             # STM32 / ESP32-CAM 硬件端代码
├── docs/                 # 架构、部署、硬件、排障和归档文档
├── scripts/              # 启动、验证和辅助脚本
└── docker-compose.yml    # 核心服务编排
```

## 文档导航

| 分类 | 文档 | 说明 |
| --- | --- | --- |
| 系统架构 | [系统架构文档](docs/architecture/system-architecture.md) | 说明系统分层、服务职责、核心数据流和关键模块设计 |
| 技术栈 | [技术栈说明](docs/architecture/tech-stack.md) | 说明后端、前端、AI 服务、视频网关、Android 和硬件端的技术选型 |
| 快速启动 | [核心服务启动指南](docs/deployment/core-stack-quickstart.md) | 说明 Backend、YOLO Service、video-hub-service、MySQL、Redis 的本地启动方式 |
| 硬件设计 | [硬件设计说明](docs/hardware/hardware-design-spec.md) | 说明 ESP32-CAM、STM32、云台、供电和通信链路设计 |
| 硬件接线 | [硬件接线指南](docs/hardware/wiring-guide.md) | 说明 ESP32-CAM、STM32、舵机、OLED、按键和电源的接线方式 |
| 云台控制 | [STM32 云台控制说明](docs/hardware/stm32-ptz-guide.md) | 说明 STM32 云台控制器、串口协议、OLED 状态和电池检测能力 |
