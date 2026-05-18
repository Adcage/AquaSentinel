# AquaSentinel 项目架构文档

## 1. 文档目的

本文用于说明 AquaSentinel 当前项目架构，重点服务于后续绘制系统架构图、部署架构图、业务链路图、视频链路图、AI 推理链路图和硬件链路图。

本文不只描述代码目录，还明确各子系统的职责边界、通信协议、核心数据流和推荐绘图节点。后续绘制 DrawIO、Mermaid 或其他架构图时，可以直接从“架构图绘制清单”章节抽取节点与连线。

## 2. 系统总体架构

AquaSentinel 是面向游泳馆、水上场馆等场景的水上安全监控系统。系统围绕“视频采集、AI 检测、告警生成、实时推送、救生员处置、数据追踪”构建闭环。

当前系统由以下部分组成：

| 架构层级 | 子系统 | 核心职责 |
|----------|--------|----------|
| 用户入口层 | `frontend` | Web 管理后台，负责设备监控、告警管理、统计分析、AI 助手、地图展示 |
| 用户入口层 | `android` | 救生员移动端，负责实时告警接收、报警处置、定位上报、视频预览 |
| 业务中心层 | `backend` | 统一业务 API、认证鉴权、告警落库、任务编排、WebSocket 分发、PTZ 代理、AI 助手 |
| AI 推理层 | `yolo-service` | 视频帧推理、YOLO 检测、DeepSort 跟踪、溺水规则判定、AI 事件上报 |
| 视频网关层 | `video-hub-service` | 视频源接入、帧缓存、MJPEG、Snapshot、WebRTC WHIP、ESP32 WebSocket 推帧接入 |
| 边缘硬件层 | `firmware` | ESP32-CAM 视频采集与网络网关，STM32 云台控制、OLED、按键、电池采样 |
| 基础设施层 | MySQL | 业务数据持久化 |
| 基础设施层 | Redis | 会话、缓存、视频流配置同步、Redisson 分布式能力 |
| 基础设施层 | RabbitMQ | 报警事件异步分发 |
| 基础设施层 | Prometheus | 监控指标采集 |
| 外部能力层 | OpenAI / DashScope 兼容模型服务 | AI 助手、报警分析、向量嵌入相关能力 |
| 外部能力层 | 高德地图服务 | Web 和 Android 地图、定位能力 |

总体链路可以概括为：

```text
摄像头 / ESP32-CAM / RTSP 源
  -> video-hub-service 视频接入与协议适配
  -> yolo-service 拉帧推理
  -> backend 告警落库与业务分发
  -> frontend 管理后台 / android 救生员端
  -> 告警确认、处置、统计和追踪
```

## 3. 子系统职责边界

### 3.1 Frontend Web 管理后台

- 路径：`frontend/`
- 技术栈：Vue 3、TypeScript、Vite、Pinia、Element Plus、ECharts、AMap JS API、Axios、openapi2ts。
- 部署形态：开发阶段为 Vite Dev Server，生产阶段为静态资源，可由 Nginx 托管。
- 核心职责：管理端登录、场馆管理、设备管理、摄像头监控、救生员管理、报警管理、统计分析、AI 助手、WebSocket 实时告警展示。
- 对外依赖：主要访问 `backend`，不直接依赖 `yolo-service`；视频预览可通过后端代理或 video-hub 输出的 WebRTC/MJPEG 地址完成。

### 3.2 Android 救生员端

- 路径：`android/`
- 技术栈：Kotlin、Jetpack Compose、Material 3、Navigation Compose、Retrofit、OkHttp、OkHttp WebSocket、Paging 3、高德地图 3D Map SDK。
- 部署形态：APK。
- 核心职责：救生员登录、首页在岗信息、实时告警接收、告警详情、告警处置、定位上报、视频预览、个人中心。
- 对外依赖：直接访问 `backend` 的 REST API、WebSocket 和 MJPEG 预览入口；AI 检测结果通过 Backend 间接获得。

### 3.3 Backend 业务中心

- 路径：`backend/`
- 技术栈：Java 17、Spring Boot 3.2.3、Spring MVC、Spring WebSocket、MyBatis-Plus、MySQL、Redis、Redisson、RabbitMQ、JWT、Spring AI、Knife4j、Micrometer、Prometheus。
- 默认端口：`8300`。
- 默认上下文路径：`/api`。
- 核心职责：用户认证、权限控制、业务 CRUD、AI 任务编排、告警落库、报警推送、WebSocket 分发、统计聚合、视频预览路由、PTZ 控制代理、AI 助手。
- 对外依赖：MySQL、Redis、RabbitMQ、YOLO Service、video-hub-service、ESP32 PTZ HTTP API、OpenAI/DashScope 兼容模型服务。

### 3.4 YOLO Service AI 推理服务

- 路径：`yolo-service/`
- 技术栈：Python、Flask、Flask-Smorest、SQLAlchemy、ultralytics YOLO、OpenCV、PyAV、DeepSort、websocket-client、requests、pika、prometheus_client。
- 默认端口：`5000`。
- 核心职责：接收 Backend 下发的推理任务，按摄像头拉取视频帧，执行 YOLO 检测和 DeepSort 跟踪，通过溺水规则生成风险事件，将实时结果和报警事件推送给 Backend。
- 对外依赖：Backend、video-hub-service、RabbitMQ、可选 SQLite/MySQL、本地模型文件。

### 3.5 video-hub-service 视频网关

- 路径：`video-hub-service/`
- 技术栈：Python、Flask、flask-sock、aiortc、PyAV、Pillow、numpy、requests、redis。
- 默认端口：`5100`。
- 核心职责：统一接入 RTSP、HTTP-MJPEG、HTTP-FLV、ESP32 WebSocket JPEG 推帧等视频源，缓存最新帧，并向客户端或 AI 服务输出 MJPEG、Snapshot、WebRTC WHIP 视频能力。
- 对外依赖：摄像头、ESP32-CAM、Redis、Backend。

### 3.6 Firmware 边缘硬件

- 路径：`firmware/`
- 技术栈：ESP32-CAM、STM32F103C8T6、PlatformIO、OV2640、SG90 舵机、SSD1306 OLED。
- 核心职责：ESP32-CAM 负责摄像头采集、WiFi、HTTP 控制接口、WebSocket 推帧；STM32 负责双轴云台 PWM 控制、OLED 页面、按键、电池 ADC 采样、校准参数保存。
- 对外依赖：ESP32 通过 WiFi 访问 video-hub-service 和接收 Backend PTZ 控制请求；STM32 不直接联网，通过 UART 与 ESP32 通信。

## 4. Backend 架构

Backend 是系统业务中心，对外统一暴露 REST API、WebSocket、SSE 和视频预览路由。

### 4.1 分层结构

```text
HTTP / WebSocket / SSE Request
  -> Controller / WebSocket Handler
  -> Service / ServiceImpl
  -> Mapper / MyBatis-Plus
  -> MySQL
```

主要目录：

| 层级 | 路径 | 说明 |
|------|------|------|
| Controller | `backend/src/main/java/com/springboot/controller` | REST API 入口 |
| AI Controller | `backend/src/main/java/com/springboot/ai/controller` | AI 助手和报警分析入口 |
| Service | `backend/src/main/java/com/springboot/service` | 业务接口 |
| ServiceImpl | `backend/src/main/java/com/springboot/service/impl` | 业务实现 |
| Mapper | `backend/src/main/java/com/springboot/mapper` | MyBatis-Plus 数据访问 |
| Entity | `backend/src/main/java/com/springboot/model/entity` | 数据库实体 |
| DTO | `backend/src/main/java/com/springboot/model/dto` | 请求参数对象 |
| VO | `backend/src/main/java/com/springboot/model/vo` | 响应视图对象 |
| Security | `backend/src/main/java/com/springboot/security` | JWT、HMAC、鉴权上下文 |
| WebSocket | `backend/src/main/java/com/springboot/websocket` | 实时推送和 AI 推送接入 |
| Messaging | `backend/src/main/java/com/springboot/messaging` | RabbitMQ 发布和消费 |
| AI | `backend/src/main/java/com/springboot/ai` | AI 对话、Function Calling、报警分析、向量检索 |

### 4.2 认证与权限

Backend 使用 JWT 双令牌机制和注解式权限控制。

```text
客户端登录
  -> /api/auth/login 或 /api/auth/admin/login
  -> AuthServiceImpl 校验账号密码
  -> JwtTokenProvider 签发 AccessToken / RefreshToken
  -> 客户端保存 Token
  -> 后续请求携带 Authorization: Bearer <token>
```

请求鉴权链路：

```text
HTTP Request
  -> JwtAuthInterceptor
  -> JwtTokenProvider 解析 Token
  -> AccessControlService 加载角色和权限
  -> AuthContextHolder 保存当前用户上下文
  -> Controller
  -> @AuthCheck AOP 校验角色或权限码
```

AI 回调使用 HMAC 签名校验：

```text
YOLO Service
  -> POST /api/internal/ai/events
  -> 请求头 X-AI-Key / X-AI-Timestamp / X-AI-Signature
  -> HmacSignatureVerifier 校验 timestamp + body
  -> InternalAiCallbackController 处理事件
```

### 4.3 核心业务模块

| 模块 | 核心职责 | 典型接口 |
|------|----------|----------|
| 认证模块 | 登录、注册、刷新令牌、登出、验证码 | `/api/auth/**` |
| 用户角色模块 | 用户管理、角色管理、权限控制 | `/api/users/**`、`/api/roles/**` |
| 场馆区域模块 | 场馆、区域、围栏数据管理 | `/api/venues/**`、`/api/venue-zones/**` |
| 摄像头模块 | 摄像头 CRUD、维护记录、预览路由、PTZ 控制 | `/api/cameras/**`、`/api/streams/**` |
| 监控任务模块 | 启动、停止、查询 AI 推理任务 | `/api/monitor/tasks/**` |
| 告警模块 | 告警记录、处置动作、分配、推送 | `/api/alerts/**` |
| 救生员模块 | 救生员档案、值班、定位、离岗报备 | `/api/lifeguards/**` |
| 统计模块 | 趋势、排名、KPI、导出 | `/api/stats/**` |
| AI 助手模块 | 对话、流式输出、报警分析、相似报警检索 | `/api/ai/**` |
| 视频网关代理 | video-hub token 校验、摄像头源解析 | `/api/video-hub/**` |

### 4.4 WebSocket 通道

Backend 有两类关键 WebSocket 通道。

| 通道 | 方向 | 作用 |
|------|------|------|
| `/api/ws/alerts` | Frontend / Android -> Backend | 客户端接收告警、实时检测批次、视频帧，也可发送订阅动作 |
| `/api/ws/ai-push` | YOLO Service -> Backend | AI 服务推送实时检测结果和视频帧 |

客户端实时订阅链路：

```text
frontend / android
  -> WebSocket /api/ws/alerts?token=<token>
  -> 发送 SUBSCRIBE_MONITOR_REALTIME
  -> Backend 记录订阅 cameraIds
  -> Backend 转发 MONITOR_REALTIME_BATCH / MONITOR_VIDEO_FRAME
```

AI 推送接入链路：

```text
yolo-service
  -> WebSocket /api/ws/ai-push
  -> AiPushWebSocketHandler
  -> AlertWebSocketHandler / MonitorRealtimeWsPublisher
  -> frontend / android
```

### 4.5 RabbitMQ 异步分发

报警事件可通过 RabbitMQ 异步分发。

```text
alert.topic exchange
  -> alert.record.queue
  -> alert.notification.queue
  -> alert.analytics.queue
```

推荐在架构图中将 RabbitMQ 表示为 Backend 内部异步扩展点：

```text
Backend Alert Event Publisher
  -> RabbitMQ alert.topic
  -> Alert Record Consumer / Notification Consumer / Analytics Consumer
```

### 4.6 AI 助手与报警分析

Backend 集成 Spring AI，对外提供对话和报警分析能力。

```text
Frontend AI Chat Panel
  -> /api/ai/chat/stream
  -> AiChatController
  -> DefaultChatService
  -> Spring AI ChatClient
  -> OpenAI / DashScope 兼容模型服务
```

报警分析链路：

```text
AlertRecord 创建
  -> 发布 AlertAnalysisEvent
  -> AlertAnalysisListener 异步处理
  -> 大模型生成分析结论
  -> Embedding 生成向量
  -> MySQL alert_embedding 存储
```

## 5. Frontend 架构

Frontend 是 PC Web 管理后台，主要通过 Backend 获取业务数据和实时消息。

### 5.1 应用结构

```text
main.ts
  -> createApp(App)
  -> Pinia
  -> Router
  -> Element Plus
  -> theme.css
```

分层结构：

```text
Views / Components
  -> Services
  -> Generated API Clients
  -> request.ts Axios 实例
  -> backend /api/**
```

主要目录：

| 层级 | 路径 | 说明 |
|------|------|------|
| 入口 | `frontend/src/main.ts`、`frontend/src/App.vue` | Vue 应用初始化 |
| 路由 | `frontend/src/router` | 基础路由、后台路由、路由守卫 |
| 布局 | `frontend/src/layouts/BackendLayout.vue` | 后台框架、菜单、顶部栏、WebSocket 告警入口 |
| 页面 | `frontend/src/views` | 登录、注册、后台各业务页面 |
| 业务组件 | `frontend/src/components/business` | 摄像头、地图、表格、指标卡、AI 聊天等组件 |
| 服务层 | `frontend/src/services` | 业务门面，封装生成 API 和数据转换 |
| API 层 | `frontend/src/api` | openapi2ts 自动生成 Controller 和类型 |
| 请求层 | `frontend/src/request.ts` | Axios 拦截器、Token 注入、401 处理 |
| 状态 | `frontend/src/stores` | Pinia Store，当前以轻量刷新信号为主 |

### 5.2 页面模块

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录 | `/user/login` | 管理员登录，写入 sessionStorage Token |
| 注册 | `/user/register` | 用户注册 |
| 监控总览 | `/admin/dashboard` | 摄像头网格、实时检测、告警概览 |
| 设备管理 | `/admin/device` | 摄像头设备 CRUD 和维护信息 |
| 云台控制 | `/admin/ptz-control` | PTZ 控制测试与校准 |
| 救生员管理 | `/admin/lifeguard` | 救生员列表、状态、地图、围栏 |
| 报警管理 | `/admin/alarm` | 报警查询、批量处理、详情 |
| 用户管理 | `/admin/user` | 后台用户管理 |
| 统计分析 | `/admin/statistics` | KPI、趋势、分布、排名、导出 |
| AI 助手 | `/admin/ai-chat` | 流式 AI 对话 |
| 系统设置 | `/admin/settings` | 系统配置页面 |
| 个人中心 | `/admin/profile` | 当前用户信息 |

### 5.3 API 调用链路

```text
Admin View
  -> service 业务门面
  -> src/api/*Controller.ts
  -> request.ts
  -> backend /api/**
```

认证状态：

```text
LoginView
  -> authService.loginAsAdmin
  -> authController.adminLogin
  -> Backend /auth/admin/login
  -> sessionStorage 保存 token / refreshToken / authUser
  -> request.ts 自动添加 Authorization
```

### 5.4 实时通信

Frontend 使用 `alertWsService` 连接 Backend WebSocket。

```text
BackendLayout
  -> alertWsService.connect
  -> /api/ws/alerts?token=<token>
  -> ALERT_CREATED
  -> 弹出紧急告警
```

实时监控订阅：

```text
AdminDashboardView
  -> alertWsService.send(SUBSCRIBE_MONITOR_REALTIME)
  -> Backend /api/ws/alerts
  -> MONITOR_REALTIME_BATCH
  -> window CustomEvent
  -> CameraGridCard / CameraOverlayLayer 更新检测框
```

视频帧链路：

```text
Backend /api/ws/alerts
  -> MONITOR_VIDEO_FRAME JSON header
  -> JPEG binary Blob / ArrayBuffer
  -> alertWsService
  -> monitor-video-frame CustomEvent
  -> CameraStreamSurface 显示帧
```

### 5.5 视频预览

Frontend 支持多种预览模式。

```text
CameraGridCard
  -> CameraStreamSurface
  -> MJPEG img / WS JPEG / WebRTC WHEP
```

WebRTC 链路：

```text
WebRtcWhepPlayer
  -> POST WHEP SDP offer
  -> video-hub-service /video-hub/cameras/{id}/whip
  -> SDP answer
  -> RTCPeerConnection 播放视频 track
```

MJPEG 链路：

```text
CameraStreamSurface
  -> img src=/api/streams/cameras/{cameraId}/preview
  -> Backend
  -> video-hub-service /video-hub/cameras/{cameraId}/stream
```

## 6. Android 架构

Android 是救生员移动端，面向现场处置场景。

### 6.1 应用结构

```text
MainActivity
  -> AppConfig.init
  -> AuthSession.init
  -> RealtimeAlertNotifier.initialize
  -> AndroidTheme
  -> SwimSafeApp
  -> Navigation Compose
  -> Screens
```

分层结构：

```text
Compose Screens
  -> RemoteRepositories
  -> ApiClient / Retrofit / OkHttp
  -> Backend REST API
```

主要目录：

| 层级 | 路径 | 说明 |
|------|------|------|
| 入口 | `android/app/src/main/java/com/vision/swimsafe/MainActivity.kt` | 应用初始化 |
| 导航 | `ui/navigation` | 路由定义和导航图 |
| 页面 | `ui/screens` | 登录、首页、告警、定位、视频、我的 |
| 组件 | `ui/components` | 顶栏、告警弹窗、视频播放器、地图组件 |
| UI 模型 | `ui/model/UiModels.kt` | 页面状态模型 |
| 远端数据 | `data/remote` | Retrofit API、Repository、DTO、Mapper |
| 实时告警 | `data/alert` | WebSocket 告警、系统通知、UI 事件流 |
| 视频流 | `data/stream` | 实时帧 WebSocket、帧组装协议 |
| 配置 | `config/AppConfig.kt` | 高德 Key 等配置 |

### 6.2 REST API 链路

```text
Compose Screen
  -> RemoteXxxRepository
  -> ApiClient.service
  -> Retrofit ApiService
  -> OkHttpClient
  -> backend /api/**
```

主要 API 能力：

- 救生员登录：`POST /api/lifeguards/login`
- 登出：`POST /api/auth/logout`
- 告警分页：`POST /api/alerts/list/page`
- 告警详情：`GET /api/alerts/{id}`
- 告警处置：`POST /api/alerts/action`
- 救生员列表：`POST /api/lifeguards/list/page/vo`
- 最近定位：`GET /api/lifeguards/location/recent`
- 定位上报：`POST /api/lifeguards/location/report`
- 摄像头分页：`POST /api/cameras/list/page/vo`
- 今日告警统计：`GET /api/alerts/stats/today`

### 6.3 实时告警

```text
Backend WebSocket /api/ws/alerts
  -> RealtimeAlertNotifier
  -> 去重 eventUid
  -> Android Notification
  -> MutableSharedFlow<UiAlertEvent>
  -> HomeScreen
  -> AlarmDialog
  -> AlarmDetailScreen
```

### 6.4 实时视频

Android 优先使用 WebSocket JPEG 帧，失败时回退 MJPEG。

```text
VideoListScreen / AlarmDetailScreen
  -> WsJpegFramePlayer
  -> MonitorRealtimeFrameWsClient
  -> Backend /api/ws/alerts
  -> MONITOR_VIDEO_FRAME header + JPEG binary
  -> VideoFrameAssembler
  -> Compose Image
```

回退链路：

```text
WsJpegFramePlayer fallback
  -> MjpegStreamPlayer
  -> WebView <img>
  -> /api/streams/cameras/{cameraId}/preview?provider=auto&token=<token>
```

### 6.5 定位与地图

```text
LocationScreen
  -> AMapLocationClient
  -> 高德定位服务
  -> AMapView
  -> 高德地图 SDK
  -> RemoteLocationRepository.reportCurrentLocation
  -> Backend /api/lifeguards/location/report
```

## 7. YOLO Service 架构

YOLO Service 是 AI 推理服务，负责把视频帧转化为检测结果和风险事件。

### 7.1 API 分层

```text
Flask App
  -> Flask-Smorest Blueprint
  -> Marshmallow Schema 参数校验
  -> Engine Task Service
  -> 推理任务线程
```

主要接口：

| 接口 | 说明 |
|------|------|
| `GET /health` | 健康检查 |
| `POST /engine/tasks/start` | 启动推理任务 |
| `POST /engine/tasks/stop` | 停止推理任务 |
| `GET /engine/tasks/{taskCode}` | 查询任务状态 |
| `POST /engine/tasks/model/switch` | 切换模型版本 |
| `POST /engine/tasks/config/update` | 更新推理配置 |
| `POST /engine/test/trigger-alert` | 测试触发报警 |

### 7.2 推理任务流程

```text
Backend
  -> POST /engine/tasks/start
  -> engine_task_service.start_task
  -> 创建 EngineTaskState
  -> 启动后台线程 engine-task-{taskCode}
  -> VideoHubClient.ensure_session
  -> VideoHubClient.fetch_snapshot
  -> OpenCV 解码 JPEG 帧
  -> YOLO 推理
  -> DeepSortTracker.update
  -> DrowningRuleEvaluator.evaluate
  -> 更新任务实时状态
  -> WebSocket 推送实时结果
  -> 必要时 HTTP 回调报警事件
  -> 可选 RabbitMQ 发布 alert.record
```

### 7.3 AI 检测内部节点

| 节点 | 职责 |
|------|------|
| Engine Task API | 接收 Backend 任务启动、停止、查询请求 |
| Engine Task Service | 管理任务生命周期、线程、状态和事件上报 |
| Task Worker Thread | 执行持续拉帧、推理、规则判断 |
| VideoHubClient | 调用 video-hub-service 建立会话和获取快照 |
| OpenCV Decoder | 将 JPEG bytes 解码为图像帧 |
| YOLO Model | 识别人体或风险目标 |
| DeepSortTracker | 生成稳定 track_id，跟踪目标轨迹 |
| DrowningRuleEvaluator | 根据姿态、热力、持续时间、冷却期判断溺水风险 |
| AI WebSocket Push Client | 向 Backend `/api/ws/ai-push` 推送实时结果和视频帧 |
| HTTP Callback Client | 向 Backend `/api/internal/ai/events` 回调报警事件 |
| RabbitMQ Publisher | 发布 `alert.record` 事件 |

### 7.4 报警事件输出

```text
DrowningRuleEvaluator 触发风险
  -> _build_event_payload
  -> eventUid / cameraId / taskCode / eventType / riskLevel / confidence / bbox / riskPoint
  -> HTTP Callback Client
  -> Backend /api/internal/ai/events
```

## 8. video-hub-service 架构

video-hub-service 是独立视频网关，承担视频接入、缓存和协议适配。

### 8.1 职责边界

video-hub-service 只处理视频，不处理业务用户、告警状态、救生员派发等业务逻辑。

```text
摄像头 / ESP32-CAM / RTSP / HTTP-FLV / HTTP-MJPEG
  -> video-hub-service
  -> FrameCache
  -> MJPEG / Snapshot / WebRTC WHIP
  -> frontend / android / yolo-service / backend proxy
```

### 8.2 内部结构

| 模块 | 职责 |
|------|------|
| Flask API | 暴露健康检查、视频会话、MJPEG、Snapshot、WebRTC、WebSocket 推帧接口 |
| VideoHubRegistry | 按 camera_id 管理 VideoHubSession |
| VideoHubSession | 维护单摄像头会话状态和 Worker |
| Source Worker | 拉取 RTSP、HTTP-MJPEG、HTTP-FLV 或接收 push 帧 |
| FrameCache | 缓存最新 JPEG 帧、尺寸、时间戳 |
| WebRTC Session Manager | 将 JPEG 帧转为 WebRTC 视频 Track |
| RedisStreamSync | 从 Redis 同步摄像头流配置和变更事件 |
| CameraSourceResolver | 调 Backend 校验 token 并解析 sourceUrl |

### 8.3 视频输入

Pull 模式：

```text
RTSP / HTTP-MJPEG / HTTP-FLV
  -> Source Worker
  -> PyAV / requests 解码或拆帧
  -> JPEG
  -> FrameCache
```

Push 模式：

```text
ESP32-CAM
  -> WS /video-hub/cameras/push
  -> token
  -> camera_id
  -> JPEG binary frames
  -> FrameCache
```

### 8.4 视频输出

| 输出方式 | 接口 | 消费者 |
|----------|------|--------|
| Snapshot | `GET /video-hub/cameras/{cameraId}/snapshot` | YOLO Service、调试工具、预览缩略图 |
| MJPEG | `GET /video-hub/cameras/{cameraId}/stream` | Backend proxy、Frontend、Android |
| WebRTC WHIP | `POST /video-hub/cameras/{cameraId}/whip` | Frontend WebRTC 播放器 |
| Status | `GET /video-hub/cameras/{cameraId}/status` | Backend、监控页面 |

### 8.5 Redis 配置同步

```text
Backend
  -> Redis Hash aqua:camera:streams
  -> Redis Pub/Sub aqua:camera:events
  -> video-hub-service RedisStreamSync
  -> upsert / delete VideoHubSession
```

### 8.6 Backend 鉴权协作

当 video-hub-service 需要解析摄像头源时，会把 token 交给 Backend 判断权限。

```text
video-hub-service
  -> GET /api/video-hub/auth/camera-source?cameraId=...&token=...
  -> Backend 校验 token 和摄像头权限
  -> 返回 sourceUrl
  -> video-hub-service 建立会话
```

## 9. 固件与硬件架构

硬件链路由 ESP32-CAM 和 STM32F103C8T6 共同组成。

### 9.1 硬件节点

| 节点 | 职责 |
|------|------|
| ESP32-CAM AI-Thinker | 摄像头采集、WiFi、HTTP PTZ 网关、WebSocket 推帧 |
| OV2640 摄像头 | 视频图像采集 |
| STM32F103C8T6 Blue Pill | 实时云台控制、OLED、按键、电池采样 |
| SG90 PAN 舵机 | 水平方向转动 |
| SG90 TILT 舵机 | 俯仰方向转动 |
| SSD1306 OLED | 状态、校准、电量、网络信息显示 |
| PB12 用户按键 | 短按切换页面，长按云台回中 |
| 电池与升压模块 | 为 ESP32、STM32、舵机提供 5V 电源 |

### 9.2 ESP32-CAM 职责

```text
ESP32-CAM
  -> 初始化 WiFi
  -> 初始化 OV2640
  -> 提供 /stream 本地 MJPEG
  -> 提供 /api/ptz/* HTTP 控制接口
  -> 通过 UART2 转发 PTZ 指令给 STM32
  -> 通过 WebSocket 推送 JPEG 帧到 video-hub-service
```

### 9.3 STM32 职责

```text
STM32
  -> 接收 UART 指令
  -> 控制 PA6 / PA7 PWM
  -> 驱动 SG90 PAN / TILT 舵机
  -> 读取 PB12 按键
  -> 采样 PA0 电池电压
  -> 更新 SSD1306 OLED
  -> 保存校准参数
```

### 9.4 UART 协议

ESP32 与 STM32 之间使用 UART 传递文本指令。

| 指令 | 说明 |
|------|------|
| `HOME` | 云台回中 |
| `STATUS?` | 查询状态 |
| `MOVE:<pan>,<tilt>` | 移动到指定角度 |
| `NUDGE:<dir>,<step>` | 按方向微调 |
| `CALIB:START` | 进入校准 |
| `CALIB:PAN,<pulse_us>` | 设置 PAN 校准脉宽 |
| `CALIB:TILT,<pulse_us>` | 设置 TILT 校准脉宽 |
| `CALIB:SAVE` | 保存校准 |
| `CALIB:EXIT` | 退出校准 |
| `CALIB:DATA?` | 查询校准数据 |
| `RESET_CALIB` | 重置校准 |
| `IP:<esp_ip>` | ESP32 向 STM32 通知当前 IP |

### 9.5 PTZ 控制链路

```text
Frontend
  -> Backend PTZ 控制接口
  -> Esp32PtzControlService
  -> ESP32-CAM /api/ptz/*
  -> ESP32 UartBridge
  -> UART 115200
  -> STM32 UartHandler
  -> PtzServo
  -> SG90 PAN / TILT 舵机
```

### 9.6 ESP32 视频推帧链路

```text
OV2640
  -> ESP32-CAM CameraStreamer
  -> FramePusher
  -> WS /video-hub/cameras/push
  -> video-hub-service
  -> FrameCache
  -> MJPEG / Snapshot / WebRTC
```

## 10. 核心业务链路

### 10.1 登录认证链路

```text
Frontend / Android
  -> Backend 登录接口
  -> AuthServiceImpl
  -> MySQL 用户和角色校验
  -> JwtTokenProvider 签发 Token
  -> 客户端保存 Token
  -> 后续 REST / WebSocket 携带 Token
```

### 10.2 监控任务启动链路

```text
管理员 Frontend
  -> POST /api/monitor/tasks/start
  -> Backend MonitorTaskController
  -> AiStreamTaskServiceImpl
  -> 保存 ai_stream_task STARTING
  -> AiEngineClient
  -> yolo-service /engine/tasks/start
  -> yolo-service 创建任务线程
  -> Backend 更新任务状态 RUNNING
```

### 10.3 视频接入链路

```text
摄像头 / ESP32-CAM / RTSP 源
  -> video-hub-service
  -> VideoHubSession
  -> Source Worker / WebSocket Push
  -> FrameCache
  -> Snapshot / MJPEG / WebRTC
```

### 10.4 AI 检测链路

```text
yolo-service Task Worker
  -> VideoHubClient.fetch_snapshot
  -> video-hub-service Snapshot
  -> OpenCV 解码
  -> YOLO 检测
  -> DeepSort 跟踪
  -> DrowningRuleEvaluator
  -> 实时检测结果 / 风险事件
```

### 10.5 告警生成链路

```text
DrowningRuleEvaluator 触发风险
  -> YOLO Service 构造 event payload
  -> HTTP Callback with HMAC
  -> Backend /api/internal/ai/events
  -> HmacSignatureVerifier
  -> monitoring_event 落库
  -> alert_record 创建
  -> AlertDispatchRoutingService 分配救生员
  -> WebSocket 推送 ALERT_CREATED
  -> 异步 AI 报警分析
```

### 10.6 多端告警推送链路

```text
Backend AlertWsPublisher
  -> /api/ws/alerts
  -> Frontend BackendLayout
  -> 管理端紧急告警弹窗

Backend AlertWsPublisher
  -> /api/ws/alerts
  -> Android RealtimeAlertNotifier
  -> Android 系统通知
  -> HomeScreen 告警弹窗
```

### 10.7 告警处置链路

```text
Android AlarmDetailScreen
  -> RemoteAlarmRepository.submitAlarmAction
  -> Backend /api/alerts/action
  -> AlertActionController
  -> 更新 alert_record / alert_disposal
  -> WebSocket 同步状态
  -> Frontend / Android 刷新告警详情
```

### 10.8 Web 管理端实时监控链路

```text
AdminDashboardView
  -> SUBSCRIBE_MONITOR_REALTIME(cameraIds)
  -> Backend /api/ws/alerts
  -> YOLO Service /api/ws/ai-push
  -> Backend 转发 MONITOR_REALTIME_BATCH
  -> Frontend 合并检测结果
  -> CameraOverlayLayer 绘制检测框
```

### 10.9 视频预览链路

```text
Frontend / Android
  -> Backend /api/streams/cameras/{cameraId}/preview
  -> CameraStreamController
  -> CameraPreviewRouteService
  -> video-hub-service /video-hub/cameras/{cameraId}/stream
  -> MJPEG 输出
```

WebRTC 预览链路：

```text
Frontend WebRtcWhepPlayer
  -> video-hub-service /video-hub/cameras/{cameraId}/whip
  -> Backend camera-source token 校验
  -> video-hub-service WebRTC Session
  -> RTCPeerConnection 播放
```

### 10.10 AI 助手链路

```text
Frontend AiChatPanel
  -> Backend /api/ai/chat/stream
  -> AiChatController
  -> DefaultChatService
  -> Function Calling 查询业务数据
  -> Spring AI ChatClient
  -> OpenAI / DashScope 兼容模型服务
  -> SSE / ReadableStream 返回前端
```

## 11. 数据存储与消息系统

### 11.1 MySQL

Backend 主要业务数据保存在 MySQL。

核心表类型：

- 账号权限：`sys_user`、`sys_role`、`sys_user_role`、`auth_refresh_token`
- 场馆设备：`venue`、`venue_zone`、`camera_device`、`camera_maintenance_log`
- 救生员：`lifeguard`、`lifeguard_duty_log`、`lifeguard_location_log`
- 监控报警：`ai_stream_task`、`monitoring_event`、`alert_record`、`alert_disposal`
- 统计审计：`stats_snapshot`、`system_audit_log`
- AI 智能：`ai_chat_conversation`、`ai_chat_message`、`alert_embedding`

### 11.2 Redis

Redis 在架构中承担多种辅助能力：

- Spring Session / 缓存能力。
- Redisson 分布式能力。
- video-hub-service 摄像头流配置同步：`aqua:camera:streams`。
- video-hub-service 摄像头事件同步：`aqua:camera:events`。

### 11.3 RabbitMQ

RabbitMQ 用于报警事件异步化。

```text
Backend / YOLO Service
  -> alert.topic
  -> alert.record.queue
  -> alert.notification.queue
  -> alert.analytics.queue
```

### 11.4 YOLO Service 本地数据库

YOLO Service 使用 SQLAlchemy，默认 SQLite，也可通过 `DATABASE_URL` 切换到 MySQL。

当前本地数据库主要保留图片/视频离线任务相关模型，实时引擎任务主要使用内存状态 `_TASKS` 管理。

### 11.5 Prometheus

Backend 暴露 Actuator Prometheus 指标：

```text
Prometheus
  -> Backend /api/actuator/prometheus
```

YOLO Service 也可启动 Prometheus 指标服务，用于记录推理次数、延迟、任务状态和报警发布等指标。

## 12. 通信协议汇总

| 协议 | 方向 | 用途 |
|------|------|------|
| REST HTTP | Frontend / Android -> Backend | 业务 API |
| REST HTTP | Backend -> YOLO Service | 推理任务启动、停止、查询 |
| REST HTTP | YOLO Service -> Backend | AI 告警事件回调 |
| REST HTTP | Backend -> video-hub-service | 视频会话、MJPEG、Snapshot、状态查询 |
| REST HTTP | video-hub-service -> Backend | camera-source token 校验和 sourceUrl 解析 |
| WebSocket | Frontend / Android -> Backend | 告警、实时检测、视频帧订阅 |
| WebSocket | YOLO Service -> Backend | AI 实时检测和视频帧推送 |
| WebSocket | ESP32-CAM -> video-hub-service | JPEG 主动推帧 |
| SSE / Fetch Stream | Frontend -> Backend | AI 助手流式输出 |
| MJPEG | Backend / video-hub-service -> Frontend / Android | 摄像头视频预览 |
| WebRTC WHIP | Frontend -> video-hub-service | 低延迟 WebRTC 视频预览 |
| Redis Hash / PubSub | Backend -> video-hub-service | 摄像头流配置同步 |
| RabbitMQ Topic | Backend / YOLO Service -> Consumers | 报警事件异步分发 |
| UART | ESP32-CAM <-> STM32 | PTZ 控制和状态传递 |
| PWM | STM32 -> SG90 | 舵机控制 |
| I2C | STM32 -> OLED | 状态显示 |

## 13. 架构图绘制清单

### 13.1 总体架构图节点

建议绘制以下节点：

- 用户浏览器 / Frontend Web 管理后台
- Android 救生员端
- Backend Spring Boot 业务中心
- YOLO Service AI 推理服务
- video-hub-service 视频网关
- ESP32-CAM / OV2640
- STM32 云台控制器
- SG90 PAN / TILT 舵机
- MySQL
- Redis
- RabbitMQ
- Prometheus
- OpenAI / DashScope 兼容模型服务
- 高德地图服务

建议绘制以下连线：

```text
Frontend -> Backend：REST /api/**
Frontend -> Backend：WebSocket /api/ws/alerts
Frontend -> Backend：SSE /api/ai/chat/stream
Frontend -> video-hub-service：WebRTC WHIP /video-hub/cameras/{id}/whip

Android -> Backend：REST /api/**
Android -> Backend：WebSocket /api/ws/alerts
Android -> Backend：MJPEG /api/streams/cameras/{id}/preview

Backend -> MySQL：业务数据读写
Backend -> Redis：缓存、会话、流配置同步
Backend -> RabbitMQ：报警事件发布
Backend -> YOLO Service：/engine/tasks/start|stop|get
Backend -> video-hub-service：/video-hub/cameras/{id}/stream|snapshot|status
Backend -> ESP32-CAM：/api/ptz/*
Backend -> OpenAI/DashScope：AI 对话、报警分析、Embedding

YOLO Service -> video-hub-service：ensure_session / snapshot
YOLO Service -> Backend：/api/internal/ai/events
YOLO Service -> Backend：WebSocket /api/ws/ai-push
YOLO Service -> RabbitMQ：alert.record

video-hub-service -> Backend：camera-source token 校验
video-hub-service -> Redis：读取 camera streams，订阅 camera events
ESP32-CAM -> video-hub-service：WebSocket JPEG 推帧
ESP32-CAM -> STM32：UART PTZ 指令
STM32 -> SG90：PWM 控制
STM32 -> OLED：I2C 显示
```

### 13.2 Backend 内部架构图节点

- Controller 层
- Auth / Security
- Service 层
- Mapper 层
- WebSocket Handlers
- Messaging / RabbitMQ Publisher & Consumers
- AI Chat / Alert Analysis
- Stream Route / CameraPreviewRouteService
- PTZ Proxy / Esp32PtzControlService
- MySQL
- Redis
- RabbitMQ

核心连线：

```text
Controller -> Service -> Mapper -> MySQL
JwtAuthInterceptor -> AuthContextHolder -> Controller
@AuthCheck -> AuthInterceptor -> AccessControlService
InternalAiCallbackController -> AlertRecordService -> AlertWsPublisher
AlertRecordService -> AlertEventPublisher -> RabbitMQ
AiChatController -> DefaultChatService -> Spring AI ChatClient
CameraStreamController -> CameraPreviewRouteService -> video-hub-service
CameraDeviceController -> Esp32PtzControlService -> ESP32-CAM
```

### 13.3 视频链路图节点

- 摄像头 / RTSP 源
- ESP32-CAM
- video-hub-service
- FrameCache
- MJPEG Stream
- Snapshot
- WebRTC WHIP
- Backend Preview Proxy
- Frontend CameraStreamSurface
- Android WsJpegFramePlayer / MjpegStreamPlayer
- YOLO Service VideoHubClient

核心连线：

```text
摄像头 / RTSP 源 -> video-hub-service Source Worker
ESP32-CAM -> video-hub-service WebSocket Push
Source Worker -> FrameCache
FrameCache -> MJPEG Stream -> Backend -> Frontend / Android
FrameCache -> Snapshot -> YOLO Service
FrameCache -> WebRTC Track -> Frontend
```

### 13.4 AI 推理链路图节点

- Backend MonitorTaskController
- AiStreamTaskServiceImpl
- AiEngineClient
- YOLO Engine Task API
- Engine Task Service
- Task Worker Thread
- VideoHubClient
- OpenCV Decoder
- YOLO Model
- DeepSortTracker
- DrowningRuleEvaluator
- AI WebSocket Push Client
- HTTP Callback Client
- Backend InternalAiCallbackController
- AlertRecordService
- AlertWsPublisher

核心连线：

```text
Backend MonitorTaskController -> AiStreamTaskServiceImpl -> AiEngineClient
AiEngineClient -> YOLO Engine Task API
Engine Task API -> Engine Task Service -> Task Worker Thread
Task Worker Thread -> VideoHubClient -> video-hub-service Snapshot
Task Worker Thread -> OpenCV -> YOLO -> DeepSort -> DrowningRuleEvaluator
DrowningRuleEvaluator -> HTTP Callback Client -> Backend InternalAiCallbackController
DrowningRuleEvaluator -> AI WebSocket Push Client -> Backend AiPushWebSocketHandler
Backend -> AlertRecordService -> AlertWsPublisher -> Frontend / Android
```

### 13.5 告警处置链路图节点

- YOLO Service
- Backend InternalAiCallbackController
- HmacSignatureVerifier
- MonitoringEvent
- AlertRecord
- AlertDispatchRoutingService
- AlertWsPublisher
- Frontend AlarmManagement
- Android RealtimeAlertNotifier
- Android AlarmDetailScreen
- AlertActionController
- AlertDisposal

核心连线：

```text
YOLO Service -> Backend HMAC Callback
Backend -> monitoring_event
Backend -> alert_record
Backend -> AlertDispatchRoutingService
Backend -> WebSocket ALERT_CREATED -> Frontend
Backend -> WebSocket ALERT_CREATED -> Android
Android AlarmDetailScreen -> AlertActionController
AlertActionController -> alert_disposal / alert_record status
```

### 13.6 硬件链路图节点

- 3.7V 锂电池
- TP4056 / 5V 升压模块
- ESP32-CAM
- OV2640
- STM32F103C8T6
- SG90 PAN
- SG90 TILT
- SSD1306 OLED
- PB12 按键
- 电池分压采样电路
- video-hub-service
- Backend

核心连线：

```text
电池 / 升压模块 -> ESP32-CAM 5V
电池 / 升压模块 -> STM32 5V
电池 / 升压模块 -> SG90 PAN/TILT VCC
ESP32-CAM GPIO13 TX -> STM32 PA10 RX
STM32 PA9 TX -> ESP32-CAM GPIO14 RX
STM32 PA6 PWM -> SG90 PAN
STM32 PA7 PWM -> SG90 TILT
STM32 PB6/PB7 I2C -> SSD1306 OLED
STM32 PB12 -> 用户按键 -> GND
电池分压 -> STM32 PA0 ADC
ESP32-CAM -> video-hub-service WebSocket JPEG Push
Backend -> ESP32-CAM HTTP PTZ API
```

## 14. 推荐绘图顺序

建议按以下顺序绘制，避免一张图过于拥挤：

1. 先画“总体架构图”，只保留子系统和外部基础设施。
2. 再画“核心告警链路图”，突出 AI 事件从产生到多端推送的路径。
3. 再画“视频链路图”，单独展示 video-hub-service 的输入、缓存和输出。
4. 再画“Backend 内部架构图”，展示业务中心的分层、鉴权、WebSocket、MQ、AI 助手。
5. 再画“硬件链路图”，展示 ESP32、STM32、舵机、OLED、电源和 UART。
6. 最后画“部署架构图”，结合 `docker-compose.yml` 表示 MySQL、Redis、Backend、video-hub-service、YOLO Service、Frontend 的部署关系。

如果只能画一张总图，建议采用分区布局：

```text
左侧：用户端 Frontend / Android
中间：Backend 业务中心
右侧：YOLO Service / video-hub-service / AI 模型
底部：MySQL / Redis / RabbitMQ / Prometheus
上方或右下：ESP32-CAM / STM32 / 摄像头硬件
```

这样可以保证业务流、视频流、AI 流和硬件控制流在同一张图中方向清晰。
