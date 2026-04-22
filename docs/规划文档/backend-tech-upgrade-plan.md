# AquaSentinel 后端技术栈升级规划

> 版本：v1.0 | 日期：2026-04-23
>
> 本文档详细规划后端（Backend + YOLO Service）的技术栈升级，涵盖每个技术的引入目的、使用方向、架构设计、设计模式应用，以及阶段划分。前端和 Android 的改造不在此文档范围内。

---

## 1. 总体架构愿景

### 1.1 当前架构

```
YOLO Service ──WebSocket──▶ Backend ──WebSocket──▶ Frontend
                                      ──WebSocket──▶ Android
                                      ──HTTP回调───▶ YOLO Service
                    │
                    └── �线即丢，无线重试，无持久化
```

**核心问题**：
- 报警事件通过 WebSocket 直传，断线即丢
- 没有观测能力，出问题只能翻日志猜
- 没有重试/熔断机制，外部调用失败直接抛异常
- 没有限流，硬件控制无法防护
- 没有 AI 能力，报警描述靠模板拼接

### 1.2 目标架构

```
                              ┌── RabbitMQ ──┐
                              │              │
YOLO Service ──publish──▶ 报警Exchange ──▶ 报警队列 ──▶ Backend 消费 ──▶ 写库/推送/分析
                              │              │
                              └── 设备控制Exchange ──▶ 控制队列 ──▶ Backend 消费 ──▶ ESP32-CAM
                                              │
                              ┌── 通知Exchange ──▶ 通知队列 ──▶ 通知服务 ──▶ 钉钉/企微/短信

Backend ──Micrometer──▶ Prometheus ──▶ Grafana 仪表盘
Backend ──Spring AI──▶ LLM API（智能分析/自然语言查询）
Backend ──Resilience4j──▶ 重试/熔断/限流
Backend ──Caffeine──▶ L1 本地缓存 ──▶ L2 Redis ──▶ MySQL

OpenClaw（外围）──▶ 调用 Backend API / 查询 Prometheus ──▶ 用户通过 QQ/微信/钉钉对话管理
```

### 1.3 架构原则

| 原则 | 说明 |
|------|------|
| **职责分离** | 消息投递、业务逻辑、AI 分析、通知推送各守边界，不越权 |
| **面向接口编程** | 所有新增模块通过接口定义契约，具体实现可替换 |
| **开闭原则** | 新增功能通过扩展（新类、新配置）而非修改现有代码 |
| **可观测先行** | 每个新功能先暴露指标，再写业务逻辑 |
| **渐进式接入** | 每个技术可独立引入、独立撤除，不产生硬耦合 |

---

## 2. 阶段 1：消息可靠投递 + 系统可观测 + 代码规范化

### 2.1 RabbitMQ — 消息可靠投递

#### 2.1.1 引入目的

当前 YOLO Service 通过 WebSocket 直连 Backend 推送 AI 检测结果，存在以下问题：

1. **报警丢失**：WebSocket 断开或 Backend 重启期间，所有报警事件丢失，无重试机制
2. **无顺序保障**：重连后事件到达顺序不可控
3. **无审计追溯**：断线期间丢失的事件无法回补

引入 RabbitMQ 后，YOLO Service 将报警事件发布到 Exchange，Backend 作为消费者从 Queue 消费。即使 Backend 宕机，消息在 RabbitMQ 中持久化等待，重启后继续消费。

#### 2.1.2 使用方向

| Exchange / Queue | 用途 | 生产者 | 消费者 | 消息类型 |
|-----------------|------|--------|--------|----------|
| `alert.topic` → `alert.record.queue` | 溺水报警事件 | YOLO Service | Backend AlertConsumer | 溺水检测、区域入侵等 |
| `alert.topic` → `alert.notification.queue` | 报警通知推送 | YOLO Service | Backend NotificationConsumer | 钉钉/企微/短信通知 |
| `alert.topic` → `alert.analytics.queue` | 报警统计分析 | YOLO Service | Backend AnalyticsConsumer | 统计聚合、趋势分析 |
| `device.control.direct` → `device.control.{deviceId}.queue` | 设备控制指令（后续） | Backend | Backend DeviceControlConsumer | PTZ 控制、预置位等 |
| `device.event.topic` → `device.status.queue` | 设备状态上报（后续） | ESP32-CAM | Backend DeviceStatusConsumer | 心跳、在线离线 |

#### 2.1.3 设计模式应用

**模式一：发布-订阅模式（Publish-Subscribe）**

报警事件通过 `alert.topic` Exchange 广播，3 个队列各绑定自己的 routing key，实现：
- 同一条报警事件同时写入数据库、推送通知、参与统计分析
- 三个消费者独立处理，互不阻塞
- 新增消费者只需绑定新队列，不修改生产者代码

```
YOLO Service ──publish──▶ alert.topic Exchange
                               │
                  ┌────────────┼────────────┐
                  ▼            ▼             ▼
          alert.record   alert.notification  alert.analytics
          .queue         .queue             .queue
                  │            │             │
                  ▼            ▼             ▼
             写入数据库    推送钉钉/企微   更新统计快照
```

**模式二：策略模式（Strategy）— 消息序列化**

```
MessageSerializer（接口）
├── JsonMessageSerializer        ← 默认，通用
├── ProtobufMessageSerializer    ← 如果后续需要高性能序列化
└── AvroMessageSerializer        ← 如果需要 Schema 演进
```

当前只用 JSON 序列化，但通过策略模式预留切换能力。YOLO Service 侧使用 Python 的 `pika` 库发布消息，Backend 侧使用 Spring AMQP 消费。

**模式三：死信队列模式（Dead Letter Queue）**

```
alert.record.queue
    │ 消费失败（重试 3 次仍失败）
    ▼
alert.record.dlq (Dead Letter Queue)
    │
    ▼
人工处理 / 告警通知 / 自动重试策略
```

配置方式：
- 消息设置 TTL + 重试次数上限
- 超过重试上限的消息自动进入死信队列
- 死信队列消费者发送告警通知，运维人员在 Grafana 或 OpenClaw 中查看

**模式四：幂等消费者模式（Idempotent Consumer）**

每条消息携带全局唯一 `messageId`（UUID），Backend 消费前先查 Redis 判断是否已处理：

```
消费消息 → Redis SET message:processed:{messageId} NX EX 86400
         → 如果设置成功：处理消息
         → 如果已存在：跳过（幂等）
```

防止消息重投导致重复报警写入数据库。

#### 2.1.4 YOLO Service 侧改造

YOLO Service 当前使用 `websocket-client` 直连 Backend 的 `/ws/ai-push`。改造方案：

**保留 WebSocket 作为实时推送通道**（前端仍然需要实时更新），**新增 RabbitMQ 作为持久化保障通道**：

```
YOLO Service 检测到溺水：
    1. 发布到 RabbitMQ alert.topic（持久化保障）
    2. 同时通过 WebSocket 推送（实时性）
    
Backend 消费 alert.record.queue：
    1. 写入数据库
    2. 如果 WebSocket 已经推送过，幂等跳过
    3. 如果 WebSocket 未推送（断线期间），重新推送
```

这样既保证实时性，又保证可靠性。WebSocket 退化为"实时通知通道"，RabbitMQ 作为"持久化兜底通道"。

YOLO Service 侧需要引入的 Python 依赖：
```
pika>=1.3.0  # RabbitMQ Python 客户端
```

新增模块（Python 侧）：
```python
# app/services/rabbitmq_publisher_service.py
# 作用：将检测事件发布到 RabbitMQ，替代部分 WebSocket 推送职责
# 使用 pika.BlockingConnection，连接断开自动重连
# 发布确认（publisher confirms）保证消息到达 Broker
```

#### 2.1.5 Backend 侧改造

新增 Maven 依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

新增包结构：
```
com.springboot
├── messaging/                    ← 新增：消息相关
│   ├── config/
│   │   └── RabbitMQConfig.java            ← Exchange/Queue/Binding 声明
│   ├── consumer/
│   │   ├── AlertRecordConsumer.java        ← 报警事件消费 → 写库
│   │   ├── AlertNotificationConsumer.java   ← 报警通知消费 → 推送
│   │   └── AlertAnalyticsConsumer.java      ← 统计分析消费
│   ├── publisher/
│   │   └── DeviceControlPublisher.java      ← 设备控制指令发布
│   ├── serializer/
│   │   ├── MessageSerializer.java           ← 序列化接口
│   │   └── JsonMessageSerializer.java        ← JSON 实现
│   ├── model/
│   │   ├── AlertEventMessage.java           ← 报警事件消息体
│   │   └── DeviceControlMessage.java         ← 设备控制消息体
│   └── errorHandler/
│       └── DeadLetterHandler.java            ← 死信队列处理
```

#### 2.1.6 可维护性保障

| 保障项 | 具体措施 |
|--------|---------|
| 配置外部化 | Exchange 名称、Queue 名称、Routing Key 全部写入 `application.yml`，支持按环境切换 |
| 消费者可开关 | 每个消费者通过 `@ConditionalOnProperty` 控制，可在配置中禁用 |
| 消息格式版本化 | 消息体包含 `version` 字段，后续格式变更时新旧消费者可共存 |
| 监控指标 | 每个消费者暴露 `messages.consumed`、`messages.failed`、`processing.latency` 三类 Micrometer 指标 |
| 本地开发 | 提供 `EmbeddedRabbitMQ` 或 `TestContainers` 配置，开发者无需本地安装 RabbitMQ 即可运行测试 |

#### 2.1.7 可扩展性设计

| 扩展场景 | 如何支持 |
|---------|---------|
| 新增通知渠道（如短信） | 新增一个 Queue 绑定 `alert.topic`，写一个新的 Consumer 即可 |
| 设备控制接入 | 新增 `device.control.direct` Exchange 和按 deviceId 的 Queue |
| YOLO Service 多实例 | 每个实例发布到同一 Exchange，RabbitMQ 自动负载均衡 |
| Backend 多实例 | 多个 Consumer 实例共享同一 Queue，RabbitMQ 自动轮流投递 |
| 消息格式变更 | `version` 字段 + 新旧消费者共存过渡期 |

---

### 2.2 Micrometer + Prometheus + Grafana — 系统可观测

#### 2.2.1 引入目的

当前系统运行完全"黑盒"：
- 不知道哪个 API 最慢
- 不知道 WebSocket 连接数是多少
- 不知道报警从产生到用户收到花了多久
- 不知道 RabbitMQ 消息积压了多少
- 只能通过翻日志 grep 来排查问题

引入可观测性后的目标：**从"出问题翻日志猜原因"变成"看仪表盘定位问题"**。

#### 2.2.2 使用方向

**自动采集的指标（零代码，加依赖即可）**：

| 指标类别 | 具体指标 | 来源 |
|---------|---------|------|
| HTTP 请求 | 每个 API 的请求数、响应时间 P50/P95/P99、错误率 | Spring Boot Actuator |
| JVM | 堆内存使用量、GC 次数和时间、线程数 | Micrometer JVM 指标 |
| Tomcat | 线程池活跃数、排队请求数 | Micrometer Tomcat 指标 |
| 数据库 | HikariCP 连接池活跃/空闲/等待数 | Micrometer HikariCP 指标 |
| Redis | 连接池活跃/空闲/等待数、命令延迟 | Micrometer Lettuce 指标 |
| RabbitMQ | 队列消息数、消费速率、重试次数 | Spring AMQP 指标 |

**自定义业务指标（需少量代码）**：

| 指标名 | 类型 | 用途 | 采集位置 |
|--------|------|------|----------|
| `alert.events.received` | Counter | 收到的报警事件总数 | AlertRecordConsumer |
| `alert.events.processing.latency` | Timer | 报警从产生到写入数据库的耗时 | AlertRecordConsumer |
| `alert.events.dropped` | Counter | 被丢弃的无效报警数 | AlertRecordConsumer |
| `ws.connections.active` | Gauge | 当前 WebSocket 连接数 | AlertWebSocketHandler |
| `ws.messages.sent` | Counter | WebSocket 推送消息数 | AlertWebSocketHandler |
| `device.online.count` | Gauge | 在线设备数 | CameraDeviceService |
| `ai.inference.latency` | Timer | AI 推理延迟（YOLO 侧） | YOLO Service |
| `ai.inference.fps` | Gauge | AI 推理帧率 | YOLO Service |

#### 2.2.3 设计模式应用

**模式：观察者模式（Observer）— 指标采集**

通过 Spring Event 机制解耦业务逻辑和指标采集：

```
业务代码（不直接依赖 Micrometer）
    └── 发布 Spring ApplicationEvent
           │
           ▼
    MetricsEventListener（观察者）
           │
           └── 更新 Micrometer Counter/Timer/Gauge
                  │
                  ▼
           /actuator/prometheus 端点暴露
                  │
                  ▼
           Prometheus 每 15s 拉取
                  │
                  ▼
           Grafana 仪表盘展示
```

这样做的好处：
- 业务代码不直接依赖 Micrometer API，保持业务逻辑干净
- 新增指标只需新增 Event 和 Listener，不修改已有代码
- 可以通过配置开关启用/禁用指标采集

新增代码结构：
```
com.springboot
├── metrics/                          ← 新增：指标采集
│   ├── event/
│   │   ├── AlertEventReceivedEvent.java       ← 报警事件收到事件
│   │   ├── AlertProcessingCompletedEvent.java  ← 报警处理完成事件
│   │   └── DeviceStatusChangedEvent.java       ← 设备状态变更事件
│   ├── listener/
│   │   └── MetricsEventListener.java           ← 监听事件，更新指标
│   └── MetricsConfig.java                      ← 自定义指标 Bean 注册
```

#### 2.2.4 YOLO Service 侧指标采集

Python 侧使用 `prometheus_client` 库暴露指标：

```
# 新增依赖到 requirements.txt
prometheus_client>=0.21.0

# 新增模块
app/metrics/
├── __init__.py
├── inference_metrics.py      ← 推理延迟、FPS、模型加载时间
├── stream_metrics.py         ← 流连接状态、帧丢弃率
└── task_metrics.py           ← 任务数量、任务启动/停止次数
```

Flask 新增 `/metrics` 端点，Prometheus 统一拉取。

#### 2.2.5 Grafana 仪表盘规划

| 仪表盘名称 | 包含面板 | 用途 |
|-----------|---------|------|
| **系统概览** | QPS、P95 延迟、错误率、JVM 堆内存、CPU | 日常巡检 |
| **报警看板** | 报警事件数/分钟、处理延迟 P95、WebSocket 连接数 | 报警业务监控 |
| **消息队列** | 各 Queue 消息积压量、消费速率、死信数量 | RabbitMQ 运维 |
| **AI 推理** | 推理 FPS、推理延迟、GPU 使用率、流连接状态 | YOLO Service 监控 |
| **基础设施** | MySQL 连接池、Redis 命令延迟、磁盘使用 | 基础运维 |

#### 2.2.6 部署方式

新增 Prometheus + Grafana 两个服务，通过 Docker Compose 部署：

```yaml
# docker-compose.monitoring.yml
services:
  prometheus:
    image: prom/prometheus:latest
    ports: ["9090:9090"]
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]
    volumes:
      - grafana_data:/var/lib/grafana
```

Backend 暴露 `/actuator/prometheus` 端点，YOLO Service 暴露 `/metrics` 端点，Prometheus 每 15 秒拉取。

---

### 2.3 Spotless + Checkstyle — Java 代码规范化

#### 2.3.1 引入目的

当前四个子项目均无 Lint/Format 工具配置。代码风格不统一增加审查成本，新人上手需要学习不成文约定。Backend 作为主项目应该率先规范化。

#### 2.3.2 使用方向

| 工具 | 职责 | 检查时机 |
|------|------|---------|
| **Spotless** | 代码格式化（缩进、空格、换行、import 排序） | `mvn spotless:check` 检查，`mvn spotless:apply` 自动修复 |
| **Checkstyle** | 代码规范检查（命名、注释、复杂度、Javadoc） | `mvn checkstyle:check` 检查，不允许自动修复（必须手动改正） |

#### 2.3.3 配置要点

**Spotless 配置**（使用 Google Java Format）：
- 4 空格缩进（与项目现有风格一致）
- import 排序：java → javax → 第三方 → 项目内部
- 删除多余空行
- 行尾分号规范

**Checkstyle 配置**（基于 Google Checkstyle，适度放宽）：
- 类和方法必须有 Javadoc（中文注释）
- 变量命名：Entity 字段暂时允许 snake_case（历史原因，后续重构再改）
- 方法长度上限 150 行
- 单个文件长度上限 500 行
- 不允许 `System.out.println`（必须用 Logger）

#### 2.3.4 与 CI 的集成

后续接入 CI/CD 时，Pipeline 中必须包含：
```bash
mvn spotless:check checkstyle:check
```
格式或规范不通过则拒绝合并。

---

## 3. 阶段 2：容错与限流

### 3.1 Resilience4j — 重试、熔断、超时

#### 3.1.1 引入目的

当前后端完全没有容错机制：
- YOLO Service 回调 Backend 失败时直接抛异常，没有重试
- 外部 API（高德地图、邮件服务）故障时无熔断，可能拖垮整个服务
- 数据库慢查询没有超时控制，线程池可能耗尽
- 后续硬件控制（ESP32-CAM）网络不稳定时需要重试保障

#### 3.1.2 使用方向

| 场景 | Resilience4j 组件 | 配置 | 说明 |
|------|-------------------|------|------|
| YOLO 回调重试 | `@Retry` | 最大 3 次，指数退避 500ms/1s/2s | 回调失败自动重试 |
| 外部 API 熔断 | `@CircuitBreaker` | 失败率 50% 触发熔断，等待 30s 后半开 | 高德/邮件服务故障时快速失败 |
| 数据库慢查询超时 | `@Timelimiter` | 超时 5 秒 | 防止慢查询拖垮线程池 |
| 硬件控制重试 | `@Retry` | 最大 3 次，固定间隔 300ms | PTZ 指令重试 |
| 硬件控制熔断 | `@CircuitBreaker` | 失败率 60% 触发熔断，等待 20s | 设备离线时快速返回失败 |
| 速率限制 | `@RateLimiter` | 每秒 10 次 | 防止短时间内发送过多控制指令 |

#### 3.1.3 设计模式应用

**模式：装饰器模式（Decorator）— 容错包装**

Resilience4j 本身就是装饰器模式的经典应用：

```
原始调用链：
  yoloCallbackService.sendAlert(alert)

装饰后（叠加容错能力）：
  CircuitBreaker.decorateFunction(
    RateLimiter.decorateFunction(
      Retry.decorateFunction(
        yoloCallbackService::sendAlert,
        retryConfig
      ),
      rateLimiterConfig
    ),
    circuitBreakerConfig
  )
```

Spring Boot 集成后通过注解简化：
```java
@CircuitBreaker(name = "yoloCallback", fallbackMethod = "onCallbackFailed")
@Retry(name = "yoloCallback", fallbackMethod = "onCallbackFailed")
@RateLimiter(name = "yoloCallback")
public void sendAlertCallback(AlertEventMessage message) {
    // 实际发送逻辑
}

// 降级方法：所有容错机制失败后的兜底处理
private void onCallbackFailed(AlertEventMessage message, Exception e) {
    log.error("YOLO回调失败，消息进入死信队列: {}", message.getMessageId(), e);
    // 写入数据库标记为"回调失败"，等待人工处理
}
```

#### 3.1.4 新增包结构

```
com.springboot
├── resilience/                     ← 新增：容错配置
│   ├── ResilienceConfig.java                ← 全局 Resilience4j 配置
│   ├── CircuitBreakerConfig.java            ← 熔断器配置
│   ├── RetryConfig.java                     ← 重试配置
│   ├── RateLimiterConfig.java               ← 限流配置
│   └── FallbackHandlers.java               ← 通用降级处理
```

#### 3.1.5 可维护性保障

| 保障项 | 具体措施 |
|--------|---------|
| 配置外部化 | 所有熔断/重试/限流参数写入 `application.yml`，支持按环境调整 |
| 配置分组 | 按调用目标分组（yoloCallback、deviceControl、externalApi），每组独立配置 |
| 降级可观测 | 每次降级执行时记录 Micrometer 指标 + WARN 级别日志 |
| 熔断状态可视化 | Resilience4j 暴露的指标通过 Micrometer 推送到 Prometheus，Grafana 展示熔断器开/关/半开状态 |

---

### 3.2 Bucket4j — 接口限流

#### 3.2.1 引入目的

当前所有 API 无任何限流保护。恶意或误操作可以短时间内发送大量请求，导致：
- 后端资源被耗尽
- 数据库被压垮
- 后续硬件控制（PTZ 指令）如果不过限流，ESP32-CAM 的 SG90 舵机可能因高频指令损坏

#### 3.2.2 使用方向

| 限流场景 | 限流策略 | 说明 |
|---------|---------|------|
| 全局 API 限流 | 令牌桶 100 req/s | 防止整体过载 |
| 登录接口限流 | 令牌桶 5 req/min/IP | 防暴力破解 |
| 报警查询限流 | 令牌桶 30 req/s/user | 防止大数据量查询压垮数据库 |
| 设备控制限流 | 令牌桶 2 req/s/device | 保护 ESP32-CAM 舵机，防止指令风暴 |
| 报警事件写入 | 令牌桶 50 req/s | 保护消息消费不积压 |

#### 3.2.3 设计模式应用

**模式：责任链模式（Chain of Responsibility）— 限流过滤**

通过 Spring Web Filter 实现全局限流，通过 AOP 注解实现接口级限流：

```
HTTP 请求 → 全局限流 Filter → 接口级限流 AOP → 业务逻辑
                │                    │
                └── Bucket4j 令牌桶    └── Bucket4j 令牌桶
```

分为两层：
- **Filter 层**：粗粒度全局限流，基于 IP 或 Token
- **AOP 层**：细粒度接口限流，基于用户或设备 ID

新增代码结构：
```
com.springboot
├── ratelimit/                       ← 新增：限流
│   ├── RateLimitFilter.java                 ← 全局限流 Filter
│   ├── RateLimitAspect.java                 ← 接口级限流 AOP
│   ├── RateLimit.java                       ← 自定义注解 @RateLimit
│   └── BucketFactory.java                   ← 令牌桶工厂（基于 Redis 分布式）
```

使用方式：
```java
@RateLimit(capacity = 2, refillRate = 2, per = "device")
@PostMapping("/control/ptz")
public BaseResponse<Void> controlPtz(...) { ... }
```

---

## 4. 阶段 3：AI 智能分析

### 4.1 Spring AI — 智能报警分析与自然语言查询

#### 4.1.1 引入目的

当前报警系统和数据查询存在以下不足：
- 报警描述靠模板拼接，信息单一
- 管理员查询数据只能通过固定页面，无法灵活自然语言提问
- 历史报警之间缺乏关联分析，每次回到"零基线"
- 安全周报需要人工汇总

引入 Spring AI 后可以在以下场景发挥作用：

| 场景 | 当前做法 | Spring AI 增强后 |
|------|---------|-----------------|
| 报警描述 | 模板："`{zone}区域检测到溺水，置信度{score}`" | LLM 综合上下文生成自然语言描述，包含时间、场馆、历史对比 |
| 自然语言查询 | 固定页面筛选 | 管理员输入"3号泳池昨天有多少次报警"，LLM 调用 Function 查库回答 |
| 报告生成 | 人工汇总 | 每周自动生成安全周报，包含趋势分析、风险区域建议 |
| 误报分析 | 需要人工判断 | LLM 结合历史案例和上下文，辅助判断是否误报 |

#### 4.1.2 Spring AI 核心能力在项目中的映射

**Chat Model（对话模型）**

用于生成自然语言文本。在 AquaSentinel 中：
- 报警描述增强：将结构化报警数据 + 上下文信息输入 LLM，生成更有价值的描述
- 安全报告生成：将统计数据输入 LLM，生成周报/月报

```
结构化报警数据 + 场馆上下文 + 历史数据
                    ↓
              Spring AI ChatClient
                    ↓
"3号泳池深水区检测到疑似溺水行为。
 本周该区域已发生2次类似报警，建议重点关注。
 当前值班的救生员张三已于30秒前收到通知。"
```

**Function Calling（函数调用）**

LLM 不直接访问数据库，而是通过 Spring AI 的 Function Calling 机制调用后端预定义的 Function。管理员自然语言提问时，LLM 自动判断需要调用哪些 Function：

```
管理员输入: "3号泳池昨天的报警情况和设备状态"

LLM 推理过程：
  1. 需要报警数据 → 调用 getAlertRecords(venueId=3, date=yesterday)
  2. 需要设备状态 → 调用 getDeviceStatus(venueId=3)
  3. 综合两个 Function 的返回结果 → 生成自然语言回答

LLM 回答:
  "3号泳池昨天共发生4次报警（3次溺水检测、1次区域入侵），
   目前4个摄像头全部在线，2号摄像头今日凌晨有过一次短暂断流（约30秒）。"
```

预定义 Function 列表：

| Function 名 | 功能 | 参数 |
|-------------|------|------|
| `getAlertRecords` | 查询报警记录 | venueId, dateRange, alertType |
| `getDeviceStatus` | 查询设备在线状态 | venueId, deviceId |
| `getLifeguardOnDuty` | 查询值班救生员 | venueId, shift |
| `getStatsSnapshot` | 查询统计数据 | venueId, dateRange, metric |
| `getMonitorTasks` | 查询推理任务状态 | venueId |
| `controlPtz` | 控制云台转动 | deviceId, command, angle |

> 注意：`controlPtz` 这类有副作用的 Function 需要确认机制——LLM 返回"建议执行 PTZ 指令"，用户确认后才真正执行。

**Embedding + Vector Store（向量存储与语义搜索）**

将历史报警记录向量化存储，新报警产生时自动匹配相似案例：

```
新报警产生:
  "2号泳池检测到溺水行为，置信度0.92"

向量化 → 在向量数据库中搜索最相似的 5 条历史报警:
  - 2025-08-12: 同一位置，同一类型，确认为真实溺水
  - 2025-09-03: 同一位置，误报（光影干扰）
  - 2025-09-15: 相邻区域，真实溺水

LLM 分析:
  "该位置历史上真实溺水和误报各占约50%，置信度0.92较高，
   建议立即通知值班救生员前往确认。"
```

#### 4.1.3 设计模式应用

**模式一：策略模式（Strategy）— 模型切换**

支持多种 LLM Provider，配置切换即可：

```
AiModel（接口）
├── OpenAiChatModel          ← OpenAI GPT-4/4o
├── OllamaChatModel          ← 本地模型（Ollama，免费，私有化部署）
└── ZhiPuChatModel           ← 智谱 GLM-4（国产，国内网络直连）
```

通过 `application.yml` 配置切换：
```yaml
spring:
  ai:
    openai:
      api-key: ${AI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.3    # 低温度确保输出稳定、一致
```

后续如果想换国产模型或本地部署，只需改配置，不改代码。

**模式二：模板方法模式（Template Method）— 报警分析流程**

```
AlertAnalysisService（抽象模板）
    ├── collectContext(alert)           ← 收集上下文（子类实现）
    ├── callAiModel(prompt)            ← 调用 LLM（模板方法固定）
    ├── parseAiResponse(response)      ← 解析 LLM 回复（子类实现）
    └── enrichAlert(alert, analysis)   ← 增强报警信息（模板方法固定）

DrowningAlertAnalysisService（具体实现）
    └── collectContext() → 查询该场馆历史溺水报警、当前天气、值班救生员

InvasionAlertAnalysisService（具体实现）
    └── collectContext() → 查询该区域入侵历史、当前时间段是否开放
```

**模式三：代理模式（Proxy）— Function Calling 安全控制**

LLM 发起的 Function 调用必须通过代理层，代理层负责：
- 权限校验：LLM 只能调用允许的 Function
- 参数校验：防止 LLM 生成异常参数
- 审计日志：记录每次 Function 调用
- 确认机制：有副作用的操作（如云台控制）需要人工确认

```
LLM → FunctionCallingProxy → 实际 Function
          │
          ├── 权限校验（Function 白名单）
          ├── 参数校验（类型、范围检查）
          ├── 操作确认（副作用操作需人工确认）
          └── 审计日志（记录调用时间、参数、结果）
```

#### 4.1.4 新增包结构

```
com.springboot
├── ai/                              ← 新增：AI 智能分析
│   ├── config/
│   │   └── SpringAiConfig.java              ← Spring AI 配置（模型、参数）
│   ├── chat/
│   │   ├── ChatService.java                  ← 聊天服务接口
│   │   └── DefaultChatService.java           ← 默认实现（OpenAI/Ollama）
│   ├── function/
│   │   ├── AiFunctionRegistry.java           ← Function 注册表（白名单管理）
│   │   ├── AlertQueryFunction.java           ← 查询报警记录
│   │   ├── DeviceStatusFunction.java         ← 查询设备状态
│   │   ├── LifeguardQueryFunction.java       ← 查询值班救生员
│   │   ├── StatsQueryFunction.java           ← 查询统计数据
│   │   └── DeviceControlFunction.java        ← 设备控制（需确认机制）
│   ├── analysis/
│   │   ├── AlertAnalysisService.java         ← 报警分析服务接口
│   │   ├── DrowningAlertAnalysisService.java  ← 溺水报警分析
│   │   └── InvasionAlertAnalysisService.java   ← 入侵报警分析
│   ├── embedding/
│   │   ├── AlertEmbeddingService.java        ← 报警向量化服务
│   │   └── SimilarAlertSearchService.java     ← 相似报警搜索
│   ├── proxy/
│   │   └── FunctionCallingProxy.java          ← Function 调用代理（权限、审计、确认）
│   └── controller/
│       └── AiChatController.java              ← AI 对话 REST API
```

#### 4.1.5 Maven 新增依赖

```xml
<!-- Spring AI OpenAI Starter -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 如果后续需要本地模型 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <scope>optional</scope>
</dependency>
```

#### 4.1.6 安全考虑

| 风险 | 防护措施 |
|------|---------|
| Prompt 注入 | 系统提示词固定化，用户输入作为变量而非指令 |
| LLM 幻觉 | Function Calling 返回结构化数据，LLM 只做语言组织，不做事实判断 |
| 数据泄露 | 不将原始数据库记录发给 LLM，只发送脱敏摘要 |
| 控制指令滥用 | PTZ 等 Function 必须经过人工确认代理，不自动执行 |
| API Key 泄露 | Key 存储在环境变量，不硬编码，不入代码库 |

---

### 4.2 向量数据库 — 历史报警语义搜索

#### 4.2.1 引入目的

传统数据库只能做关键词搜索，无法理解语义。例如搜索"泳池有人挣扎"，传统 SQL 无法匹配到描述为"溺水行为检测"的历史记录。

向量数据库将报警描述转化为向量，通过向量相似度搜索，实现语义级别的匹配。

#### 4.2.2 选型：PGVector（推荐）

| 候选 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **PGVector** | MySQL/PostgreSQL 扩展，运维简单，不需要新服务 | 检索性能不如专业向量库 | ★★★★★ |
| Milvus | 专业向量数据库，检索性能强 | 需要独立部署和运维 | ★★★ |
| Weaviate | 内置向量化，开箱即用 | Java SDK 不成熟 | ★★ |

**推荐 PGVector 的理由**：
- 项目已有 MySQL，PGVector 是 PostgreSQL 扩展，如果后续迁移到 PostgreSQL 可以无缝集成
- 当前报警数据量（几万到几十万条） PGVector 完全够用
- 不增加新的基础设施，降低运维负担
- 如果后续数据量超出 PGVector 能力（百万级以上），可以平滑迁移到 Milvus

实施策略：先在 MySQL 中用 JSON 类型存储向量（简化方案），后续如需要再迁移到 PostgreSQL + PGVector。Spring AI 对两种方案都有支持。

#### 4.2.3 设计模式应用

**模式：仓库模式（Repository）— 向量存储抽象**

```
AlertVectorRepository（接口）
├── store(alertId, embedding)          ← 存储报警向量
├── searchSimilar(embedding, topK)     ← 搜索相似报警
└── delete(alertId)                    ← 删除向量

MySqlAlertVectorRepository（MySQL JSON 向量实现）
└── 使用 MySQL 的 JSON 类型存向量， cosine 相似度用 Java 计算

PgVectorAlertVectorRepository（后续迁移）
└── 使用 PostgreSQL + PGVector 扩展，向量检索在数据库层完成
```

通过接口抽象，存储实现可以从 MySQL JSON 平滑迁移到 PGVector，业务代码无需修改。

---

## 5. 阶段 4：通知与缓存

### 5.1 多渠道通知 — 钉钉/企微 Webhook

#### 5.1.1 引入目的

当前报警推送只有 WebSocket（Frontend/Android）一种渠道。如果管理员不在电脑旁或手机未打开 APP，报警信息无法触达。

多渠道通知使得报警信息可以同时推送到钉钉、企业微信等 IM，确保重要报警不会被遗漏。

#### 5.1.2 使用方向

```
报警事件 → RabbitMQ alert.notification.queue
               │
               ▼
        NotificationConsumer（通知分发器）
               │
               ├── NotificationChannel（接口）
               │       │
               │       ├── DingTalkWebhookChannel    ← 钉钉机器人
               │       ├── WeComWebhookChannel       ← 企业微信机器人
               │       ├── SmsChannel                ← 短信（阿里云/腾讯云）
               │       └── WebsocketChannel          ← 现有推送（解耦后）
               │
               └── NotificationRouter（路由器）
                       └── 根据报警级别和用户配置选择通知渠道
```

#### 5.1.3 设计模式应用

**模式：策略模式（Strategy）— 通知渠道切换**

```
NotificationChannel（接口）
├── send(alert, recipient)    ← 发送通知
├── supports(level)           ← 是否支持该报警级别
└── getChannelName()         ← 渠道名称

DingTalkWebhookChannel（实现）
└── HTTP POST 到钉钉机器人 Webhook

WeComWebhookChannel（实现）
└── HTTP POST 到企业微信机器人 Webhook

SmsChannel（实现）
└── 调用阿里云/腾讯云短信 API
```

新增渠道只需实现 `NotificationChannel` 接口，注册为 Spring Bean 即可，路由器自动发现。

**模式：责任链模式（Chain of Responsibility）— 通知降级**

高优先级报警需要确保送达：
```
高优先级报警 → 先尝试 WebSocket → 未送达 → 尝试钉钉 → 未送达 → 发短信
低优先级报警 → 只尝试 WebSocket → 未送达 → 尝试钉钉 → 停止
```

降级链路通过配置文件定义，灵活可调。

#### 5.1.4 新增包结构

```
com.springboot
├── notification/                    ← 新增：多渠道通知
│   ├── channel/
│   │   ├── NotificationChannel.java         ← 通知渠道接口
│   │   ├── DingTalkWebhookChannel.java       ← 钉钉机器人
│   │   ├── WeComWebhookChannel.java          ← 企业微信机器人
│   │   ├── SmsChannel.java                   ← 短信通知
│   │   └── WebsocketChannel.java             ← 现有 WebSocket 推送
│   ├── router/
│   │   └── NotificationRouter.java           ← 通知路由（根据级别选渠道）
│   ├── fallback/
│   │   └── NotificationFallbackChain.java     ← 降级链路
│   ├── model/
│   │   ├── NotificationRequest.java           ← 通知请求
│   │   └── NotificationResult.java            ← 通知结果
│   └── config/
│       └── NotificationConfig.java            ← 渠道配置
```

---

### 5.2 Caffeine + Redis 两级缓存

#### 5.2.1 引入目的

当前某些热点数据（权限映射、场馆配置、设备状态）每次请求都查数据库，在高并发场景下会产生不必要的数据库压力。

两级缓存策略：
- **L1 Caffeine（本地缓存）**：毫秒级读取，存不常变化的热点数据（权限、配置）
- **L2 Redis（分布式缓存）**：毫秒级读取，存需要跨实例共享的数据（会话、设备状态）

#### 5.2.2 使用方向

| 数据类型 | 缓存策略 | L1 TTL | L2 TTL | 说明 |
|---------|---------|--------|--------|------|
| 用户权限映射 | L1 → L2 → DB | 5 分钟 | 30 分钟 | 变化极少，命中率高 |
| 场馆配置 | L1 → L2 → DB | 5 分钟 | 30 分钟 | 变化极少 |
| 设备在线状态 | L2 → DB | — | 30 秒 | 变化频繁，需要跨实例一致 |
| 统计快照 | L2 → DB | — | 5 分钟 | 仪表盘频繁查询 |
| 报警详情 | L2 → DB | — | 10 分钟 | 查询后缓存，变更时主动失效 |

#### 5.2.3 设计模式应用

**模式：代理模式（Proxy）— 透明缓存**

业务代码不直接操作缓存，通过代理层透明访问：

```java
// 业务代码不变，只需加注解
@Cached(key = "'user:permissions:' + #userId", 
        cacheType = CacheType.TWO_LEVEL,
        l1Ttl = 300, l2Ttl = 1800)
public UserPermissions getUserPermissions(Long userId) {
    return permissionMapper.selectByUserId(userId);
}

// 缓存失效
@CacheEvict(key = "'user:permissions:' + #userId", 
            cacheType = CacheType.TWO_LEVEL)
public void updateUserPermissions(Long userId, UserPermissions permissions) {
    permissionMapper.updateByUserId(userId, permissions);
}
```

新增包结构：
```
com.springboot
├── cache/                            ← 新增：二级缓存
│   ├── config/
│   │   └── CacheConfig.java                 ← Caffeine + Redis 配置
│   ├── annotation/
│   │   ├── Cached.java                       ← 缓存注解
│   │   └── CacheEvict.java                   ← 缓存失效注解
│   ├── aspect/
│   │   └── CacheAspect.java                  ← 缓存切面（AOP）
│   └── CacheManager.java                     ← 缓存管理器（L1 → L2 → DB）
```

---

## 6. 阶段 5：OpenClaw — 运维 AI 助手（外围）

### 6.1 定位说明

OpenClaw **不嵌入 AquaSentinel**，而是作为独立外围部署。它与 AquaSentinel 的关系是"运维层调用业务层"：

```
用户（QQ/微信/钉钉）
    ↓ 发送自然语言消息
OpenClaw（独立部署的 Node.js 服务）
    ↓ Skill 调用 AquaSentinel API
AquaSentinel Backend（提供 REST API）
    ↑ 返回结构化数据
OpenClaw
    ↓ LLM 组织自然语言回复
用户
```

### 6.2 OpenClaw 需要对接的 AquaSentinel API

为了让 OpenClaw 能查询和管理 AquaSentinel，Backend 需要暴露以下 API（大部分已有，可能需要补充）：

| 类别 | API | 说明 | 状态 |
|------|-----|------|------|
| 报警查询 | `GET /alert-records/list/page/vo` | 分页查询报警记录 | ✅ 已有 |
| 设备状态 | `GET /camera-devices/list/vo` | 查询摄像头状态 | ✅ 已有 |
| 场馆信息 | `GET /venues/list/vo` | 查询场馆列表 | ✅ 已有 |
| 救生员状态 | `GET /lifeguards/list/vo` | 查询救生员信息 | ✅ 已有 |
| 统计数据 | `GET /stats/snapshot/list` | 查询统计快照 | ✅ 已有 |
| 服务健康 | `GET /actuator/health` | 健康检查 | ✅ 已有 |
| Prometheus 指标 | `GET /actuator/prometheus` | 指标数据 | ✅ 已有（阶段1后） |
| AI 推理任务 | `GET /monitor/tasks/realtime/by-camera` | 查询推理任务状态 | ✅ 已有 |
| 设备控制 | `POST /camera-devices/control/ptz` | 云台控制 | ❌ 需新增 |

### 6.3 需要为 OpenClaw 创建的 Skill

OpenClaw 的 Skill 是它理解和调用外部系统的能力定义。需要为 AquaSentinel 创建的 Skill：

| Skill 名称 | 功能 | 调用的 API |
|-----------|------|-----------|
| `aquasentinel-alerts` | 查询报警记录 | `/alert-records/list/page/vo` |
| `aquasentinel-devices` | 查询设备状态 | `/camera-devices/list/vo` |
| `aquasentinel-venues` | 查询场馆信息 | `/venues/list/vo` |
| `aquasentinel-stats` | 查询统计数据 | `/stats/snapshot/list` |
| `aquasentinel-health` | 查询服务健康状态 | `/actuator/health`, Prometheus API |
| `aquasentinel-control` | 控制云台/设备 | `/camera-devices/control/ptz` |

### 6.4 AquaSentinel 侧需要做的准备

为 OpenClaw 对接做准备，Backend 需要：

1. **API 认证增强**：新增 OpenClaw 专用 API Key 认证方式（与现有 JWT 认证并行），OpenClaw 通过 API Key 调用 Backend API
2. **新增设备控制 API**：当前缺少 PTZ 控制接口，需要新增（也是硬件对接的必要接口）
3. **Prometheus 指标暴露**：阶段 2 已包含
4. **CORS 配置**：允许 OpenClaw 所在域名跨域访问

---

## 7. 设计模式总结

本项目将使用的设计模式及其应用场景汇总：

| 设计模式 | 应用位置 | 目的 |
|---------|---------|------|
| **发布-订阅** | RabbitMQ Exchange → Queue | 报警事件一对多分发，解耦生产者和消费者 |
| **策略模式** | 消息序列化、通知渠道、AI 模型、流协议 | 运行时切换实现，不修改调用方代码 |
| **模板方法** | 报警分析流程 | 固定分析步骤，子类只实现差异部分 |
| **代理模式** | Function Calling 安全控制、二级缓存 | 透明增加横切关注点（权限、审计、缓存） |
| **观察者** | 指标采集（Spring Event） | 解耦业务逻辑和指标采集 |
| **装饰器** | Resilience4j 容错包装 | 叠加重试/熔断/限流能力 |
| **责任链** | 限流过滤、通知降级 | 按顺序处理请求，每层可中断或继续 |
| **工厂** | 令牌桶（BucketFactory） | 统一创建不同配置的限流桶 |
| **仓库** | 向量存储（AlertVectorRepository） | 抽象数据访问，存储实现可替换 |

---

## 8. 完整阶段规划总览

| 阶段 | 技术 | 目标 | 新增依赖/基建 | 预估工时 |
|------|------|------|-------------|----------|
| **1a** | RabbitMQ | 报警事件可靠投递 + 后续设备控制队列 | `spring-boot-starter-amqp` + YOLO 侧 `pika` | 3-5 天 |
| **1b** | Micrometer + Prometheus + Grafana | 系统可观测 | `micrometer-registry-prometheus` + Prometheus + Grafana Docker | 2-3 天 |
| **1c** | Spotless + Checkstyle | Java 代码规范化 | Maven 插件 | 0.5 天 |
| **2a** | Resilience4j | 重试/熔断/超时 | `resilience4j-spring-boot3` | 2-3 天 |
| **2b** | Bucket4j + Redis | 接口限流 | `bucket4j-redis` | 1-2 天 |
| **3a** | Spring AI (Chat + Function Calling) | 智能报警分析、自然语言查询 | `spring-ai-openai-spring-boot-starter` | 5-7 天 |
| **3b** | 向量数据库 (MySQL JSON → PGVector) | 历史报警语义搜索 | Spring AI Vector Store | 3-5 天 |
| **4a** | 多渠道通知 | 报警推送到 IM | 纯 HTTP 调用 | 2-3 天 |
| **4b** | Caffeine + Redis 两级缓存 | 热点数据加速 | `caffeine` + Spring Cache | 2-3 天 |
| **5** | OpenClaw（外围） | 运维 AI 助手 | Node.js 服务 + Skill 开发 | 3-5 天 |

**总预估：22-35 天**

---

## 9. 架构演进对比

### 9.1 消息流对比

**当前**：
```
YOLO ──WebSocket──▶ Backend ──▶ 数据库 + WebSocket 推送
                  │
                  └── 断线即丢，无线重试
```

**升级后**：
```
YOLO ──publish──▶ RabbitMQ ──┬── alert.record.queue ──▶ 写库 + WebSocket
                              ├── alert.notification.queue ──▶ 钉钉/企微/短信
                              └── alert.analytics.queue ──▶ 统计 + AI 分析

（WebSocket 作为实时通知通道保留，RabbitMQ 作为持久化保障兜底）
```

### 9.2 可观测性对比

**当前**：
```
出问题 → 翻日志 grep → 猜原因 → 30-60 分钟定位
```

**升级后**：
```
出问题 → Grafana 仪表盘看趋势 → 看指标定位 → <5 分钟定位
```

### 9.3 AI 能力对比

**当前**：
```
报警描述 = 模板拼接（"{zone}区域检测到{type}，置信度{score}"）
查询数据 = 固定页面筛选
报告生成 = 人工汇总
```

**升级后**：
```
报警描述 = LLM 综合上下文（场馆、天气、历史）生成自然语言
查询数据 = 自然语言提问 → LLM 调用 Function → 结构化回答
报告生成 = LLM 自动生成周报/月报
误报判断 = LLM 匹配历史相似案例，辅助判断
```

### 9.4 通知渠道对比

**当前**：
```
报警通知 = 仅 WebSocket（需在线才能收到）
```

**升级后**：
```
报警通知 = WebSocket（实时） + 钉钉（即时） + 企微（即时） + 短信（保底）
降级链路 = WebSocket 未送达 → 钉钉 → 短信
```