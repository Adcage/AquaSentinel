# AquaSentinel 系统架构文档

## 1. 系统概览

AquaSentinel 是水上安全监控系统，用于游泳场馆的溺水检测、报警推送、救生员管理及设备监控。系统由四个独立部署的子服务组成，通过 REST API 和 WebSocket 通信。

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   Frontend  │────▶│   Backend   │────▶│  YOLO Service │
│  (Vue 3)    │◀────│  (Spring)   │◀────│  (Flask)     │
│  Port: ?    │ WS  │  Port: 8300 │ WS  │  Port: 5000  │
└──────┬──────┘     └──────┬──────┘     └──────┬───────┘
       │                   │                    │
       │            ┌──────┴──────┐             │
       │            │   MySQL     │             │
       │            │   Redis     │             │ GPU / Model
       │            └─────────────┘             │
       │                                        │
┌──────┴──────┐                          ┌─────┴──────┐
│   Android   │◀────── REST API ──────────│  Backend   │
│  (Kotlin)   │◀────── WebSocket ────────│            │
└─────────────┘                          └────────────┘
```

### 1.1 服务职责

| 服务 | 职责 | 端口 | 部署方式 |
|------|------|------|----------|
| **Backend** | 业务核心：用户认证、权限管理、CRUD、报警分发、统计聚合、流网关 | 8300 | JAR |
| **Frontend** | PC 端管理后台：场馆管理、设备监控、报警处理、数据分析 | 开发服务器 / Nginx | 静态资源 |
| **YOLO Service** | AI 推理引擎：视频流接收、目标检测（YOLOv8+DeepSort）、溺水判定、WebSocket 推送结果 | 5000 | Python 进程 |
| **Android** | 移动端 APP：救生员实时报警接收、监控视频预览、巡检签到 | N/A | APK |

### 1.2 数据流路径

**报警主流程**：

```
摄像头 RTSP → YOLO Service (拉流+推理)
                    ↓
              溺水规则判定 (DrowningRuleEvaluator)
                    ↓
              WebSocket → Backend /ws/ai-push
                    ↓
              Backend 写入数据库 + WebSocket → Frontend/Android
```

**监控预览流程**：

```
Frontend → Backend /streams/cameras/{id}/preview
                    ↓
              StreamProviderRouter → FFmpeg/Javacv/RTSP Direct
                    ↓
              MJPEG 帧 → Frontend <img> 标签
```

---

## 2. Backend 架构

### 2.1 分层架构

```
┌──────────────────────────────────────────┐
│  Controller (25 个)                      │  ← REST API 入口
│  @RestController + @AuthCheck 权限控制    │
├──────────────────────────────────────────┤
│  Service 接口 (19) + Impl (32)           │  ← 业务逻辑层
│  含 Push/Stream 策略子包                  │
├──────────────────────────────────────────┤
│  Mapper (19) extends BaseMapper<T>       │  ← MyBatis-Plus 数据访问
├──────────────────────────────────────────┤
│  Model                                   │
│  ├─ entity/ (19)  @TableName + snake_case│  ← 数据库实体
│  ├─ dto/    (81)  按 [Entity][Action]Request│  ← 请求参数
│  └─ vo/     (19)  [Entity]VO camelCase    │  ← 响应视图
└──────────────────────────────────────────┘
```

### 2.2 横切关注点

| 关注点 | 实现 | 包路径 |
|--------|------|--------|
| **认证** | JWT（jjwt 0.12.5）+ 刷新令牌 | `security/` |
| **授权** | `@AuthCheck(mustRole)` + AOP 拦截 | `annotation/` + `aop/` |
| **异常** | `BusinessException` + `GlobalExceptionHandler` → `BaseResponse<T>` | `exception/` |
| **日志** | AOP 日志拦截 + 系统审计日志 | `aop/LogInterceptor` + `aop/SystemAuditLogInterceptor` |
| **请求追踪** | `RequestIdFilter` → ThreadLocal → BaseResponse.requestId | `config/RequestIdFilter` |
| **WebSocket** |.AlertWebSocketHandler（推送报警）+ AiPushWebSocketHandler（接收 AI 结果）+ MonitorRealtimeWsPublisher | `websocket/` |
| **流代理** | StreamProvider 策略模式（FFmpeg / Javacv / RTSP Direct） | `service/stream/` |

### 2.3 数据库模型

共 19 张表，核心实体关系：

```
sys_user ──M:N── sys_role          用户-角色关联
  │                                 │
  └── venue (1:N) ── zone (1:N)    场馆-区域层级
                         │
              camera_device (1:N)   摄像头
              lifeguard (1:N)       救生员
                   │
         ┌─────────┴──────────┐
   lifeguard_duty_log   lifeguard_location_log  签到/轨迹
         
monitoring_event ──1:N── alert_record            检测事件→报警
                              │
                    alert_disposal              报警处置
                    
ai_stream_task                                  AI 推理任务
system_audit_log / stats_snapshot               审计日志/统计快照
```

数据库约定：
- 主键：`BIGINT AUTO_INCREMENT`
- 软删除：`is_delete TINYINT DEFAULT 0`
- 时间戳：`created_at / updated_at DATETIME DEFAULT CURRENT_TIMESTAMP`
- 列名：snake_case，表注释中文
- 索引：`uk_` 唯一索引，`idx_` 普通索引

---

## 3. Frontend 架构

### 3.1 分层架构

```
┌─────────────────────────────────────────┐
│  Views (24) + Dialogs (9)              │  ← 页面与弹窗
│  admin/, Dashboard, Login, Register     │
├─────────────────────────────────────────┤
│  Composables (useAMap, useVenue...)     │  ← 组合式函数
├─────────────────────────────────────────┤
│  Services (12) ← API 门面层            │  ← 业务逻辑 + 数据转换
│  authService, dashboardService, ...     │
├─────────────────────────────────────────┤
│  API Controllers (25) ← 自动生成       │  ← HTTP 请求封装
│  openapi2ts 生成, typings.d.ts (2484行) │
├─────────────────────────────────────────┤
│  Request (Axios 实例)                   │  ← 拦截器: Token/401/错误
├─────────────────────────────────────────┤
│  Stores (2) ← Pinia                    │  ← 全局状态（venueStore 等）
└─────────────────────────────────────────┘
```

### 3.2 组件分层

| 层级 | 目录 | 文件数 | 说明 |
|------|------|--------|------|
| 业务组件 | `components/business/` | 5 | CameraGridCard, PageTable, MetricCard 等 |
| 通用组件 | `components/common/` | 2 | EmptyState, StatusTag |
| 图表组件 | `components/dashboard/` | 5 | BarChart, LineChart, MiniChart, PieChart, StatCard |
| 图标组件 | `components/icons/` | 1 | IconCampusPoint（自定义 SVG） |
| 导航 | `components/NavBar.vue` | 1 | 顶部导航栏 |

### 3.3 关键数据流

- **认证流**：Login → JWT Token → sessionStorage → Axios 拦截器自动附加
- **报警推送**：WebSocket `/ws/alert` → `alertWsService` → `window.dispatchEvent` → 组件监听
- **AI 推理状态**：WebSocket `/ws/ai-push` → Backend ← YOLO Service
- **地图集成**：高德地图 JS API（`@amap/amap-jsapi-loader`）
- **数据可视化**：ECharts（6 种图表类型）

---

## 4. YOLO Service 架构

### 4.1 分层架构

```
┌─────────────────────────────────────────┐
│  API Layer (Flask-Smorest Blueprint)   │  ← REST 端点 + Marshmallow 验证
│  engine_tasks, health, test_trigger     │
├─────────────────────────────────────────┤
│  Service Layer (10 个核心服务)          │  ← 业务逻辑
│  engine_task_service (1049行)           │     推理任务生命周期管理
│  drowning_rule_service                  │     溺水规则判定引擎
│  model_inference_service                │     YOLOv8 模型加载/推理
│  tracker_service                        │     DeepSort 多目标跟踪
│  ai_ws_push_service                     │     WebSocket 推送客户端
│  video_overlay_service                  │     检测框绘制/视频推帧
│  callback_client_service                │     HTTP 回调 + 签名
├─────────────────────────────────────────┤
│  Repository Layer (5)                   │  ← SQLAlchemy CRUD
├─────────────────────────────────────────┤
│  Models (6)                             │  ← ORM 实体
│  video_task, video_detection,            │
│  image_task, image_detection, image_report│
└─────────────────────────────────────────┘
```

### 4.2 推理任务生命周期

```
[start] → PENDING → RUNNING → [stop] → STOPPED
                ↓
            ERROR (自动重连)
```

- 任务管理使用内存字典 `_TASKS: dict[str, EngineTaskState]` + 线程锁
- 每个任务包含独立读帧线程 + 推理线程 + 推帧线程
- 模型使用全局单例 + 读写锁 `_MODEL_LOCK`

### 4.3 模块系统

```python
MODULE_REGISTRY = {
    "health": HealthModule,      # 内置
    "auth": None,                # 空壳
    "file_upload": None,          # 空壳
    "pdf_report": None,           # 空壳
}
```

当前仅 `health` 模块实装，其余为预留空壳。

---

## 5. Android 架构

### 5.1 分层架构

```
┌─────────────────────────────────────────┐
│  UI Screens (6 页面)                    │  ← Jetpack Compose
│  Home, AlarmCenter, AlarmDetail,         │
│  VideoList, Location, Profile, Record    │
├─────────────────────────────────────────┤
│  UI Components (18+)                   │  ← 可复用 Composable
│  AlarmDialog, VideoStreamPlayer,        │
│  AppBottomBar, AMapView, ...           │
├─────────────────────────────────────────┤
│  Data Layer                             │
│  ├─ remote/ (ApiClient, ApiService)     │  ← Retrofit + OkHttp
│  ├─ alert/ (RealtimeAlertNotifier)      │  ← WebSocket 报警推送
│  └─ stream/ (MonitorRealtimeFrameWsClient)│ ← WebSocket 视频帧
├─────────────────────────────────────────┤
│  Repository Layer (object 单例)         │  ← 无 ViewModel，直接调用
│  RemoteHomeRepository, RemoteAlarmRepo.. │
└─────────────────────────────────────────┘
```

### 5.2 关键特征

- **无 ViewModel**：Compose 直接通过 `produceState` / `LaunchedEffect` 调用 Repository
- **无 DI 框架**：所有仓库为 `object` 单例，懒加载初始化
- **双 WebSocket 通道**：报警实时推送 + 视频帧流
- **认证**：SharedPreferences 存储 JWT + 用户信息