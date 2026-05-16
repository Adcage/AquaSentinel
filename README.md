# AquaSentinel

水上安全监控系统 -- 面向泳池/场馆场景的实时溺水检测与告警。

---

## 核心亮点

- **全链路 AI 溺水检测**：YOLOv8 目标检测 + DeepSort 多目标跟踪 + 规则引擎判定，从摄像头 RTSP 流到前端报警推送端到端打通
- **四端协同架构**：Backend (Spring Boot) / Frontend (Vue 3) / YOLO Service (Flask) / Android (Jetpack Compose) 各司其职，REST API + WebSocket 双通道通信
- **统一流路由（video-hub-service）**：独立微服务统一处理 RTSP / HTTP-FLV / HTTP-MJPEG 协议接入，PyAV 适配器拉流，Redis Pub/Sub 摄像头状态同步，Backend 不再直接处理视频转码
- **实时报警推送**：YOLO 检测结果经 WebSocket 推至 Backend，再分发给 Frontend 和 Android 端，救生员 APP 实时接收溺水告警
- **前后端 API 自动生成**：后端 Knife4j (Swagger) 输出 OpenAPI 规范，前端 openapi2ts 自动生成 25 个 API Controller + 2484 行类型定义，保持接口一致性
- **策略模式流代理**：StreamProviderRouter 支持 FFmpeg / Javacv / RTSP Direct 三种 MJPEG 转码策略，按配置切换
- **完整权限体系**：JWT 双令牌 + Redis 会话 + 自定义 @AuthCheck AOP 注解，覆盖管理后台与 AI 服务间 HMAC 签名验证
- **已验证协议链路**：HTTP-MJPEG / RTSP 经 video-hub 到前端预览的全链路测试通过（video-hub 74/74、YOLO 52/52 核心测试）

---

## 系统架构

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│  Frontend   │────▶│   Backend   │────▶│  YOLO Service│     │ video-hub   │
│  (Vue 3)    │◀────│  (Spring)   │◀────│  (Flask)     │     │ (Flask/PyAV) │
│             │ WS  │  Port 8300  │ WS  │  Port 5000   │     │  Port 8400   │
└──────┬──────┘     └──────┬──────┘     └──────┬───────┘     └──────┬───────┘
       │                   │                    │                    │
       │            ┌──────┴──────┐             │             ┌──────┴──────┐
       │            │ MySQL + Redis│             │             │ Redis PubSub│
       │            └─────────────┘             │             └─────────────┘
       │                                        │
┌──────┴──────┐                          ┌─────┴──────┐
│   Android   │◀────── REST API ──────────│  Backend   │
│  (Kotlin)   │◀────── WebSocket ──────────│            │
└─────────────┘                          └────────────┘
```

详细架构文档见 [docs/项目现状/architecture.md](docs/项目现状/architecture.md)。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **Backend** | Java 17 / Spring Boot 3.2.3 / MyBatis-Plus 3.5.5 / MySQL / Redis / JWT / WebSocket / Knife4j |
| **Frontend** | Vue 3 (Composition API) / TypeScript / Vite 7 / Pinia / Element Plus 2.11 / ECharts 6 / 高德地图 |
| **YOLO Service** | Python 3 / Flask 3.x / Flask-Smorest / SQLAlchemy / YOLOv8 (ultralytics) / DeepSort / OpenCV / PyAV |
| **Android** | Kotlin / Jetpack Compose (Material 3) / Retrofit 2 / OkHttp / Media3 ExoPlayer / 高德 3D Map SDK |
| **Video Hub** | Python / Flask / PyAV / aiortc / Redis Pub/Sub |
| **Infra** | Maven / npm / pip / Gradle / Node >= 20.19 / MySQL / Redis / FFmpeg (可选) |

完整技术栈分析见 [docs/项目现状/tech-stack.md](docs/项目现状/tech-stack.md)。

---

## 快速开始

各子项目的构建、运行和测试命令见 [docs/运行部署/core-stack-quickstart.md](docs/运行部署/core-stack-quickstart.md)。

> 该文档将在后续补充，当前可参考各子项目目录下的 README 或 `AGENTS.md` 中的构建命令。

---

## 指标摘要

| 指标 | 数值 |
|------|------|
| 代码总行数 | 56,159 |
| 源文件数 | 502 |
| 测试文件 / 测试行数 | 67 / 7,673 |
| video-hub 测试 | 74/74 通过 |
| YOLO 核心测试 | 52/52 通过 |
| Backend 测试 | 46/46 通过 |
| Android 单元测试 | 82/82 通过 |
| 已验证协议链路 | HTTP-MJPEG / RTSP -> video-hub -> 前端预览 |
| 构建产物 (Frontend) | 3.1 MB |
| APK (Android) | 115 MB |

完整基线数据见 [docs/项目现状/baseline-metrics.md](docs/项目现状/baseline-metrics.md)。

---

## 文档导航

| 文档 | 说明 |
|------|------|
| [docs/项目现状/architecture.md](docs/项目现状/architecture.md) | 系统架构详解（分层、数据流、数据库模型） |
| [docs/项目现状/tech-stack.md](docs/项目现状/tech-stack.md) | 技术栈依赖矩阵与详析 |
| [docs/项目现状/baseline-metrics.md](docs/项目现状/baseline-metrics.md) | 可量化基线指标（代码规模、性能、质量） |
| [docs/项目现状/issues-and-optimization.md](docs/项目现状/issues-and-optimization.md) | 问题分析与优化路线图 |