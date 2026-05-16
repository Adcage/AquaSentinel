# AquaSentinel 简历可引用指标摘要

> 本文档整理面向简历/作品集的项目量化指标，严格区分已实测数据与条件性指标，确保所有数字真实可验证。

---

## 1. 已实测指标

以下指标在当前开发环境中稳定复现，可直接引用。

### 1.1 代码规模

| 指标 | 数值 | 说明 |
|------|------|------|
| 总代码行数 | 56,159 行 | 四个子项目合计 |
| 源文件数 | 502 个 | Java 293 + Vue/TS 123 + Python 39 + Kotlin 47 |
| 测试文件数 | 67 个 | 覆盖全部四个子项目 |
| 测试代码行数 | 7,673 行 | 单元测试 + 集成测试 + E2E 测试 |
| API 端点数 | ~188 个 | Backend REST ~183 + YOLO REST ~5 |

### 1.2 子项目规模明细

| 子项目 | 技术栈 | 源文件 | 代码行 | 测试文件 | 测试行 |
|--------|--------|--------|--------|---------|--------|
| Backend | Spring Boot 3 + MyBatis-Plus | 293 | 20,719 | 24 | 1,945 |
| Frontend | Vue 3 + TypeScript + Element Plus | 123 | 25,098 | 16 | 3,273 |
| YOLO Service | Flask + YOLOv8 + DeepSort | 39 | 4,131 | 16 | 1,927 |
| Android | Kotlin + Jetpack Compose | 47 | 6,211 | 11 | 528 |

### 1.3 测试通过情况

| 子项目 | 结果 | 说明 |
|--------|------|------|
| Backend | 46/46 通过 | `mvn test`，含单元测试与集成测试 |
| Android | 82/82 通过 | `./gradlew test`，Kotlin 单元测试 |
| video-hub-service | 74/74 通过 | `pytest`，流媒体网关全部测试 |
| YOLO Service | 52/52 核心测试通过 | 核心业务逻辑测试全部通过（排除依赖外部服务的健康检查用例） |

### 1.4 构建与启动指标

| 指标 | 数值 | 说明 |
|------|------|------|
| Frontend 构建产物 | ~3.1 MB | `npm run build` 后 dist/ 大小 3,155 KB |
| Backend 启动耗时 | ~5.754 秒 | Spring 上下文启动（dev profile） |
| Android APK 大小 | 115 MB | debug 构建产物 |
| Frontend 最大业务 chunk | 1,134.82 KB | StatisticsView 页面，含 ECharts |

### 1.5 已验证协议链路

| 摄像头 | 协议 | 分辨率 | 链路 |
|--------|------|--------|------|
| ESP32-CAM | HTTP MJPEG | 320x240 | ESP32-CAM → video-hub → YOLO 推理 |
| Chicony | RTSP | 1280x720 | Chicony → video-hub → YOLO 推理 |
| Ysd-Anzija | RTSP | 1920x1080 | Ysd-Anzija → video-hub → YOLO 推理 |

三路摄像头均已验证从采集到 AI 推理的完整链路连通性。

### 1.6 架构特征

| 特征 | 描述 |
|------|------|
| 服务数量 | 4 个独立可部署服务 + 1 个流媒体网关 |
| 数据库表数 | 19 张（Backend MySQL） |
| WebSocket 通道 | 3 类：报警推送、AI 推理结果、实时监控帧 |
| 认证体系 | JWT + Redis 令牌管理 + HMAC 服务间签名 |
| 流代理策略 | 3 种：FFmpeg 转码 / Javacv / RTSP Direct |
| AI 推理管线 | YOLOv8 目标检测 → DeepSort 多目标跟踪 → 溺水规则判定引擎 |

---

## 2. 条件性指标

以下指标依赖真实运行环境（GPU、网络、数据库负载等），需在部署后实测。标注了测量前提和推荐方法。

| 指标 | 单位 | 测量前提 | 推荐采集方法 |
|------|------|---------|-------------|
| GPU 推理延迟（单帧） | ms | NVIDIA GPU + YOLOv8 模型加载 | YOLO Service 日志 `inference_time` 字段 |
| CPU 推理延迟（单帧） | ms | 无 GPU 环境，CPU fallback | 同上，对比 GPU/CPU 环境 |
| 实际处理 FPS | fps | 摄像头推流 + GPU 推理运行 | YOLO Service 日志帧率统计 |
| 多路并发上限 | 路 | GPU 显存确定，多路同时推理 | 逐步增加推理任务数至显存溢出 |
| Android 冷启动时间 | 秒 | 真机安装 + 首次启动 | Android Studio Profiler / `adb shell am start -W` |
| Android 端帧率 | fps | 真机 + WebSocket 视频流连接 | 帧时间戳差值统计 |
| WebSocket 推送延迟 | ms | Backend + YOLO Service + 客户端同时运行 | 端到端时间戳差值 |
| JVM 堆内存占用 | MB | Backend 运行态 | `jcmd <pid> GC.heap_info` |
| 分页查询 P95 响应 | ms | MySQL 有数据 | `ab -n 1000 -c 10` 或 JMeter |
| Frontend 首屏 LCP | 秒 | Nginx 部署 + 真实网络 | Chrome Lighthouse |

---

## 3. 指标采集前提

### 3.1 已实测指标的环境

| 指标类别 | 采集环境 | 采集时间 | 采集方法 |
|---------|---------|---------|---------|
| 代码规模 | 离线统计，无环境依赖 | 项目当前快照 | `cloc` / `find + wc` 按子项目统计 |
| 测试通过数 | 各子项目开发环境 | 最近一次 `mvn test` / `pytest` / `./gradlew test` 执行 | 各语言标准测试运行器 |
| Frontend 构建产物 | Node >= 20.19，`npm run build` | 最近一次构建 | `du -sh dist/` |
| Backend 启动耗时 | Java 17，dev profile，本地 MySQL + Redis | `MainApplicationTests` 启动计时 | Spring Boot 启动日志 |
| 协议链路验证 | 实物摄像头 + 局域网 | 手动联调测试 | video-hub 日志 + YOLO 推理日志确认帧到达 |
| API 端点数 | 离线代码扫描 | 静态分析 | 统计 `@RequestMapping` / `@GetMapping` 等注解 |

### 3.2 条件性指标的采集要求

| 指标 | 必要条件 | 阻塞因素 |
|------|---------|---------|
| GPU 推理延迟 | NVIDIA GPU + CUDA + 模型权重文件 | 无 GPU 或无模型文件时无法测量 |
| 实际 FPS | 摄像头持续推流 + 推理任务运行 | 需完整部署链路 |
| 多路并发上限 | GPU 显存 >= 模型需求 × N | 受硬件限制 |
| Android 冷启动 | 真机或模拟器 + APK 安装 | 需 Android 运行环境 |
| WebSocket 推送延迟 | Backend + YOLO + 客户端三方在线 | 需完整服务部署 |
| 数据库查询性能 | MySQL 有真实数据量 | 空库无法反映真实性能 |

---

## 4. 简历写法示例

### 示例一：偏工程落地

> 主导开发水上安全监控系统 AquaSentinel，覆盖从摄像头接入到 AI 推理到多端报警推送的完整链路。系统由 4 个微服务组成（Spring Boot / Flask / Vue 3 / Kotlin），共 56K 行代码、188 个 API 端点、67 个测试文件。已验证 ESP32-CAM（MJPEG 320x240）和两路 RTSP 摄像头（720p/1080p）经自研流媒体网关到 YOLOv8+DeepSort 推理的端到端连通；Backend 启动耗时约 5.8 秒，Frontend 构建产物约 3.1 MB。

### 示例二：偏技术深度

> 独立完成水上安全监控系统的全栈开发：后端 Spring Boot 3（JWT 认证、WebSocket 三通道实时推送、流代理策略模式）、AI 推理服务 Flask+YOLOv8+DeepSort（溺水规则判定引擎、多目标跟踪）、PC 管理后台 Vue 3+TypeScript（ECharts 可视化、高德地图集成）、移动端 Kotlin+Jetpack Compose（双 WebSocket 通道实时报警与视频帧）。项目 502 个源文件、7,673 行测试代码，video-hub-service 74/74 测试通过，Android 82/82 测试通过。

---

## 5. 使用原则

1. **只引用已实测指标**：简历中出现的数字必须来自第 1 节，不得将条件性指标包装为已验证数据
2. **条件性指标需标注前提**：如引用第 2 节指标，必须同时说明测量环境（如"在 NVIDIA T4 GPU 环境下"）
3. **数字取近似值**：简历中建议使用约数（如"约 56K 行"而非"56,159 行"），避免过度精确暗示不存在的测量精度
4. **定期更新**：项目迭代后需重新采集基线数据，避免引用过时数字
