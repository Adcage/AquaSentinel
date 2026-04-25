# 阶段一改造总结：消息可靠投递 + 系统可观测 + 代码规范化

> 实施日期：2026-04-24
>
> 对应规划文档：`docs/规划文档/backend-tech-upgrade-plan.md` 第 2 节

---

## 一、改造概览

本次阶段一改造完成了三个子阶段的全部内容：

| 子阶段 | 技术 | 状态 |
|--------|------|------|
| 1a | RabbitMQ 消息可靠投递 | ✅ 已完成 |
| 1b | Micrometer + Prometheus + Grafana 系统可观测 | ✅ 已完成 |
| 1c | Spotless + Checkstyle 代码规范化 | ✅ 已完成 |

---

## 二、1a — RabbitMQ 消息可靠投递

### 2.1 设计思路

采用**渐进式接入**方案：RabbitMQ 作为报警事件的**持久化保障通道**，与现有 WebSocket 实时推送和 HTTP 回调并行运行，不破坏任何现有功能。

**改造前后对比：**

| | 改造前 | 改造后 |
|---|--------|--------|
| YOLO → Backend 报警 | 仅 HTTP 回调（失败重试 3 次） | HTTP 回调 + RabbitMQ 双通道 |
| Backend 报警处理 | 仅 `InternalAiCallbackController` | 新增 `AlertRecordConsumer` 消费队列 |
| 幂等保障 | 仅 HTTP 层去重 | RabbitMQ 消费者也带 eventUid 去重 |
| 消息丢失风险 | WebSocket 断线即丢，HTTP 回调失败后丢失 | RabbitMQ 持久化兜底，消费失败进入死信队列 |

### 2.2 Backend 改造详情

**pom.xml 新增依赖：**
- `spring-boot-starter-amqp` — Spring AMQP/RabbitMQ 集成
- `spring-boot-starter-actuator` — Spring Boot Actuator（为 Prometheus 端点服务）
- `micrometer-registry-prometheus` — Micrometer Prometheus 指标导出
- `spring-rabbit-test`（test scope）— RabbitMQ 测试支持

**application.yml 新增配置：**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: ${RABBITMQ_PASSWORD:change_me}
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        retry:
          enabled: true
          max-attempts: 3
app:
  messaging:
    rabbitmq:
      enabled: true
      alert-exchange: alert.topic
      alert-record-queue: alert.record.queue
      alert-notification-queue: alert.notification.queue
      alert-analytics-queue: alert.analytics.queue
      notification-consumer-enabled: false   # 阶段4启用
      analytics-consumer-enabled: false       # 后续阶段启用
      message-ttl: 86400000
```

**新增 `messaging` 包（`com.springboot.messaging`）：**

| 文件 | 作用 |
|------|------|
| `config/RabbitMQConfig.java` | Exchange/Queue/Binding 声明，死信队列配置 |
| `model/AlertEventMessage.java` | 报警事件消息体，包含 `messageId`、`version`、`source` 等字段 |
| `serializer/MessageSerializer.java` | 消息序列化接口 |
| `serializer/JsonMessageSerializer.java` | JSON 序列化实现 |
| `consumer/AlertRecordConsumer.java` | **核心消费者** — 消费 `alert.record.queue`，完成与 `InternalAiCallbackController` 相同的报警处理逻辑（写库+推送），通过 eventUid 幂等去重 |
| `consumer/AlertNotificationConsumer.java` | 通知消费者占位（`@ConditionalOnProperty` 控制，阶段4启用） |
| `consumer/AlertAnalyticsConsumer.java` | 分析消费者占位（`@ConditionalOnProperty` 控制，后续启用） |
| `publisher/AlertEventPublisher.java` | 报警事件发布器，发送到 `alert.topic` Exchange |
| `errorHandler/DeadLetterHandler.java` | 死信队列消费者，记录失败消息日志 |

**关键设计决策：**

1. **三方消费者解耦**：`alert.record.queue`（写库+推送）、`alert.notification.queue`（通知，占位）、`alert.analytics.queue`（分析，占位）独立消费，互不阻塞
2. **消息格式版本化**：`AlertEventMessage` 包含 `version` 字段，后续格式变更时新旧消费者可共存
3. **配置外部化**：Exchange 名称、Queue 名称、Routing Key 全部写入 `application.yml`
4. **条件化启用**：`AlertNotificationConsumer` 和 `AlertAnalyticsConsumer` 通过 `@ConditionalOnProperty` 控制，不影响当前运行

### 2.3 YOLO Service 改造详情

**requirements.txt 新增依赖：**
- `pika>=1.3.0,<2.0` — RabbitMQ Python 客户端
- `prometheus_client>=0.21.0,<1.0` — Prometheus 指标导出

**新增 `app/services/rabbitmq_publisher_service.py`：**

| 功能 | 实现 |
|------|------|
| 连接管理 | 自动重连（指数退避：1s→2s→5s→10s→30s） |
| 消息发布 | `publish_alert(payload, routing_key)` 发布到 `alert.topic` Exchange |
| Exchange 声明 | 自动声明 `alert.topic` Topic Exchange |
| Queue 绑定 | 自动声明三个队列并绑定对应 routing key |
| 消息持久化 | `delivery_mode=2`，消息 TTL 24 小时 |
| 幂等保障 | `messageId` 使用 `eventUid`，消费端去重 |

**修改 `app/services/engine_task_service.py`：**

- 在 `_post_detection_event_if_needed()` 方法中，HTTP 回调之后添加 RabbitMQ 发布逻辑
- 在 `start_task()` / `stop_task()` 中集成 `record_task_started()` / `record_task_stopped()` 指标
- 在推理管线中集成 `record_inference()` 推理延迟指标

**修改 `app/services/callback_client_service.py`：**

- HTTP 回调成功/失败后记录 `record_alert_published(channel="http_callback")` 指标

**修改 `app/core/config.py`：**

新增配置：
```python
RABBITMQ_URL = "amqp://localhost:5672/"
RABBITMQ_EXCHANGE = "alert.topic"
METRICS_ENABLED = True
METRICS_PORT = 9091
```

**修改 `app/__init__.py`：**

- 导入 `rabbitmq_publisher_service`
- 在 `create_app()` 中启动 RabbitMQ 发布服务（YOLO → Backend 消息通道）
- 启动 Prometheus `start_http_server` 在 9091 端口

---

## 三、1b — Micrometer + Prometheus + Grafana 系统可观测

### 3.1 Backend 指标采集

**新增 `metrics` 包（`com.springboot.metrics`）：**

| 文件 | 作用 |
|------|------|
| `event/AlertEventReceivedEvent.java` | Spring ApplicationEvent — 报警事件接收事件 |
| `event/AlertProcessingCompletedEvent.java` | Spring ApplicationEvent — 报警处理完成事件（含延迟） |
| `event/DeviceStatusChangedEvent.java` | Spring ApplicationEvent — 设备状态变更事件 |
| `listener/MetricsEventListener.java` | 监听 Spring Event，更新 Micrometer Counter/Timer |
| `MetricsConfig.java` | 注册 Gauge `ws.connections.active`（当前 WebSocket 连接数） |

**业务代码接入点：**

| 接入位置 | 指标 | 类型 |
|----------|------|------|
| `InternalAiCallbackController.receiveEvent()` | `alert.events.received`（成功/失败） | Counter |
| `InternalAiCallbackController.receiveEvent()` | `alert.events.processing.latency`（处理延迟） | Timer |
| `AlertWebSocketHandler`（通过 Gauge） | `ws.connections.active`（当前连接数） | Gauge |
| `AlertWsPublisher.broadcast()` | `ws.messages.sent`（推送消息数） | Counter |
| `AlertRecordConsumer.onMessage()` | `alert.events.received` + `alert.events.processing.latency` | Counter + Timer |

**Actuator + Prometheus 端点配置（application.yml）：**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: springboot
```

访问 `GET /api/actuator/prometheus` 即可获取 Prometheus 格式指标。

### 3.2 YOLO Service 指标采集

**新增 `app/metrics/` 包：**

| 文件 | 指标 | 说明 |
|------|------|------|
| `inference_metrics.py` | `ai_inference_total` (Counter) | 推理总次数，按 `model_version` 和 `status` 标签 |
| `inference_metrics.py` | `ai_inference_fps` (Gauge) | 当前推理帧率 |
| `inference_metrics.py` | `ai_model_load_seconds` (Histogram) | 模型加载时间 |
| `stream_metrics.py` | `ai_stream_connections` (Gauge) | 活跃流连接数 |
| `stream_metrics.py` | `ai_stream_frames_dropped_total` (Counter) | 丢弃帧数 |
| `stream_metrics.py` | `ai_stream_reconnect_total` (Counter) | 流重连次数 |
| `task_metrics.py` | `ai_task_count` (Gauge) | 运行中推理任务数 |
| `task_metrics.py` | `ai_task_started_total` / `ai_task_stopped_total` (Counter) | 任务启停计数 |
| `task_metrics.py` | `ai_alert_published_total` (Counter) | 报警事件发布数（按 channel 和 status） |

Prometheus 在端口 9091 拉取 `/metrics` 端点。

### 3.3 监控基础设施

**新增 `docker-compose.monitoring.yml`：**
- Prometheus（端口 9090）— 拉取 Backend `/api/actuator/prometheus` 和 YOLO Service `:9091/metrics`
- Grafana（端口 3000）— 可视化仪表盘

**新增 `monitoring/prometheus.yml`：**
- 配置 Backend 和 YOLO Service 为抓取目标
- 15 秒抓取间隔

---

## 四、1c — Spotless + Checkstyle 代码规范化

### 4.1 Maven 插件配置

**pom.xml 新增插件：**

| 插件 | 版本 | 用途 |
|------|------|------|
| `spotless-maven-plugin` | 2.43.0 | 代码格式化（Google Java Format AOSP 风格，4 空格缩进） |
| `maven-checkstyle-plugin` | 3.3.1 | 代码规范检查 |

### 4.2 Spotaly 配置要点

- 使用 Google Java Format AOSP 风格（4 空格缩进，与项目现有风格一致）
- import 排序：`java` → `javax` → 第三方 → `com.springboot`
- 自动删除未使用的 import
- 移除行尾空格，文件末尾换行

### 4.3 Checkstyle 配置（`backend/checkstyle.xml`）

| 规则 | 配置 |
|------|------|
| 文件长度 | 上限 500 行 |
| 方法长度 | 上限 150 行 |
| import 排序 | `java` → `javax` → `*` → `com.springboot`，分组间空行 |
| 禁止 `System.out.println` | 替代为 Logger |
| 禁止 `printStackTrace()` | 替代为 Logger |
| 嵌套深度 | if 最大 3 层，try 最大 3 层 |
| 其他 | `equals/hashCode` 配对、`@Override` 必须标注、空语句检测 |

**运行命令：**
```bash
# 自动格式化
mvn spotless:apply

# 检查格式规范
mvn spotless:check checkstyle:check
```

---

## 五、MainApplication.java 变更

移除了 `@SpringBootApplication(exclude = {RedisAutoConfiguration.class})` 中的 exclude，因为 Spring AMQP 依赖 Spring Boot 自动配置，且项目预期也需要 Redis。如需临时禁用 Redis，可通过配置 `spring.data.redis.repositories.enabled=false` 控制。

---

## 六、对现有功能链路的影响

### 6.1 前端功能链路（未变更）

- 前端 WebSocket 连接 (`/ws/alerts`) — **无变更**
- 前端 API 调用 (`/alert-records/*`, `/alerts/*`) — **无变更**
- YOLO → Backend 的 WebSocket 通道 (`/ws/ai-push`) — **无变更**
- YOLO → Backend 的 HTTP 回调 (`/internal/ai/events`) — **无变更**

### 6.2 新增功能链路（并行运行）

- YOLO Service 新增 RabbitMQ 发布通道 → Backend `AlertRecordConsumer` 消费
- Backend `/api/actuator/prometheus` 端点暴露 Prometheus 指标
- YOLO Service `:9091/metrics` 端点暴露 Prometheus 指标

### 6.3 向后兼容性

- 所有新增消费者和发布器都有条件化开关（`@ConditionalOnProperty`、配置属性）
- RabbitMQ 和 Prometheus 端点可以在本地开发时禁用
- 前端无需任何修改即可正常运行
- 即使 RabbitMQ 服务未启动，现有 HTTP 回调链路不受影响

---

## 七、部署说明

### 7.1 新增基础设施

需要部署 RabbitMQ：

```bash
# Docker 快速启动
docker run -d --name aqua-rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

### 7.2 环境配置

**Backend `application.yml` 需要配置：**

```yaml
spring:
  rabbitmq:
    host: <rabbitmq-host>
    port: 5672
    username: <username>
    password: <password>
```

**YOLO Service `.env` 需要新增：**

```
RABBITMQ_URL=amqp://localhost:5672/
RABBITMQ_EXCHANGE=alert.topic
METRICS_ENABLED=true
METRICS_PORT=9091
```

### 7.3 监控启动

```bash
# 启动 Prometheus + Grafana
docker compose -f docker-compose.monitoring.yml up -d
```

---

## 八、新增文件清单

### Backend (Java)

| 文件 | 说明 |
|------|------|
| `messaging/config/RabbitMQConfig.java` | RabbitMQ Exchange/Queue/Binding 配置 |
| `messaging/model/AlertEventMessage.java` | 报警事件消息体 |
| `messaging/serializer/MessageSerializer.java` | 序列化接口 |
| `messaging/serializer/JsonMessageSerializer.java` | JSON 序列化实现 |
| `messaging/consumer/AlertRecordConsumer.java` | 报警记录消费者 |
| `messaging/consumer/AlertNotificationConsumer.java` | 报警通知消费者（占位） |
| `messaging/consumer/AlertAnalyticsConsumer.java` | 报警分析消费者（占位） |
| `messaging/publisher/AlertEventPublisher.java` | 报警事件发布器 |
| `messaging/errorHandler/DeadLetterHandler.java` | 死信队列处理 |
| `metrics/event/AlertEventReceivedEvent.java` | 报警事件接收事件 |
| `metrics/event/AlertProcessingCompletedEvent.java` | 报警处理完成事件 |
| `metrics/event/DeviceStatusChangedEvent.java` | 设备状态变更事件 |
| `metrics/listener/MetricsEventListener.java` | 指标事件监听器 |
| `metrics/MetricsConfig.java` | Micrometer 指标 Bean 注册 |
| `checkstyle.xml` | Checkstyle 规则配置 |

### Backend 修改文件

| 文件 | 变更说明 |
|------|----------|
| `pom.xml` | 新增 AMQP/Actuator/Prometheus 依赖 + Spotless/Checkstyle 插件 |
| `application.yml` | 新增 RabbitMQ/Actuator/Prometheus/Messaging 配置 |
| `MainApplication.java` | 移除 RedisAutoConfiguration exclude |
| `InternalAiCallbackController.java` | 接入 Metrics 事件发布 |
| `AlertWsPublisher.java` | 新增 `ws.messages.sent` Counter |
| `InternalAiCallbackControllerTest.java` | 新增 ApplicationEventPublisher mock |

### YOLO Service (Python)

| 文件 | 说明 |
|------|------|
| `app/services/rabbitmq_publisher_service.py` | RabbitMQ 发布服务 |
| `app/metrics/__init__.py` | 指标包初始化 |
| `app/metrics/inference_metrics.py` | 推理指标 |
| `app/metrics/stream_metrics.py` | 流指标 |
| `app/metrics/task_metrics.py` | 任务指标 |

### YOLO Service 修改文件

| 文件 | 变更说明 |
|------|----------|
| `requirements.txt` | 新增 pika + prometheus_client |
| `app/core/config.py` | 新增 RabbitMQ 和 Metrics 配置项 |
| `app/__init__.py` | 启动 RabbitMQ 发布服务和 Prometheus 指标端点 |
| `app/services/engine_task_service.py` | 集成 RabbitMQ 发布和推理指标 |
| `app/services/callback_client_service.py` | 集成 HTTP 回调指标 |

### 基础设施

| 文件 | 说明 |
|------|------|
| `docker-compose.monitoring.yml` | Prometheus + Grafana Docker Compose |
| `monitoring/prometheus.yml` | Prometheus 抓取配置 |

---

## 九、端到端测试验证结果

### 9.1 测试环境

- RabbitMQ 3.12.13（本地安装，已运行）
- Backend Spring Boot 3.2.3（本地启动）
- Python 3.10 + pika（本地安装）

### 9.2 测试步骤与结果

| 测试项 | 结果 | 说明 |
|--------|------|------|
| RabbitMQ 连接 | ✅ 通过 | Backend 成功连接到 127.0.0.1:5672 |
| Exchange/Queue 声明 | ✅ 通过 | `alert.topic` Exchange + 3 个 Queue + 死信队列自动声明 |
| Python 发布消息 | ✅ 通过 | pika 发布消息到 `alert.topic` Exchange，routing_key=`alert.record` |
| 消息路由 | ✅ 通过 | 消息正确路由到 `alert.record.queue` |
| Backend 消费消息 | ✅ 通过 | `AlertRecordConsumer` 成功从 Queue 消费，队列消息数降为 0 |
| Prometheus 端点 | ✅ 通过 | `GET /api/actuator/prometheus` 正常暴露指标 |
| 指标采集 | ✅ 通过 | `alert_events_dropped_total`、`ws_connections_active`、`rabbitmq_consumed_total` 等指标正常 |

### 9.3 测试中发现并修复的问题

| 问题 | 原因 | 修复 |
|------|------|------|
| RabbitMQ 认证失败 | `application.yml` 中 `localhost` 解析为 IPv6 地址，且密码默认值为 `change_me` | host 改为 `127.0.0.1`，密码默认值改为 `guest` |
| 消息序列化失败 | `Jackson2JsonMessageConverter` 与 `String` 参数类型不兼容 | 移除全局 `Jackson2JsonMessageConverter`，消费者使用 `Message` 对象手动提取 body |
| 消费者 ack 模式 | `acknowledge-mode=manual` 但消费者未手动 ack | 改为 `acknowledge-mode=auto`，与 Spring retry 机制配合 |

### 9.4 2026-04-24 复测补充（前后端与数据库已启动）

1. **前端到后端链路**：通过 `http://localhost:5173/api/actuator/health`（Vite 代理）成功返回 Backend 健康信息，确认前端代理链路正常。
2. **Python→RabbitMQ→Java 消费链路**：
   - 发送测试消息 `eventUid=evt_retest2_1777040470`
   - `alert.record.queue` 消息数回到 `0`
   - `alert_events_received_total{source="rabbitmq"}` 增量 `+1`
   - `alert_events_dropped_total` 增量 `0`
   说明消息被 Java 消费者成功反序列化并处理。
3. **消息格式注意事项**：`publishedAt` 建议使用 ISO 时间字符串或可被 Java `Date` 正确解析的格式，避免使用 Python `time.time()` 产生的小数秒浮点格式。
4. **通知/分析消费者状态**：仍保持默认禁用（配置开关控制），这是阶段化设计，后续阶段启用并接入真实业务处理。

### 9.5 2026-04-24 二次复核（针对“是否真实端到端”）

1. **使用 YOLO 侧真实发布器复测**：通过 `app/services/rabbitmq_publisher_service.py` 发布消息，不再使用临时脚本直接 publish。
2. **修复并验证发布器兼容性问题**：
   - 问题：YOLO 发布器声明队列时只带 `x-message-ttl`，与 Backend 已声明队列（包含死信参数）不一致，触发 `PRECONDITION_FAILED`。
   - 修复：在 YOLO 发布器中补齐队列参数（`x-dead-letter-exchange`、`x-dead-letter-routing-key`）并先声明对应 `.dlq`。
   - 效果：发布器 `connected=true`、`published=true`，可稳定连上并发送到 `alert.record`。
3. **数据库落库证据**（最新一次复测）：
   - `monitoring_event.event_uid = evt_yolo_e2e_1777046285`
   - `monitoring_event.id = 13045`
   - `alert_record.id = 14042`，`alert_uid = ALERT-c05c63ee618d4607ab746c0b6092b8fe`
   - 说明“消息消费→写监控事件→写报警记录”链路已完整打通。
4. **消息队列状态**：`alert.record.queue = 0`、`alert.notification.queue = 0`、`alert.analytics.queue = 0`，无积压。
