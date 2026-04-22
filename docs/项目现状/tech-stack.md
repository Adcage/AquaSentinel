# AquaSentinel 技术栈分析文档

## 1. 技术栈全景

### 1.1 依赖矩阵

| 维度 | Backend | Frontend | YOLO Service | Android |
|------|---------|----------|--------------|---------|
| **语言** | Java 17 | TypeScript 5.9 | Python 3.x | Kotlin |
| **框架** | Spring Boot 3.2.3 | Vue 3.5 | Flask 3.x | Jetpack Compose |
| **构建** | Maven | Vite 7 / npm | pip | Gradle (Kotlin DSL) |
| **数据库** | MySQL + Redis | - | SQLite (开发) | - |
| **ORM** | MyBatis-Plus 3.5.5 | - | SQLAlchemy 2.x | - |
| **HTTP 客户端** | - | Axios 1.13 | Requests | Retrofit 2 + OkHttp |
| **实时通信** | Spring WebSocket | 原生 WebSocket | websocket-client | OkHttp WebSocket |
| **认证** | jjwt 0.12.5 | sessionStorage | HMAC 签名 | SharedPreferences |
| **地图** | - | 高德 JS API | - | 高德 3D Map SDK |
| **图表** | - | ECharts 6 | - | - |
| **UI 库** | - | Element Plus 2.11 | - | Material 3 |
| **AI/ML** | - | - | ultralytics 8.x + DeepSort | - |
| **视频处理** | FFmpeg/Javacv | html2canvas + jspdf | OpenCV + av | Media3 ExoPlayer |
| **API 文档** | Knife4j (Swagger) | openapi2ts (自动生成) | Flask-Smorest | - |
| **测试** | Spring Boot Test | Vitest 3 + @vue/test-utils | pytest | JUnit 4 |

### 1.2 依赖数量统计

| 子项目 | 直接依赖数 | 关键依赖 |
|--------|-----------|----------|
| Backend (pom.xml) | 12 | Spring Boot Starter (web/ws/aop/redis/test), MyBatis-Plus, jjwt, Knife4j, Hutool, EasyExcel |
| Frontend (package.json) | 运行时 11 + 开发 9 | Vue, Element Plus, ECharts, Pinia, vue-router, axios, dayjs, xlsx |
| YOLO Service (requirements.txt) | 27 | Flask, flask-smorest, marshmallow, ultralytics, opencv, deep-sort-realtime, SQLAlchemy |
| Android (build.gradle.kts) | 15+ | Compose BOM 2024.09, Navigation 2.8, Paging 3.3, Media3, Retrofit, AMap |

---

## 2. Backend 技术详析

### 2.1 核心框架组件

```
Spring Boot 3.2.3
├── spring-boot-starter-web          ← REST API
├── spring-boot-starter-websocket    ← 双向实时通信
├── spring-boot-starter-aop          ← @AuthCheck 权限拦截
├── spring-boot-starter-data-redis   ← Session + 缓存
├── spring-session-data-redis        ← 分布式会话
├── mybatis-plus-spring-boot3-starter  ← ORM + 分页
├── knife4j-openapi3-jakarta         ← API 文档
├── jjwt-api / jjwt-impl / jjwt-jackson  ← JWT 认证
├── hutool-all                       ← 工具类库
├── easyexcel                        ← Excel 导出
└── mysql-connector-j                 ← 数据库驱动
```

### 2.2 认证与授权

- **JWT 双令牌**：Access Token + Refresh Token
- **Redis 会话**：`spring-session-data-redis` 管理会话
- **自定义注解**：`@AuthCheck(mustRole = RoleConstant.XXX)` + AOP 拦截
- **HMAC 签名**：YOLO Service 调用 Backend 时使用 `HmacSignatureVerifier`

### 2.3 流媒体代理

采用策略模式，`StreamProviderRouter` 根据配置选择：

| Provider | 实现 | 特点 |
|----------|------|------|
| `ffmpeg` | `FfmpegMjpegStreamProvider` | FFmpeg 进程转码，质量高但进程开销大 |
| `javacv` | `JavacvMjpegStreamProvider` | Java 进程内转码，无需外部 FFmpeg |
| `rtsp_direct` | `DirectRtspStreamProvider` | 直透传 RTSP，延迟最低但浏览器兼容差 |

### 2.4 数据模型映射

```
数据库 (snake_case) ←→ Entity (snake_case) ←→ DTO/VO (camelCase)
     ↑                    ↑                        ↑
  @TableField         MyBatis-Plus              手动 getter/setter
  value="alert_uid"    自动映射                   转换在 Service 层
```

**已知问题**：Entity 字段使用 snake_case（如 `alert_uid`）而非 Java 惯例的 camelCase（如 `alertUid`），导致 Lombok 生成的 getter/setter 也是 `getAlert_uid()` 而非 `getAlertUid()`，降低代码可读性。

---

## 3. Frontend 技术详析

### 3.1 构建与开发流程

```
开发流程：
  npm run dev        ← Vite 开发服务器 + /api 代理到 localhost:8300
  npm run openapi    ← 从后端 Swagger 生成 API 客户端代码到 src/api/

构建产物：
  npm run build      ← 静态资源到 dist/
```

### 3.2 API 三层架构

```
openapi2ts 自动生成 (src/api/)
    ↓ 解封装
业务服务层 (src/services/)
    ↓ 调用
组合式函数 & 视图 (src/composables/ & src/views/)
    ↓ unwrapApiData() 提取 data
数据消费
```

**关键特性**：
- `api/typings.d.ts`：2484 行自动生成的 `API` 命名空间类型定义
- `api/controller`：25 个自动生成的 API 调用模块
- `services/serviceUtils.ts`：`unwrapApiData()` 统一解包 `BaseResponse<T>` 信封

### 3.3 实时数据架构

```
YOLO Service ──WS──→ Backend /ws/ai-push
                         │
                    AlertWebSocketHandler
                         │
           ┌─────────────┼─────────────┐
           ↓             ↓              ↓
      Frontend WS    Android WS    数据库写入
      /ws/alert      (报警推送)    alert_record
```

- Frontend 通过 `window.dispatchEvent(new CustomEvent(...))` 跨组件传递 WebSocket 事件
- 仅使用 2 个 Pinia Store，大部分状态管理在组件内完成

### 3.4 主题系统

```css
:root {
  --color-primary: #1b4f9b;      /* 主色-深蓝 */
  --color-bg-page: #f0f2f5;      /* 页面背景 */
  --color-border: #e8e8e8;       /* 边框色 */
  --shadow-card: 0 2px 8px rgba(0, 0, 0, 0.08);
  /* ... 共 15+ 设计令牌 */
}
```

---

## 4. YOLO Service 技术详析

### 4.1 推理管线

```
RTSP 摄像头流
    ↓ (av 库拉流)
读帧线程 (_capture_loop)
    ↓ (threading.Event 帧同步)
推理线程 (_inference_loop)
    ↓ (YOLOv8 → DeepSort)
检测结果 + 跟踪 ID
    ↓ (DrowningRuleEvaluator 规则引擎)
溺水判定
    ↓ (WebSocket / HTTP 回调)
Backend 推送
```

### 4.2 模型与跟踪

| 组件 | 版本 | 说明 |
|------|------|------|
| YOLOv8 | ultralytics ≥ 8.2 | 目标检测，支持多模型版本热切换 |
| DeepSort | deep-sort-realtime ≥ 1.3 | 多目标跟踪，分配持久 track ID |
| 备用跟踪器 | `_SimpleIouTracker` | IoU 匹配的轻量跟踪器，DeepSort 不可用时的 fallback |

### 4.3 进程模型

- **单进程多线程**：每个推理任务创建 3 个线程（读帧、推理、推帧）
- **全局模型单例**：`_shared_model` + `_MODEL_LOCK` 读写锁，所有任务共享同一模型实例
- **任务状态字典**：`_TASKS: dict[str, EngineTaskState]`，纯内存，进程重启即丢失

### 4.4 响应信封

```python
# 成功响应
success_payload(data) → {"code": "OK", "message": "ok", "data": ..., "request_id": "..."}

# 错误响应
error_payload(code, message) → {"code": "BUSINESS_ERROR", "message": "...", "data": {}, "request_id": "..."}
```

**与 Backend 信封差异**：

| 维度 | Backend | YOLO Service |
|------|---------|--------------|
| 成功 code | `0`（整数） | `"OK"`（字符串） |
| 错误 code | `40000`-`50001`（五位数） | `"BUSINESS_ERROR"` / `"PARAM_ERROR"` / `"SYSTEM_ERROR"` |
| data 字段 | 泛型 T | 任意 JSON |
| request_id | ThreadLocal 字符串 | 请求中间件注入 |

---

## 5. Android 技术详析

### 5.1 依赖结构

```
Jetpack Compose BOM 2024.09
├── material3                          ← UI 组件
├── navigation-compose 2.8.9           ← 页面导航
├── paging-compose 3.3.5               ← 分页加载
├── lifecycle-runtime-ktx 2.6.1        ← 生命周期
└── activity-compose 1.8.0              ← Activity 桥接

Media3
├── media3-exoplayer                    ← 视频播放
├── media3-exoplayer-rtsp              ← RTSP 协议支持
└── media3-ui                          ← 播放器 UI

网络
├── retrofit2 + converter-gson         ← REST API
├── okhttp3 + logging-interceptor       ← HTTP 客户端
└── okhttp3-ws                         ← WebSocket

地图
└── amap-3dmap 9.8.3                   ← 高德地图
```

### 5.2 双 WebSocket 通道

| 通道 | 用途 | 地址 | 数据格式 |
|------|------|------|----------|
| 报警推送 | 实时接收溺水报警 | Backend `/ws/alert` | JSON（报警详情） |
| 视频帧流 | 实时监控画面 | Backend `/ws/monitor/realtime` | JPEG 二进制帧 |

### 5.3 状态管理特征

```kotlin
// 无 ViewModel，直接 produceState
val state by produceState(initialValue = ..., key1 = refreshVersion) {
    value = RemoteHomeRepository.getHomeUiState()
}
```

- 配置变更（如屏幕旋转）会丢失状态
- 所有 Repository 为 `object` 单例，无依赖注入

---

## 6. 基础设施依赖

| 组件 | 版本 | 用途 |
|------|------|------|
| **MySQL** | 未锁定 | 主数据库（19 张表） |
| **Redis** | 未锁定 | Session 存储 + 缓存 |
| **FFmpeg** | 外部进程 | 流媒体转码（可选） |
| **YOLOv8 模型** | ≥ 8.2 | 目标检测推理 |
| **Node.js** | ≥ 20.19 或 ≥ 22.12 | 前端构建 |
| **Python** | 未锁定 | YOLO Service 运行时 |
| **Java** | 17 | Backend 运行时 |
| **Android SDK** | min 29 / target 36 | Android 运行时 |

### 6.1 环境变量

| 服务 | 关键环境变量 | 默认值 |
|------|-------------|--------|
| Backend | `spring.datasource.*`, `spring.redis.*`, `jwt.secret` | application.yml |
| Frontend | `VITE_API_BASE_URL`, `VITE_CAMERA_PREVIEW_MODE`, `VITE_AMAP_KEY` | /api, backend_proxy, - |
| YOLO Service | `SQLALCHEMY_DATABASE_URI`, `RECOGNITION_USE_FAKE_MODEL`, `ENABLED_MODULES` | sqlite, False, health |
| Android | `BASE_URL`, AMap API Key (gradle.properties) | 192.168.0.181:8300 |