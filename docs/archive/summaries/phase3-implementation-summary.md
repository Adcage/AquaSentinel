# 阶段三改造总结：AI 智能分析

> 实施日期：2026-04-28 ~ 2026-05-02
>
> 对应规划文档：`docs/archive/plans/backend-tech-upgrade-plan.md` 第 4 节

---

## 一、改造概览

本次阶段三改造为后端引入了 **AI 智能分析能力**，覆盖三大功能域：AI 对话、报警智能分析、报警语义搜索。所有功能通过 `app.ai.intelligence.enabled` 配置开关控制，可独立启停。

| 子模块 | 技术方案 | 核心能力 | 状态 |
|--------|---------|--------|------|
| 3a | Spring AI ChatClient + Function Calling | 自然语言查询报警数据、设备状态、救生员信息等 | ✅ 已完成 |
| 3b | Strategy 模式 + Spring Event | 自动报警智能分析（溺水/入侵） | ✅ 已完成 |
| 3c | Embedding + MySQL JSON 向量 + 余弦相似度 | 历史报警语义搜索 | ✅ 已完成 |

**改造前后对比：**

| | 改造前 | 改造后 |
|---|--------|--------|
| 报警描述 | 模板拼接 "`{zone}区域检测到溺水，置信度{score}`" | LLM 综合上下文生成自然语言分析，包含历史相似案例、场馆信息、处置建议 |
| 数据查询 | 固定页面筛选 | 自然语言提问 → LLM 调用 5 个 Function → 结构化回答 |
| 相似报警匹配 | 无 | 向量化存储 + 余弦相似度搜索，支持语义级别匹配 |
| 报警分析触发 | 无 | 报警创建后自动异步触发 AI 分析 + 向量化 |

---

## 二、架构设计

### 2.1 整体数据流

```
┌───────────────── 报警智能分析流程（异步，事件驱动）──────────────────┐
│                                                                    │
│  YOLO callback → InternalAiCallbackController.receiveEvent()     │
│       → 创建 AlertRecord + 推送通知                                │
│       → publishEvent(AlertAnalysisEvent)                          │
│           → AlertAnalysisListener [@Async]                        │
│               → resolveService(alertType)                          │
│                   → DrowningAlertAnalysisService                   │
│                   → InvasionAlertAnalysisService                   │
│               → 加载报警 + 相似案例 + 场馆信息                      │
│               → chatService.chatWithSystemPrompt(分析提示词)        │
│               → 更新 alert_record.ai_analysis                      │
│               → similarAlertSearchService.generateAndStoreEmbedding│
│                                                                    │
└────────────────────────────────────────────────────────────────────┘

┌───────────────── AI 对话流程（同步/SSE 流式）──────────────────────┐
│                                                                    │
│  用户消息 → AiChatController                                       │
│       → DefaultChatService.chat/chatStream()                       │
│           → 确保会话存在                                            │
│           → 保存用户消息                                            │
│           → 构建消息列表（系统提示词 + 历史消息 + 当前消息）           │
│           → ChatClient.prompt().messages().functions(5个).call/stream│
│           → 保存助手回复                                            │
│           → 返回 ChatResponse / SseEmitter                         │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘

┌───────────────── 语义搜索流程 ─────────────────────────────────────┐
│                                                                    │
│  用户查询 → AiChatController.searchSimilarAlerts()                │
│       → SimilarAlertSearchService.searchByText(queryText, topK)   │
│           → OpenAiEmbeddingModel.embed(queryText)                 │
│           → MySqlAlertVectorRepository.searchSimilar()            │
│               → 加载近 N 天嵌入向量                                  │
│               → JSON 反序列化为 float[]                             │
│               → 逐条计算余弦相似度                                   │
│               → 按 threshold 过滤，取 top-K                        │
│       → 映射为 SimilarAlertVO 返回                                  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 2.2 设计模式应用

**模式一：策略模式（Strategy）— 报警分析**

```
AlertAnalysisService（接口）
├── analyzeAlert(Long alertId) → String
├── getSupportedAlertType() → String
│
├── DrowningAlertAnalysisService  ← Bean: "drowningAlertAnalysisService", type: "DROWING"
├── InvasionAlertAnalysisService  ← Bean: "invasionAlertAnalysisService", type: "CROSS_BORDER"
│
AlertAnalysisListener.resolveService(alertType)
    → "DROWING"/"DROWNING" → drowningAlertAnalysisService
    → "CROSS_BORDER" → invasionAlertAnalysisService
    → default → drowningAlertAnalysisService
```

新增报警类型只需实现 `AlertAnalysisService` 并注册为 Spring Bean，`AlertAnalysisListener` 自动发现。

**模式二：仓库模式（Repository）— 向量存储抽象**

```
AlertVectorRepository（接口）
├── store(alertId, alertUid, sourceText, embedding, model)
├── getEmbedding(alertId) → float[]
├── searchSimilar(queryEmbedding, topK, threshold) → List<SimilarAlert>
├── delete(alertId)
│
└── MySqlAlertVectorRepository  ← JSON 字符串存储 + Java 余弦计算
    （后续可迁移到 PgVectorAlertVectorRepository，业务代码无需修改）
```

**模式三：模板方法（Template Method）— 分析提示词构建**

`DrowningAlertAnalysisService.buildPrompt()` 和 `InvasionAlertAnalysisService.buildPrompt()` 各自构建不同侧重点的提示词模板，但共享同一个分析流程（加载报警 → 加载相似案例 → 加载场馆 → 调用 LLM → 返回结果）。`DrowningAlertAnalysisService` 优先使用 `application.yml` 中的可配置模板（含 8 个占位符），回退到硬编码提示词；`InvasionAlertAnalysisService` 使用独立硬编码的入侵误报分析提示词。

---

## 三、新增依赖

### pom.xml 新增依赖

```xml
<!-- Spring AI OpenAI Starter -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Spring AI BOM（版本管理） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0-M5</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### Frontend 新增依赖

```json
{
  "marked": "^15.0.7",
  "dompurify": "^3.2.6"
}
```

---

## 四、配置详解

### 4.1 application.yml 新增配置

```yaml
spring:
  ai:
    openai:
      api-key: ${AI_API_KEY:}
      base-url: ${AI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${AI_CHAT_MODEL:gpt-4o}
          temperature: 0.3
      embedding:
        options:
          model: ${AI_EMBEDDING_MODEL:text-embedding-v4}

app:
  ai:
    intelligence:
      enabled: true
      chat-system-prompt: |
        你是 AquaSentinel 水上安全智能助手。你的职责是：
        1. 帮助管理员查询和分析报警数据
        2. 提供水上安全相关的专业建议
        3. 生成安全报告和趋势分析

        回答规则：
        - 始终基于事实数据回答，不要编造信息
        - 如果数据库中查不到相关数据，明确告知用户
        - 涉及设备控制操作时，必须先征得用户确认
        - 所有回复使用中文
      alert-analysis-prompt-template: |
        请根据以下报警信息生成一段智能分析描述：

        报警类型：{alertType}
        报警位置：{incidentLocation}
        检测结果：{detectionResult}
        置信度：{confidence}
        持续时间：{durationSec}秒
        触发规则：{ruleHits}
        场馆信息：{venueName}
        历史相似报警数量：{similarAlertCount}

        请生成一段2-3句话的分析描述，包含：
        1. 事件严重程度判断
        2. 与历史报警的关联分析
        3. 建议采取的措施
      embedding:
        enabled: true
        similarity-threshold: 0.5
        max-similar-results: 5
        recent-days-limit: 30
      chat-history:
        max-conversations-per-user: 50
        max-messages-per-conversation: 100
```

**关键设计决策**：

- 系统提示词和分析模板均可配置化，运维人员可调整 LLM 行为而无需改代码
- `similarity-threshold: 0.5`（而非默认值 0.7）：经实际测试，`text-embedding-v4` 模型对中文水上安全领域查询的相似度分数通常在 0.4–0.6 之间，0.7 会过滤掉大部分相关结果
- `temperature: 0.3`：低温度确保 AI 回复稳定、一致，适合数据分析场景
- `recent-days-limit: 30`：语义搜索只扫描最近 30 天的嵌入向量，避免全表扫描

### 4.2 application-local.yml（gitignored）

```yaml
spring:
  ai:
    openai:
      base-url: https://coding.dashscope.aliyuncs.com
      api-key: ${AI_API_KEY}
      chat:
        options:
          model: kimi-k2.5
      embedding:
        base-url: https://dashscope.aliyuncs.com/compatible-mode
        api-key: ${AI_EMBEDDING_API_KEY}
        options:
          model: text-embedding-v4
```

**两个 DashScope 端点说明**：

| 端点 | 用途 | 模型 | 说明 |
|------|------|------|------|
| `coding.dashscope.aliyuncs.com` | Chat | `kimi-k2.5` | 阿里云 Coding 子域，仅支持 `/v1/chat/completions`，使用 Coding API Key |
| `dashscope.aliyuncs.com/compatible-mode` | Embedding | `text-embedding-v4` | 标准 DashScope 兼容模式，支持 `/v1/embeddings`，使用标准 API Key |

### 4.3 AppAiIntelligenceProperties 配置类

**文件**：`com.springboot.config.AppAiIntelligenceProperties`

绑定 `app.ai.intelligence` 配置节：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | AI 智能功能总开关 |
| `chatSystemPrompt` | String | "你是AquaSentinel水上安全智能助手。" | 对话系统提示词 |
| `alertAnalysisPromptTemplate` | String | null | 报警分析模板（含占位符） |
| `embedding.enabled` | boolean | true | 向量搜索开关 |
| `embedding.similarityThreshold` | double | 0.7 | 相似度阈值（YAML 覆盖为 0.5） |
| `embedding.maxSimilarResults` | int | 5 | 返回最多相似结果数 |
| `embedding.recentDaysLimit` | int | 30 | 搜索最近 N 天的向量 |
| `chatHistory.maxConversationsPerUser` | int | 50 | 每用户最大会话数 |
| `chatHistory.maxMessagesPerConversation` | int | 100 | 每会话最大消息数 |

---

## 五、AI 对话模块详解

### 5.1 SpringAiConfig — ChatClient 与 Embedding 配置

**文件**：`com.springboot.ai.config.SpringAiConfig`

**条件化加载**：`@ConditionalOnProperty(name="app.ai.intelligence.enabled", havingValue="true", matchIfMissing=true)`

**Bean 清单**：

| Bean | 类型 | 说明 |
|------|------|------|
| `chatClient` | `ChatClient` | 基于 `OpenAiChatModel` 构建，供 `DefaultChatService` 使用 |
| `openAiEmbeddingModel` | `OpenAiEmbeddingModel`（`@Primary`） | 独立的 Embedding 模型 Bean，使用独立的 `base-url` 和 `api-key` |

**为什么需要独立的 Embedding Bean**：

Spring AI M5 的 `OpenAiAutoConfiguration` 使用统一的 `base-url` 和 `api-key` 创建 `OpenAiApi`，然后同时用于 Chat 和 Embedding。但本项目 Chat 和 Embedding 使用不同的 DashScope 端点（coding 子域 vs 标准 API），因此需要：

1. 让 Spring AI 自动配置处理 Chat（使用 `spring.ai.openai.base-url` 和 `spring.ai.openai.api-key`）
2. 手动创建独立的 `OpenAiEmbeddingModel` Bean，使用 `spring.ai.openai.embedding.base-url` 和 `spring.ai.openai.embedding.api-key`

**ContentTypeFixingResponse 内部类**：

DashScope Embedding API 有时会返回 `Content-Type: application/octet-stream` 而非 `application/json`，导致 Spring AI 的 `OpenAiEmbeddingModel` 解析失败。该内部类装饰 `ClientHttpResponse`，强制将 `Content-Type` 覆写为 `application/json`：

```java
private static class ContentTypeFixingResponse implements ClientHttpResponse {
    private final ClientHttpResponse delegate;
    
    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(delegate.getHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    // ... 其他方法委托给 delegate
}
```

此装饰器通过 `RestClient.Builder.requestInterceptor` 注入到 Embedding 模型的 HTTP 请求中。

### 5.2 ChatService 接口与 DefaultChatService 实现

**接口**：`com.springboot.ai.chat.ChatService`

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `chat(userId, conversationId, userMessage)` | `ChatResponse` | 同步对话，等待 LLM 完整响应 |
| `chatStream(userId, conversationId, userMessage)` | `SseEmitter` | SSE 流式对话，逐 token 推送 |
| `chatWithSystemPrompt(systemPrompt, userMessage)` | `String` | 无上下文对话，用于报警分析 |

**DefaultChatService 核心流程**：

#### `chat()` 同步对话

```
1. ensureConversation() → 若 conversationId 为空，创建新会话
2. 保存用户消息到 ai_chat_message
3. buildSpringAiMessages() → 构建消息列表：
   a. 系统提示词（来自配置）
   b. 历史消息（截断至 maxMessagesPerConversation 条）
   c. 当前用户消息
4. chatClient.prompt().messages(messages)
   .functions("getAlertRecords", "getDeviceStatus", "getLifeguardOnDuty", "getStatsSnapshot", "getMonitorTasks")
   .call().content()
5. 保存助手回复到 ai_chat_message
6. 返回 ChatResponse（含 conversationId、message、functionName 等）
```

#### `chatStream()` SSE 流式对话

```
1. 同上 1-3 构建消息列表
2. 创建 SseEmitter（超时 120 秒）
3. chatClient.prompt().messages(messages).functions(...).stream().content()
   → Flux<String> 逐 token 回调
4. emitter.send(chunk) 逐块推送给前端
5. doOnComplete(): 保存完整助手回复到 ai_chat_message
6. doOnError(): 发送降级消息 "[连接中断，请重试]"
```

**关键修复**：初版 `chatStream()` 在 `doOnComplete()` 中保存空字符串，原因是 `StringBuilder` 在流式回调中正确累积，但保存时未使用累积值。修复后使用 `StringBuilder.toString()` 获取完整响应。

#### `chatWithSystemPrompt()` 上下文无关对话

```
chatClient.prompt()
    .system(systemPrompt)     // 报警分析专用的系统提示词
    .user(userMessage)        // 包含报警详情的用户提示词
    .call().content()         // 无历史，无 Function Calling
```

### 5.3 AiChatController — REST API

**文件**：`com.springboot.ai.controller.AiChatController`

**路由前缀**：`/ai`

**条件化加载**：`@ConditionalOnProperty(name="app.ai.intelligence.enabled", havingValue="true", matchIfMissing=true)`

| 端点 | 方法 | 认证 | 限流 | 说明 |
|------|------|------|------|------|
| `POST /ai/chat` | 同步对话 | `VENUE_ADMIN` | 10/60s/USER | 返回完整 AI 响应 |
| `GET /ai/chat/stream` | SSE 流式 | `token` 参数 | 10/60s/USER | 逐 token 推送 |
| `GET /ai/conversations` | 列表 | `VENUE_ADMIN` | — | 获取用户会话列表 |
| `POST /ai/conversations` | 创建 | `VENUE_ADMIN` | — | 创建新会话 |
| `GET /ai/conversations/{id}/messages` | 消息列表 | `VENUE_ADMIN` | — | 获取会话消息 |
| `DELETE /ai/conversations/{id}` | 删除 | `VENUE_ADMIN` | — | 软删除会话 |
| `POST /ai/alerts/{alertId}/analyze` | 分析 | `VENUE_ADMIN` | 5/60s/USER | AI 分析 + 生成嵌入向量 |
| `POST /ai/alerts/search-similar` | 语义搜索 | `VENUE_ADMIN` | 5/60s/USER | 查找相似报警 |

**SSE 端点认证处理**：

`GET /ai/chat/stream` 使用 URL 参数 `token` 认证（`?token=xxx`），而非 HTTP Header。原因是浏览器 `EventSource` API 不支持自定义请求头。Token 通过 `JwtTokenProvider.parseAccessToken(token)` 解析。

---

## 六、报警智能分析模块详解

### 6.1 事件驱动分析流程

**触发点**：`InternalAiCallbackController.receiveEvent()`

在报警记录创建 + WebSocket 推送 + 指标事件发布之后，新增：

```java
applicationEventPublisher.publishEvent(
    new AlertAnalysisEvent(this, alertRecord.getId(), alertRecord.getAlert_type()));
```

**异步处理**：`AlertAnalysisListener.onAlertAnalysisRequest()`

```
接收 AlertAnalysisEvent
  ↓
1. resolveService(alertType) → 选择分析服务
2. analyzeAlert(alertId) → 调用 LLM 生成分析
3. 更新 alert_record.ai_analysis = 分析结果
4. similarAlertSearchService.generateAndStoreEmbedding(alert) → 生成并存储向量
```

整个流程通过 `@Async` 异步执行，不阻塞报警写入和推送。分析失败仅记录日志，不影响报警数据落库。

**最佳实践**：无论报警分析是否成功，都会尝试生成嵌入向量（best-effort）。这样可以确保即使 LLM 调用失败（如 API 限流、网络错误），向量化数据仍然有基础信息可供语义搜索。

### 6.2 DrowningAlertAnalysisService — 溺水报警分析

**Bean 名**：`drowningAlertAnalysisService`

**分析流程**：

```
1. 加载 AlertRecord（报警类型、位置、检测结果、置信度、持续时间等）
2. 加载相似报警：similarAlertSearchService.searchSimilar(alertId, 5)
3. 加载场馆信息：venueService.getById(venueId)
4. 构建 Prompt：
   a. 优先使用 YAML 配置的 alertAnalysisPromptTemplate
   b. 替换占位符：{alertType}, {incidentLocation}, {detectionResult}, {confidence}, {durationSec}, {ruleHits}, {venueName}, {similarAlertCount}
   c. 若模板为空，使用硬编码的溺水分析提示词
5. chatService.chatWithSystemPrompt(systemPrompt, prompt)
6. 返回 AI 生成的分析文本
```

### 6.3 InvasionAlertAnalysisService — 入侵报警分析

**Bean 名**：`invasionAlertAnalysisService`

与溺水分析流程相同，但使用独立的入侵误报分析提示词，侧重于：

- 当前时间段该区域是否开放
- 入侵行为的误报判断
- 是否有正常进入的记录

---

## 七、语义搜索模块详解

### 7.1 AlertVectorRepository 接口

```java
public interface AlertVectorRepository {
    void store(Long alertId, String alertUid, String sourceText, float[] embedding, String model);
    float[] getEmbedding(Long alertId);
    List<SimilarAlert> searchSimilar(float[] queryEmbedding, int topK, double threshold);
    void delete(Long alertId);

    record SimilarAlert(Long alertId, String alertUid, double similarity) {}
}
```

### 7.2 MySqlAlertVectorRepository — MySQL JSON 向量实现

**为什么用 MySQL JSON 而非 PGVector**：

| 对比项 | MySQL JSON | PGVector | 专业向量库 (Milvus) |
|--------|-----------|----------|-------------------|
| 运维复杂度 | 零（已有 MySQL） | 需迁移到 PostgreSQL | 需独立部署 |
| 数据量支持 | 十万级 | 百万级 | 千万级 |
| 检索性能 | Java 内存计算 | 数据库层计算 | 专用索引 |
| 迁移成本 | 无 | 高 | 中 |
| 当前适用性 | ✅ 足够 | — | — |

当前报警数据量在万级，MySQL JSON 方案完全够用。通过仓库模式抽象，后续可无缝迁移到 PGVector。

**核心实现**：

- **存储**：`float[]` → JSON 数组字符串 → 存入 `alert_embedding.embedding` 列（MySQL JSON 类型）
- **查询**：加载近 N 天的嵌入向量记录，反序列化为 `float[]`，Java 内存计算余弦相似度
- **Upsert**：写入前检查 `alert_id` 是否已存在，存在则更新 `embedding` 列，不存在则新建

**余弦相似度计算**：

```java
private double cosineSimilarity(float[] a, float[] b) {
    double dotProduct = 0.0, normA = 0.0, normB = 0.0;
    for (int i = 0; i < a.length; i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-8);
}
```

### 7.3 SimilarAlertSearchService — 语义搜索服务

三种核心操作：

| 方法 | 入参 | 流程 | 用途 |
|------|------|------|------|
| `searchSimilar(alertId, maxResults)` | 报警 ID | 加载该报警的嵌入向量 → 搜索相似 | 报警分析时查找历史案例 |
| `searchByText(queryText, maxResults)` | 自然语言文本 | 文本向量化 → 搜索相似 | 用户输入语义查询 |
| `generateAndStoreEmbedding(alert)` | 报警记录 | 拼接 `alert_type + incident_location + detection_result + ai_analysis` → 向量化 → 存储 | 新报警入库时生成向量 |

**sourceText 构建策略**：

```
sourceText = alert.getAlert_type() + " " + alert.getIncident_location() + " " + alert.getDetection_result() + " " + alert.getAi_analysis()
```

将结构化字段和 AI 分析结果拼接为完整文本，兼顾关键信息检索和语义分析。

---

## 八、Function Calling 详解

LLM 通过 Spring AI Function Calling 机制调用后端预定义的 5 个 Function，实现自然语言查询数据库：

### 8.1 Function 注册列表

| Bean 名 | 类 | 说明 | 参数 |
|---------|-----|------|------|
| `getAlertRecords` | `AlertQueryFunction` | 查询报警记录 | venueId, dateRange, alertType, alertStatus, page, pageSize |
| `getDeviceStatus` | `DeviceStatusFunction` | 查询设备状态 | venueId, cameraCode, page, pageSize |
| `getLifeguardOnDuty` | `LifeguardQueryFunction` | 查询值班救生员 | venueId, dutyStatus, page, pageSize |
| `getStatsSnapshot` | `StatsQueryFunction` | 查询统计数据 | venueId, dateRange, days, page, pageSize |
| `getMonitorTasks` | `MonitorTaskQueryFunction` | 查询推理任务状态 | venueId, taskCode, status, page, pageSize |

### 8.2 Function 通用模式

所有 Function 均遵循 Spring AI 的 `Function<Request, Response>` 接口：

```java
@Component("getAlertRecords")
@Description("查询报警记录。可按场馆ID、日期范围、报警类型筛选。返回报警摘要列表。")
public class AlertQueryFunction implements Function<AlertQueryFunction.Request, AlertQueryFunction.Response> {
    @Resource
    private AlertRecordService alertRecordService;

    @Override
    public Response apply(Request request) {
        // 构建查询条件，调用 Service，返回结果
    }
}
```

**关键设计**：
- 所有查询带 `is_delete=0` 过滤
- 分页上限保护（`pageSize` 最大 20/30）
- 返回摘要而非完整实体（如 `detection_result` 截断至 100 字符）
- `LifeguardQueryFunction` 默认筛选 `ON_DUTY` + `APPROVED` 状态

### 8.3 Function Calling 调用流程

```
用户: "3号泳池昨天的报警情况"
  ↓
ChatClient 将消息发送给 LLM
  ↓
LLM 判断需要调用 getAlertRecords(venueId=null, dateRange="昨天", alertType=null)
  ↓
Spring AI 框架自动路由到 AlertQueryFunction.apply()
  ↓
Function 返回结构化报警数据
  ↓
LLM 基于返回数据组织自然语言回复
  ↓
"3号泳池昨天共发生4次报警，其中溺水检测3次、区域入侵1次……"
```

---

## 九、数据库变更

### 9.1 迁移脚本

**文件**：`backend/sql/migration_20260428_ai_intelligence.sql`

#### 表 1：alert_record 表新增列

```sql
ALTER TABLE alert_record 
ADD COLUMN ai_analysis TEXT COMMENT 'AI智能分析结果' AFTER detection_result;
```

#### 表 2：alert_embedding（报警向量嵌入）

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| alert_id | BIGINT NOT NULL | 关联报警 ID（唯一约束） |
| alert_uid | VARCHAR(64) NOT NULL | 报警 UID |
| source_text | TEXT NOT NULL | 原始文本 |
| embedding | JSON NOT NULL | 向量数据（JSON 数组） |
| embedding_model | VARCHAR(128) NOT NULL | 模型名称 |
| similarity_search_text | TEXT | 搜索用文本 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_delete | TINYINT DEFAULT 0 | 软删除 |

索引：`uk_alert_id`（唯一）、`idx_alert_uid`、`idx_created_at`

#### 表 3：ai_chat_conversation（AI 对话会话）

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| user_id | BIGINT NOT NULL | 用户 ID |
| title | VARCHAR(128) | 会话标题 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_delete | TINYINT DEFAULT 0 | 软删除 |

索引：`idx_user_id`、`idx_updated_at`

#### 表 4：ai_chat_message（AI 对话消息）

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| conversation_id | BIGINT NOT NULL | 会话 ID |
| role | VARCHAR(20) NOT NULL | 角色（user/assistant/function） |
| content | TEXT NOT NULL | 消息内容 |
| function_name | VARCHAR(64) | Function 名称 |
| function_args | TEXT | Function 参数 |
| function_result | TEXT | Function 返回值 |
| tokens_used | INT | Token 用量 |
| created_at | DATETIME | 创建时间 |
| is_delete | TINYINT DEFAULT 0 | 软删除 |

索引：`idx_conversation_id`、`idx_created_at`

所有表遵循项目约定：BIGINT AUTO_INCREMENT 主键、is_delete 软删除、created_at/updated_at 时间戳、snake_case 列名、中文表注释、utf8mb4_unicode 排序规则。

---

## 十、前端实现

### 10.1 AiChatPanel.vue

**文件**：`frontend/src/components/business/AiChatPanel.vue`

465 行 Vue 3 Composition API 组件，`<script setup lang="ts">`。

| 特性 | 实现 |
|------|------|
| 欢迎界面 | 3 个快捷问题按钮 |
| 消息列表 | 按角色区分样式（user 蓝色右对齐、assistant 灰色左对齐、function 浅蓝色） |
| Markdown 渲染 | `marked` 解析 + `DOMPurify.sanitize` 防注入 |
| SSE 流式返回 | `chatStream()` + `ReadableStream` 逐 token 累积 |
| 自动滚动 | `scrollToBottom()` 在新消息到达时触发 |
| 加载指示 | 3 点动画 |
| 会话管理 | 首次对话自动创建会话 |

### 10.2 aiChatService.ts

| 函数 | HTTP 方法 | 端点 |
|------|-----------|------|
| `chat(params)` | POST | `/ai/chat` |
| `chatStream(...)` | GET (SSE) | `/ai/chat/stream` |
| `listConversations()` | GET | `/ai/conversations` |
| `createConversation(params)` | POST | `/ai/conversations` |
| `getMessages(id)` | GET | `/ai/conversations/{id}/messages` |
| `deleteConversation(id)` | DELETE | `/ai/conversations/{id}` |
| `analyzeAlert(id)` | POST | `/ai/alerts/{id}/analyze` |
| `searchSimilarAlerts(params)` | POST | `/ai/alerts/search-similar` |

### 10.3 business.ts 类型定义

新增 `AiConversation`、`AiChatMessageItem`、`SimilarAlertItem` 三个接口。

---

## 十一、YAML 配置缩进 Bug 修复

**问题**：`application.yml` 中 `embedding:` 和 `chat-history:` 配置节最初缩进在 `app.ai` 下而非 `app.ai.intelligence` 下，导致 `AppAiIntelligenceProperties` 的嵌套属性 `similarityThreshold`、`maxSimilarResults`、`recentDaysLimit`、`maxConversationsPerUser`、`maxMessagesPerConversation` 全部使用 Java 默认值而未被 YAML 值覆盖。

**影响**：`similarityThreshold` 实际为 Java 默认值 `0.7` 而非配置文件中的 `0.5`，过滤掉了大部分相关搜索结果（典型中文查询相似度分数在 0.4–0.6 之间）。

**修复**：将 `embedding:` 和 `chat-history:` 的缩进从 `app.ai` 层级修正到 `app.ai.intelligence` 层级。

```yaml
# 修复前（错误）
app:
  ai:
    intelligence:
      enabled: true
    embedding:          # ← 错误层级
      similarity-threshold: 0.5

# 修复后（正确）
app:
  ai:
    intelligence:
      enabled: true
      embedding:        # ← 正确层级
        similarity-threshold: 0.5
```

---

## 十二、开发过程中修复的 Bug

| Bug | 原因 | 修复 | 影响 |
|-----|------|------|------|
| AI Chat 404 | `base-url` 包含 `/v1` 后缀，Spring AI M5 自动追加 `/v1/chat/completions`，导致 `/v1/v1/chat/completions` | 移除 `base-url` 中的 `/v1` 后缀 | 修复前 AI 对话无法使用 |
| Embedding API 404 | DashScope Coding 子域不支持 `/v1/embeddings` 端点 | 创建独立的 `@Primary OpenAiEmbeddingModel` Bean，指向 `dashscope.aliyuncs.com/compatible-mode` | 修复前语义搜索完全不可用 |
| Embedding 模型消失 | `text-embedding-3-small` 不被 DashScope 支持 | 模型改为 `text-embedding-v4`（DashScope 最新嵌入模型，1024 维） | 修复前嵌入生成报错 |
| 语义搜索返回 0 结果 | YAML 中 `embedding:` 缩进层级错误，`similarityThreshold` 使用 Java 默认值 0.7 | 修正缩进到 `app.ai.intelligence.embedding` 下，使配置值 0.5 生效 | 修复前所有语义搜索结果被过滤 |
| AlertAnalysisListener 保存空嵌入 | 初版仅创建 `embedding_model="pending"` 的空记录，未调用向量化 | 改为调用 `similarAlertSearchService.generateAndStoreEmbedding(alert)` | 修复前嵌入向量为空，语义搜索无法匹配 |
| SSE 流式响应保存空字符串 | `chatStream()` 的 `doOnComplete()` 中保存了空 StringBuilder | 累积完整响应后再保存 | 修复前端对话历史显示空消息 |
| DashScope Embedding 返回 `octet-stream` | DashScope Embedding API 偶尔返回 `Content-Type: application/octet-stream` | 新增 `ContentTypeFixingResponse` 内部类，强制覆写为 `application/json` | 修复前间歇性嵌入生成失败 |

---

## 十三、关键设计决策

### 13.1 为什么 Chat 和 Embedding 使用不同端点

DashScope 平台有两个不同的 API 入口：

| 能力 | 端点 | 支持的模型 | API Key |
|------|------|-----------|---------|
| Chat | `coding.dashscope.aliyuncs.com` | `kimi-k2.5` 等对话模型 | Coding API Key (`sk-sp-...`) |
| Embedding | `dashscope.aliyuncs.com/compatible-mode` | `text-embedding-v4` 等嵌入模型 | 标准 API Key (`sk-4b5401...`) |

Coding 子域仅支持 `/v1/chat/completions`，不支持 `/v1/embeddings`。因此必须为 Embedding 创建独立的 `OpenAiEmbeddingModel` Bean，使用不同的 `base-url` 和 `api-key`。

### 13.2 为什么相似度阈值是 0.5 而非 0.7

经实际测试，`text-embedding-v4` 模型在中文水上安全领域查询中，相关报警的余弦相似度分数通常在 0.4–0.6 之间。阈值为 0.7 时几乎无结果返回。0.5 在召回率和准确率之间取得平衡。

### 13.3 为什么报警分析使用 @Async 异步

报警分析依赖 LLM API 调用（通常需要 3-10 秒），如果在 `InternalAiCallbackController.receiveEvent()` 中同步执行，会阻塞报警接收流程。使用 `@Async + @EventListener` 实现异步分析：

- 报警写入和推送立即完成（毫秒级）
- AI 分析在后台线程池中执行（秒级）
- 分析结果写入 `alert_record.ai_analysis` 列
- 分析失败不影响报警数据完整性

### 13.4 为什么 SSE 流式端点使用 URL 参数认证

浏览器 `EventSource` API 不支持自定义 HTTP 请求头，因此 `GET /ai/chat/stream` 通过 URL 参数 `?token=xxx` 传递 JWT Token，而非 `Authorization` 头。服务端通过 `JwtTokenProvider.parseAccessToken(token)` 解析。

### 13.5 为什么使用 MySQL JSON 存储向量而非专业向量库

详见第七章 7.2 节。核心原因：当前数据量在万级，MySQL JSON 方案零额外运维成本，通过仓库模式抽象可平滑迁移到 PGVector 或专业向量库。

---

## 十四、对现有功能链路的影响

### 14.1 报警接收链路

- `InternalAiCallbackController.receiveEvent()` 末尾新增 `publishEvent(AlertAnalysisEvent)`
- `AlertAnalysisListener` 异步处理，不阻塞报警写入和推送
- 报警分析失败仅记录日志，不影响报警数据完整性

### 14.2 前端功能链路

- 前端新增 `AiChatPanel.vue` 组件和 `aiChatService.ts` 服务
- 现有页面无需修改，AI 聊天面板作为独立组件集成
- 新增 `marked` 和 `dompurify` 两个依赖

### 14.3 向后兼容性

- 所有 AI 功能通过 `app.ai.intelligence.enabled` 开关控制，设为 `false` 则整个 AI 模块不加载
- Embedding 搜索通过 `app.ai.intelligence.embedding.enabled` 独立开关控制
- 即使 AI API Key 未配置，应用仍可正常启动（Spring AI 自动配置属性均为空默认值）
- AI 分析结果存储在 `alert_record.ai_analysis` 列，前端可选择性展示

---

## 十五、新增文件清单

### Backend (Java) — 新增 24 个文件

| 文件 | 包路径 | 说明 |
|------|--------|------|
| `SpringAiConfig.java` | `com.springboot.ai.config` | Spring AI 配置（ChatClient、Embedding 模型、ContentType 修復） |
| `ChatService.java` | `com.springboot.ai.chat` | 聊天服务接口 |
| `DefaultChatService.java` | `com.springboot.ai.chat` | 聊天服务默认实现 |
| `AiChatController.java` | `com.springboot.ai.controller` | AI REST API 控制器（部分放在 `ai.controller`，部分放在 `controller`） |
| `AlertAnalysisService.java` | `com.springboot.ai.analysis` | 报警分析服务接口 |
| `DrowningAlertAnalysisService.java` | `com.springboot.ai.analysis` | 溺水报警分析实现 |
| `InvasionAlertAnalysisService.java` | `com.springboot.ai.analysis` | 入侵报警分析实现 |
| `AlertAnalysisEvent.java` | `com.springboot.ai.analysis` | 报警分析 Spring Event |
| `AlertAnalysisListener.java` | `com.springboot.ai.analysis` | 报警分析事件监听器 |
| `AlertVectorRepository.java` | `com.springboot.ai.embedding` | 向量仓库接口 |
| `MySqlAlertVectorRepository.java` | `com.springboot.ai.embedding` | MySQL JSON 向量仓库实现 |
| `SimilarAlertSearchService.java` | `com.springboot.ai.embedding` | 语义搜索服务 |
| `AlertQueryFunction.java` | `com.springboot.ai.function` | 报警查询 Function |
| `DeviceStatusFunction.java` | `com.springboot.ai.function` | 设备状态查询 Function |
| `LifeguardQueryFunction.java` | `com.springboot.ai.function` | 救生员查询 Function |
| `StatsQueryFunction.java` | `com.springboot.ai.function` | 统计数据查询 Function |
| `MonitorTaskQueryFunction.java` | `com.springboot.ai.function` | 推理任务查询 Function |
| `AppAiIntelligenceProperties.java` | `com.springboot.config` | AI 智能配置属性类 |
| `AiChatConversation.java` | `com.springboot.model.entity` | 对话会话实体 |
| `AiChatMessage.java` | `com.springboot.model.entity` | 对话消息实体 |
| `AlertEmbedding.java` | `com.springboot.model.entity` | 报警嵌入向量实体 |
| `ChatRequest.java` | `com.springboot.model.dto.ai` | 对话请求 DTO |
| `ChatResponse.java` | `com.springboot.model.dto.ai` | 对话响应 DTO |
| `CreateConversationRequest.java` | `com.springboot.model.dto.ai` | 创建会话请求 DTO |
| `SimilarSearchRequest.java` | `com.springboot.model.dto.ai` | 语义搜索请求 DTO |
| `AiChatMessageVO.java` | `com.springboot.model.vo` | 对话消息 VO |
| `ConversationSummaryVO.java` | `com.springboot.model.vo` | 会话摘要 VO |
| `SimilarAlertVO.java` | `com.springboot.model.vo` | 相似报警 VO |
| `AiChatConversationService.java` | `com.springboot.service` | 对话会话服务接口 |
| `AiChatConversationServiceImpl.java` | `com.springboot.service.impl` | 对话会话服务实现 |
| `AiChatMessageService.java` | `com.springboot.service` | 对话消息服务接口 |
| `AiChatMessageServiceImpl.java` | `com.springboot.service.impl` | 对话消息服务实现 |
| `AlertEmbeddingService.java` | `com.springboot.service` | 报警嵌入服务接口 |
| `AlertEmbeddingServiceImpl.java` | `com.springboot.service.impl` | 报警嵌入服务实现 |
| `AiChatConversationMapper.java` | `com.springboot.mapper` | 会话 Mapper |
| `AiChatMessageMapper.java` | `com.springboot.mapper` | 消息 Mapper |
| `AlertEmbeddingMapper.java` | `com.springboot.mapper` | 嵌入向量 Mapper |

### Backend — 修改文件

| 文件 | 变更说明 |
|------|----------|
| `pom.xml` | 新增 `spring-ai-openai-spring-boot-starter` 和 `spring-ai-bom` 依赖 |
| `application.yml` | 新增 `spring.ai.openai` 和 `app.ai.intelligence` 配置节 |
| `InternalAiCallbackController.java` | 新增 `AlertAnalysisEvent` 事件发布 |
| `ErrorCode.java` | 新增 AI 相关错误码（如有） |

### Frontend — 新增文件

| 文件 | 说明 |
|------|------|
| `src/components/business/AiChatPanel.vue` | AI 聊天面板组件（465 行） |
| `src/services/aiChatService.ts` | AI 聊天服务（含 SSE 流式） |

### Frontend — 修改文件

| 文件 | 变更说明 |
|------|----------|
| `package.json` | 新增 `marked` 和 `dompurify` 依赖 |
| `src/types/business.ts` | 新增 `AiConversation`、`AiChatMessageItem`、`SimilarAlertItem` 接口 |

### SQL

| 文件 | 说明 |
|------|------|
| `backend/sql/migration_20260428_ai_intelligence.sql` | 数据库迁移脚本（4 条操作） |

---

## 十六、端到端测试验证结果

### 16.1 AI 同步对话

```bash
curl -X POST http://localhost:7529/api/ai/chat \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"message": "今天有多少次报警？"}'
```

**结果**：✅ AI 返回结构化的报警查询结果（通过 Function Calling 查询数据库后组织自然语言回复）

### 16.2 AI SSE 流式对话

```bash
curl -N "http://localhost:7529/api/ai/chat/stream?token={token}&conversationId=1&message=3号泳池的设备状态如何"
```

**结果**：✅ 逐 token 返回 SSE 事件流，完整响应被保存到 `ai_chat_message`

### 16.3 报警智能分析

```bash
curl -X POST http://localhost:7529/api/ai/alerts/{alertId}/analyze \
  -H "Authorization: Bearer {token}"
```

**结果**：✅ 返回 AI 生成的分析文本，同时生成嵌入向量并存储到 `alert_embedding`

### 16.4 语义搜索

```bash
curl -X POST http://localhost:7529/api/ai/alerts/search-similar \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"query": "泳池溺水", "maxResults": 5}'
```

**结果**：✅ 返回相似报警列表（含相似度分数），阈值 0.5 下可匹配到相关历史报警

### 16.5 会话管理

| 操作 | 结果 |
|------|------|
| 创建会话 | ✅ 返回 `ConversationSummaryVO` |
| 列表查询 | ✅ 按 `updated_at` 降序分页 |
| 消息历史 | ✅ 按 `created_at` 升序排列 |
| 删除会话 | ✅ 软删除 |

### 16.6 异步报警分析

- YOLO 回调触发 `AlertAnalysisEvent` → `AlertAnalysisListener` 异步处理 ✅
- 报警分析结果写入 `alert_record.ai_analysis` ✅
- 同时生成嵌入向量存入 `alert_embedding` ✅
- 分析失败不影响报警写入和推送 ✅

---

## 十七、后续待办

| 待办项 | 优先级 | 说明 |
|--------|--------|------|
| 前端 AI 聊天页面集成 | 高 | 将 `AiChatPanel.vue` 集成到实际页面中，需确认放置位置 |
| AI 端点集成/单元测试 | 中 | 为 Chat、Analysis、Search 三个端点编写测试 |
| 告警 Prompt 模板优化 | 低 | 当前硬编码提示词可进一步调优 |
| 向量搜索迁移到 PGVector | 低 | 数据量增长后迁移到 PostgreSQL + PGVector |
| Embedding 增量更新 | 低 | 定期对已有但未生成嵌入的报警批量生成 |
| Function Calling 扩展 | 低 | 新增 PTZ 控制 Function（需确认机制） |