# 阶段二改造总结：限流与容错双层防护

> 实施日期：2026-04-28
>
> 对应规划文档：`docs/archive/plans/backend-tech-upgrade-plan.md` 第 3 节

---

## 一、改造概览

本次阶段二改造为后端引入了 **三层防护机制**，从粗到细逐级过滤恶意或过载请求，并为外部服务调用增加容错与降级能力：

| 层级   | 组件                             | 粒度         | 技术方案                                            | 状态     |
| ------ | -------------------------------- | ------------ | --------------------------------------------------- | -------- |
| 第一层 | `RateLimitFilter`                | 全局 IP      | Bucket4j 令牌桶，100 req/s/IP                       | ✅ 已完成 |
| 第二层 | `@RateLimit` + `RateLimitAspect` | 接口级       | Bucket4j 令牌桶，按 USER/IP/GLOBAL 三种 Key 限流    | ✅ 已完成 |
| 第三层 | Resilience4j                     | 外部服务调用 | CircuitBreaker 熔断 + Retry 重试 + RateLimiter 限流 | ✅ 已完成 |

**改造前后对比：**

|              | 改造前                              | 改造后                                  |
| ------------ | ----------------------------------- | --------------------------------------- |
| 全局防护     | 无，任何 IP 可无限速请求            | 每个 IP 全局限流 100 req/s              |
| 接口级防护   | 无，登录/注册等敏感接口可被暴力破解 | 按 IP/用户限流，如登录 5 次/分钟/IP     |
| 外部服务容错 | AI 引擎调用失败直接抛异常，无重试   | 自动重试 3 次 + 熔断器保护              |
| 推送异常处理 | 推送失败导致报警写入回滚            | 推送失败仅记日志，不影响数据落库        |
| 熔断降级     | 无，外部服务宕机时请求持续超时      | 熔断器打开后快速失败，返回 503          |
| 错误码       | 无限流专用错误码                    | `RATE_LIMIT_EXCEEDED(40301)` → HTTP 429 |

---

## 二、新增依赖

### pom.xml 新增 4 个依赖

```xml
<!-- Resilience4j 容错框架 -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- Bucket4j 令牌桶限流（核心） -->
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- Bucket4j Redis 扩展（分布式限流预留，当前未启用） -->
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- Redisson（分布式限流后端，当前未启用） -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.2</version>
</dependency>
```

---

## 三、限流模块详解

### 3.1 第一层：全局限流 Filter（RateLimitFilter）

**文件**：`com.springboot.ratelimit.RateLimitFilter`

**执行位置**：`@Order(Ordered.HIGHEST_PRECEDENCE + 1)`，在 RequestIdFilter 之后、所有其他 Filter 之前。

**工作流程**：

```
请求进入
  ↓
1. 检查限流开关（app.rate-limit.enabled）
   → 未启用则直接放行
  ↓
2. 提取客户端 IP
   → 优先 X-Forwarded-For（取第一个值）
   → 其次 X-Real-IP
   → 最后 request.getRemoteAddr()
  ↓
3. 构建全局 Key：rate-limit:global:{ip}
  ↓
4. 从 BucketFactory 获取本地令牌桶
   → capacity=100, refillRate=100, refillPeriodSeconds=1
  ↓
5. bucket.tryConsume(1)
   → 成功 → filterChain.doFilter() 放行
   → 失败 → HTTP 429 + BaseResponse(code=40301, message="请求过于频繁，请稍后重试")
```

**关键设计**：
- 使用 `OncePerRequestFilter` 保证每个请求只拦截一次
- 响应体使用 `ObjectMapper` 序列化为统一信封格式 `{ code, data, message, requestId }`
- HTTP 429 状态码使用字面量 `429`（Servlet 5.x 的 `HttpServletResponse` 未提供 `SC_TOO_MANY_REQUESTS` 常量）

### 3.2 第二层：接口级限流（@RateLimit + RateLimitAspect）

#### 3.2.1 @RateLimit 注解

**文件**：`com.springboot.ratelimit.RateLimit`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int capacity() default 10;           // 令牌桶容量
    int refillRate() default 10;         // 每 refillPeriodSeconds 秒补充的令牌数
    int refillPeriodSeconds() default 1; // 补充周期（秒）
    String key() default "";             // 自定义限流 Key（可选）
    String keyType() default "USER";     // Key 类型：USER / IP / GLOBAL
    String fallbackMessage() default "请求过于频繁，请稍后重试"; // 限流提示
}
```

**使用示例**：

```java
@RateLimit(
    capacity = 5,
    refillRate = 5,
    refillPeriodSeconds = 60,
    keyType = "IP",
    fallbackMessage = "登录请求过于频繁，请1分钟后重试"
)
@PostMapping("/login")
public BaseResponse<LoginResultVO> login(...) { ... }
```

#### 3.2.2 RateLimitAspect 切面

**文件**：`com.springboot.ratelimit.RateLimitAspect`

**工作流程**：

```
AOP 拦截 @RateLimit 方法
  ↓
1. 检查限流开关
   → 未启用则直接 joinPoint.proceed()
  ↓
2. buildKey(rateLimit) 构建限流 Key
  ↓
3. bucketFactory.getLocalBucket(key, capacity, refillRate, refillPeriodSeconds)
  ↓
4. bucket.tryConsume(1)
   → 成功 → joinPoint.proceed() 放行
   → 失败 → log.warn() + throw BusinessException(RATE_LIMIT_EXCEEDED, fallbackMessage)
```

**buildKey() 逻辑详解**：

Key 格式：`rate-limit:{baseKey}{suffix}`

| keyType  | baseKey                    | suffix                             | 示例                                 |
| -------- | -------------------------- | ---------------------------------- | ------------------------------------ |
| `USER`   | `key + ":"`（若 key 非空） | 当前登录用户 ID（未登录回退到 IP） | `rate-limit:auth-login:123.45.67.89` |
| `IP`     | `key + ":"`（若 key 非空） | 客户端 IP                          | `rate-limit:auth-login:123.45.67.89` |
| `GLOBAL` | `key + ":"`（若 key 非空） | `"global"`                         | `rate-limit:auth-login:global`       |

**IP 提取策略**（与 Filter 一致）：
1. `X-Forwarded-For` 头 → 取逗号分隔的第一个值
2. `X-Real-IP` 头
3. `request.getRemoteAddr()`

#### 3.2.3 BucketFactory 令牌桶工厂

**文件**：`com.springboot.ratelimit.BucketFactory`

**两种实现**：

| 实现                         | 当前状态 | 说明                         |
| ---------------------------- | -------- | ---------------------------- |
| 本地桶（ConcurrentHashMap）  | ✅ 启用   | 单实例限流，开发环境足够     |
| 分布式桶（Redis + Redisson） | ✗ 未启用 | 多实例共享限流额度，预留接口 |

**本地桶创建逻辑**：

```java
public Bucket getLocalBucket(String key, int capacity, int refillRate, int refillPeriodSeconds) {
    return localBuckets.computeIfAbsent(key, k -> {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillRate, Duration.ofSeconds(refillPeriodSeconds))
                .initialTokens(capacity)   // ★ 初始即满桶，首次请求不被限流
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    });
}
```

**关键选择**：`refillGreedy` vs `refillIntervally`
- `refillGreedy`：令牌在周期内匀速补充，适合突发流量场景
- `refillIntervally`：令牌在周期开始时一次性补充
- 项目选择 `refillGreedy`，因为报警回调可能突发密集，匀速补充更合理

**`initialTokens(capacity)` 的意义**：
- 若使用 `Bandwidth.classic(capacity, Refill.intervally(...))`，初始令牌为 0，首次请求即被限流
- 显式设置 `initialTokens(capacity)` 确保桶初始化时即满，首次请求可正常通过

#### 3.2.4 RateLimitProperties 配置类

**文件**：`com.springboot.ratelimit.RateLimitProperties`

绑定 `app.rate-limit` 配置节：

```yaml
app:
  rate-limit:
    enabled: true                    # 限流总开关
    distributed: true                # 分布式限流开关（预留）
    global-capacity: 100             # 全局限流桶容量
    global-refill-rate: 100          # 全局限流补充速率
    global-refill-period-seconds: 1  # 全局限流补充周期
    endpoints:                       # 各端点限流配置（预留，当前使用注解直接配置）
      auth-login:
        capacity: 5
        refill-rate: 5
        refill-period-seconds: 60
        key-type: IP
```

---

## 四、Resilience4j 容错模块详解

### 4.1 ResilienceConfig — 事件监听

**文件**：`com.springboot.resilience.ResilienceConfig`

监听 Resilience4j 熔断器事件，记录日志：

| 事件                                   | 日志级别 | 内容                      |
| -------------------------------------- | -------- | ------------------------- |
| `CircuitBreakerOnErrorEvent`           | WARN     | 熔断器处理失败：异常消息  |
| `CircuitBreakerOnStateTransitionEvent` | WARN     | 熔断器状态变更：FROM → TO |

实例注册完全由 `application.yml` 中的 `resilience4j.circuitbreaker.instances` 驱动，无需手动注册 Bean。

### 4.2 FallbackHandlers — 降级处理器

**文件**：`com.springboot.resilience.FallbackHandlers`

提供 3 个通用降级方法：

| 方法                                | 返回值 | 行为                                                   |
| ----------------------------------- | ------ | ------------------------------------------------------ |
| `onYoloCallbackFailure(Throwable)`  | void   | 记录日志，不抛异常（供 Resilience4j RateLimiter 降级） |
| `onDeviceControlFailure(Throwable)` | void   | 同上                                                   |
| `onAiEngineQueryFailure(Throwable)` | Void   | 记录日志，返回 null                                    |

**设计原则**：降级方法只记录日志，不做业务补偿。报警类操作通过 RabbitMQ 消息队列保证最终一致性，无需在降级方法中补偿。

### 4.3 AiEngineClient — 外部服务容错注解

**文件**：`com.springboot.service.AiEngineClient`

5 个方法添加 `@Retry` + `@CircuitBreaker` 注解，每个方法对应一个 fallback 方法：

| 原方法                                  | 注解实例                           | fallback 方法                                            | fallback 行为                      |
| --------------------------------------- | ---------------------------------- | -------------------------------------------------------- | ---------------------------------- |
| `startTask(6参数)`                      | yoloCallback                       | `onStartTaskFailure(6参数 + Throwable)`                  | 抛 BusinessException(SYSTEM_ERROR) |
| `stopTask(taskCode)`                    | yoloCallback                       | `onStopTaskFailure(taskCode + Throwable)`                | 同上                               |
| `getTask(taskCode)`                     | yoloCallback                       | `onGetTaskFailure(taskCode + Throwable)`                 | 同上                               |
| `getTask(taskCode, timeoutMs)`          | yoloCallback                       | `onGetTaskFailure(taskCode, timeoutMs + Throwable)`      | 同上                               |
| `healthCheck()`                         | yoloCallback（仅 @CircuitBreaker） | `onHealthCheckFailure(Throwable)`                        | 同上                               |
| `updateTaskConfig(taskCode, threshold)` | yoloCallback                       | `onUpdateConfigFailure(taskCode, threshold + Throwable)` | 同上                               |

**fallback 方法签名规则**：Resilience4j 要求 fallback 方法的参数列表 = 原方法参数 + `Throwable` 最后一个参数，返回类型与原方法一致。重载方法需要分别提供对应签名的 fallback。

**降级返回值**：所有 fallback 均抛出 `BusinessException(SYSTEM_ERROR, "AI引擎服务暂时不可用，请稍后重试")`，由 `GlobalExceptionHandler` 统一处理。

---

## 五、YAML 配置详解

### 5.1 Resilience4j 配置

**CircuitBreaker（熔断器）**：

| 实例名          | failureRateThreshold | slowCallDurationThreshold | waitDurationInOpenState | 用途             |
| --------------- | -------------------- | ------------------------- | ----------------------- | ---------------- |
| `yoloCallback`  | 50%                  | 3s                        | 30s                     | AI 引擎 API 调用 |
| `deviceControl` | 60%                  | 2s                        | 20s                     | 硬件设备控制     |

公共配置（`configs.default`）：
- 滑动窗口：COUNT_BASED，大小 10
- 慢调用比例阈值：50%
- HALF_OPEN 允许调用数：3
- 最小调用数：5
- 自动从 OPEN → HALF_OPEN：开启
- 记录异常：IOException、TimeoutException、ConnectException、ResourceAccessException

**Retry（重试）**：

| 实例名          | maxAttempts | waitDuration | retryExceptions                                 |
| --------------- | ----------- | ------------ | ----------------------------------------------- |
| `yoloCallback`  | 3           | 500ms        | IOException, TimeoutException, ConnectException |
| `deviceControl` | 3           | 300ms        | 同上                                            |

**TimeLimiter（超时限制）**：

| 实例名         | timeoutDuration |
| -------------- | --------------- |
| `yoloCallback` | 5s              |
| `slowQuery`    | 3s              |

**RateLimiter（Resilience4j 限流）**：

| 实例名          | limitForPeriod | limitRefreshPeriod | timeoutDuration |
| --------------- | -------------- | ------------------ | --------------- |
| `yoloCallback`  | 10             | 1s                 | 0（立即拒绝）   |
| `deviceControl` | 2              | 1s                 | 0               |

### 5.2 Redis + Redisson 配置

```yaml
spring:
  data:
    redis:
      database: 1
      host: localhost
      port: 6379
      connect-timeout: 5s
  redisson:
    config: |
      singleServerConfig:
        address: "redis://127.0.0.1:6379"
        database: 1
        connectionPoolSize: 16
        connectionMinimumIdleSize: 4
      nettyThreads: 4
```

当前 Redis 主要用于：
1. **Spring Session**（预留，`spring.session.store-type: redis` 未启用）
2. **分布式限流后端**（预留，Bucket4j-redis 未启用）

### 5.3 限流配置

```yaml
app:
  rate-limit:
    enabled: true
    distributed: true
    global-capacity: 100
    global-refill-rate: 100
    global-refill-period-seconds: 1
    endpoints:
      auth-login:        { capacity: 5,  refill-rate: 5,  refill-period-seconds: 60, key-type: IP }
      auth-admin-login:  { capacity: 3,  refill-rate: 3,  refill-period-seconds: 60, key-type: IP }
      auth-register:     { capacity: 3,  refill-rate: 3,  refill-period-seconds: 60, key-type: IP }
      auth-refresh:      { capacity: 10, refill-rate: 10, refill-period-seconds: 60, key-type: IP }
      auth-captcha:      { capacity: 20, refill-rate: 20, refill-period-seconds: 60, key-type: IP }
      ai-callback:       { capacity: 60, refill-rate: 60, refill-period-seconds: 60, key-type: IP }
      device-control:    { capacity: 2,  refill-rate: 2,  refill-period-seconds: 1,  key-type: USER }
```

注：`endpoints` 配置当前为预留属性，实际限流参数通过 `@RateLimit` 注解直接声明在各 Controller 方法上。

---

## 六、Controller 限流注解详情

### 6.1 AuthController（5 个端点）

| 端点                     | capacity | refillRate | refillPeriodSeconds | keyType | fallbackMessage                         |
| ------------------------ | -------- | ---------- | ------------------- | ------- | --------------------------------------- |
| `POST /auth/login`       | 5        | 5          | 60                  | IP      | "登录请求过于频繁，请1分钟后重试"       |
| `POST /auth/admin/login` | 3        | 3          | 60                  | IP      | "管理员登录请求过于频繁，请1分钟后重试" |
| `POST /auth/register`    | 3        | 3          | 60                  | IP      | "注册请求过于频繁，请1分钟后重试"       |
| `POST /auth/refresh`     | 10       | 10         | 60                  | IP      | "刷新令牌请求过于频繁"                  |
| `GET /auth/captcha`      | 20       | 20         | 60                  | IP      | "验证码请求过于频繁"                    |

**限流策略说明**：
- 登录/注册使用 IP 限流，防止暴力破解
- 管理员登录限制最严（3 次/分钟），因为管理员账户价值更高
- 验证码相对宽松（20 次/分钟），因为每次登录前都需获取验证码

### 6.2 CameraDeviceController（4 个端点）

| 端点                   | capacity | refillRate | refillPeriodSeconds | keyType | fallbackMessage        |
| ---------------------- | -------- | ---------- | ------------------- | ------- | ---------------------- |
| `POST /cameras/add`    | 10       | 10         | 60                  | USER    | "设备操作请求过于频繁" |
| `POST /cameras/delete` | 10       | 10         | 60                  | USER    | "设备操作请求过于频繁" |
| `POST /cameras/update` | 10       | 10         | 60                  | USER    | "设备操作请求过于频繁" |
| `POST /cameras/edit`   | 10       | 10         | 60                  | USER    | "设备操作请求过于频繁" |

**限流策略说明**：
- 设备操作使用 USER 限流（已登录用户 ID），防止异常用户批量操作设备
- 容量 10 次/分钟足够正常运维使用

### 6.3 InternalAiCallbackController（双重限流）

```java
@RateLimiter(name = "yoloCallback")           // Resilience4j：10 req/s
@RateLimit(
    capacity = 60,
    refillRate = 60,
    refillPeriodSeconds = 60,
    keyType = "IP",
    fallbackMessage = "AI回调请求过于频繁"
)
@PostMapping("/events")
```

**双重限流说明**：
- Resilience4j `@RateLimiter`：秒级限流，10 req/s，防止 YOLO 服务突发大量回调
- 自定义 `@RateLimit`：分钟级限流，60 req/min/IP，补充长时间窗口的防护
- 两者互补：Resilience4j 限流针对瞬时突发，自定义限流针对持续高频

---

## 七、AlertRecordConsumer 推送异常隔离

**文件**：`com.springboot.messaging.consumer.AlertRecordConsumer`

### 改造前

```java
boolean appPushed = alertPushService.pushToApp(alertRecord);
boolean pcPushed = alertPushService.pushToPc(alertRecord);
```

推送失败时异常向上传播，如果方法处于 `@Transactional` 上下文中，会导致整个事务回滚——报警记录和监控事件都不会落库。

### 改造后

```java
boolean appPushed = false;
boolean pcPushed = false;
try {
    appPushed = alertPushService.pushToApp(alertRecord);
} catch (Exception e) {
    log.error("APP推送失败, alertUid={}, 不影响报警写入", alertUid, e);
}
try {
    pcPushed = alertPushService.pushToPc(alertRecord);
} catch (Exception e) {
    log.error("PC推送失败, alertUid={}, 不影响报警写入", alertUid, e);
}
```

同样对 `alertWsPublisher.publishAlertCreated()` 也做了 try-catch 隔离：

```java
try {
    alertWsPublisher.publishAlertCreated(...);
} catch (Exception e) {
    log.warn("WebSocket广播失败, alertUid={}, 不影响报警写入", alertUid, e);
}
```

**效果**：推送失败仅记录日志，不影响报警数据落库。推送状态字段 `pushed_to_app` / `pushed_to_pc` 会正确标记为 0（未推送），后续可通过补偿机制重试推送。

---

## 八、GlobalExceptionHandler 扩展

**文件**：`com.springboot.exception.GlobalExceptionHandler`

### 新增异常处理

| 异常类型                    | HTTP 状态码 | 返回体                                                   | 说明       |
| --------------------------- | ----------- | -------------------------------------------------------- | ---------- |
| `BusinessException(40301)`  | 429         | `{ code: 40301, message: "..." }`                        | 限流触发   |
| `CallNotPermittedException` | 503         | `{ code: 50000, message: "服务暂时不可用，请稍后重试" }` | 熔断器打开 |

### resolveHttpStatus 映射逻辑

```
40100     → 401 Unauthorized
40101/40300 → 403 Forbidden
40301     → 429 Too Many Requests    ← 新增
40000-49999 → 400 Bad Request
50000+    → 500 Internal Server Error
```

### ErrorCode 新增

```java
RATE_LIMIT_EXCEEDED(40301, "请求过于频繁")  // 新增
```

---

## 九、RedisConfig 配置类

**文件**：`com.springboot.config.RedisConfig`

提供 `StringRedisTemplate` Bean，统一序列化器为 `StringRedisSerializer.UTF_8`。供 Bucket4j-redis 和后续缓存模块使用。

---

## 十、新增文件清单

### Backend (Java) — 新增 8 个文件

| 文件                       | 包路径                      | 说明                  |
| -------------------------- | --------------------------- | --------------------- |
| `RateLimit.java`           | `com.springboot.ratelimit`  | 限流注解              |
| `RateLimitAspect.java`     | `com.springboot.ratelimit`  | 限流 AOP 切面         |
| `BucketFactory.java`       | `com.springboot.ratelimit`  | 令牌桶工厂            |
| `RateLimitProperties.java` | `com.springboot.ratelimit`  | 限流配置属性          |
| `RateLimitFilter.java`     | `com.springboot.ratelimit`  | 全局限流 Filter       |
| `ResilienceConfig.java`    | `com.springboot.resilience` | Resilience4j 事件监听 |
| `FallbackHandlers.java`    | `com.springboot.resilience` | 降级处理器            |
| `RedisConfig.java`         | `com.springboot.config`     | Redis 配置            |

### Backend — 新增测试文件

| 文件                     | 说明                                    |
| ------------------------ | --------------------------------------- |
| `BucketFactoryTest.java` | Bucket4j 令牌桶单元测试（5 个测试用例） |

### Backend — 修改文件

| 文件                                | 变更说明                                                                                    |
| ----------------------------------- | ------------------------------------------------------------------------------------------- |
| `pom.xml`                           | 新增 resilience4j-spring-boot3、bucket4j-core、bucket4j-redis、redisson-spring-boot-starter |
| `application.yml`                   | 新增 resilience4j、redisson、app.rate-limit 配置节                                          |
| `ErrorCode.java`                    | 新增 `RATE_LIMIT_EXCEEDED(40301)`                                                           |
| `GlobalExceptionHandler.java`       | 新增 `CallNotPermittedException` 处理器 + 40301→429 映射                                    |
| `AiEngineClient.java`               | 5 个方法添加 `@Retry` + `@CircuitBreaker`，新增 6 个 fallback 方法                          |
| `InternalAiCallbackController.java` | 添加 `@RateLimiter` + `@RateLimit` 双重限流                                                 |
| `AuthController.java`               | 5 个端点添加 `@RateLimit` 注解                                                              |
| `CameraDeviceController.java`       | 4 个端点添加 `@RateLimit` 注解                                                              |
| `AlertRecordConsumer.java`          | pushToApp/pushToPc/publishAlertCreated 异常隔离                                             |

---

## 十一、端到端测试验证结果

### 11.1 BucketFactoryTest 单元测试

| 测试用例                                    | 验证内容                                       | 结果   |
| ------------------------------------------- | ---------------------------------------------- | ------ |
| `testLocalBucketAllowsWithinCapacity`       | 容量 5 的桶允许 5 次消费                       | ✅ 通过 |
| `testLocalBucketDeniesOverCapacity`         | 容量 3 的桶第 4 次被拒绝                       | ✅ 通过 |
| `testLocalBucketRefillsOverTime`            | 令牌用尽后等待 1.1s 恢复                       | ✅ 通过 |
| `testDifferentKeysHaveIndependentBuckets`   | 不同 Key 的桶互不影响                          | ✅ 通过 |
| `testDistributedBucketThrowsWhenNotEnabled` | 未启用分布式桶抛 UnsupportedOperationException | ✅ 通过 |

### 11.2 接口限流端到端测试

| 测试项                | 配置              | 结果                                               |
| --------------------- | ----------------- | -------------------------------------------------- |
| `/auth/login` 限流    | capacity=5/IP/60s | 前 5 次返回 200，第 6 次返回 HTTP 429 + code=40301 |
| `/auth/register` 限流 | capacity=3/IP/60s | 前 3 次返回 200，第 4 次返回 HTTP 429              |
| 不同 IP 独立限流      | 每个IP独立桶      | 7 个不同 IP 各自通过，互不影响                     |
| 全局 Filter           | 100 req/s/IP      | 50 次请求全部通过（未超阈值）                      |

### 11.3 Resilience4j 熔断器验证

| 验证项            | 结果                                                                            |
| ----------------- | ------------------------------------------------------------------------------- |
| 熔断器实例加载    | yoloCallback、deviceControl 2 个实例成功注册                                    |
| Actuator 端点     | `/api/actuator/health` 显示熔断器状态                                           |
| YOLO 服务健康检查 | `curl http://127.0.0.1:5000/health` 返回 `{"code":"OK","data":{"status":"ok"}}` |

---

## 十二、开发过程中修复的 Bug

| Bug                                                    | 原因                                                                                                 | 修复                                                              | 影响                               |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- | ---------------------------------- |
| `RateLimitAspect.buildKey()` 自定义 key 时未追加 IP    | key 拼接逻辑遗漏 suffix                                                                              | 重构为 `prefix + baseKey + suffix` 统一拼接                       | 修复前自定义 key 的 IP 限流失效    |
| `BucketFactory` 使用 `Refill.intervally` 初始 tokens=0 | Bucket4j classic 构造器默认初始 tokens=0                                                             | 改用 `Bandwidth.builder().refillGreedy().initialTokens(capacity)` | 修复前首次请求即被限流             |
| 缺少 `RATE_LIMIT_EXCEEDED` 错误码                      | 新增限流功能未同步更新 ErrorCode                                                                     | 新增 `RATE_LIMIT_EXCEEDED(40301, "请求过于频繁")`                 | 修复前限流触发时使用通用错误码     |
| 限流触发时返回 HTTP 400                                | `resolveHttpStatus` 未映射 40301                                                                     | 新增 40301 → 429 映射                                             | 修复前客户端无法区分限流和参数错误 |
| Resilience4j retry 配置冲突                            | `enable-exponential-backoff` 与 `wait-duration` 同时配置导致 "intervalFunction was configured twice" | 移除 `enable-exponential-backoff`，简化为固定间隔重试             | 修复前应用启动失败                 |
| Bucket4j-redis 与 Redisson API 不兼容                  | `RedissonBasedProxyManager.builderFor()` 需要 `CommandAsyncExecutor` 而非 `RedissonClient`           | 暂不启用分布式限流，保留本地桶实现                                | 分布式限流待后续解决               |

---

## 十三、关键设计决策

### 13.1 为什么使用自定义 @RateLimit 而非 Resilience4j RateLimiter

| 对比项     | 自定义 @RateLimit        | Resilience4j @RateLimiter |
| ---------- | ------------------------ | ------------------------- |
| 限流算法   | Bucket4j 令牌桶          | Semaphore 信号量          |
| Key 灵活性 | USER/IP/GLOBAL 三种模式  | 仅按实例名，无 Key 维度   |
| 提示消息   | 可自定义 fallbackMessage | 固定格式                  |
| 配置方式   | 注解直接声明             | YAML + 注解名引用         |
| 令牌恢复   | 按时间匀速恢复           | 按周期释放许可            |

**结论**：接口级限流使用自定义 `@RateLimit`（按 IP/用户粒度），外部服务调用使用 Resilience4j `@RateLimiter`（按服务实例粒度）。两者互补，不冲突。

### 13.2 为什么本地桶而非分布式桶

| 对比项       | 本地桶         | 分布式桶           |
| ------------ | -------------- | ------------------ |
| 依赖         | 无外部依赖     | 需要 Redis         |
| 多实例一致性 | 各实例独立限流 | 全局共享限流额度   |
| 延迟         | 纳秒级         | 毫秒级（网络往返） |
| 部署复杂度   | 零             | 需维护 Redis 集群  |

当前后端为单实例部署，本地桶已足够。后续多实例部署时，需解决 Bucket4j-redis 与 Redisson 的 API 兼容性问题。

### 13.3 为什么 healthCheck 不加重试

健康检查的目的就是判断服务是否可用，重试会掩盖瞬时不可用的状态。熔断器已足够：如果 YOLO 服务持续不可用，熔断器会打开并快速失败。

---

## 十四、三层防护协作流程

以 AI 回调请求为例，完整流转如下：

```
YOLO Service → POST /internal/ai/events
  │
  ├─ 第一层：RateLimitFilter
  │   → rate-limit:global:{ip} → 100 req/s/IP 限流
  │   → 通过 ↓    → 拒绝 → HTTP 429
  │
  ├─ 第二层：@RateLimit AOP
  │   → rate-limit:ai-callback:{ip} → 60 req/min/IP 限流
  │   → 通过 ↓    → 拒绝 → HTTP 429 + code=40301
  │
  ├─ 第二层补充：@RateLimiter (Resilience4j)
  │   → yoloCallback 实例 → 10 req/s 限流
  │   → 通过 ↓    → 拒绝 → 降级 onYoloCallbackFailure()
  │
  ├─ HMAC 签名校验
  │   → 通过 ↓    → 失败 → HTTP 403
  │
  └─ 业务处理（写库 + 推送）
      → 推送失败：仅记日志，不影响落库
```

以 Backend 调用 AI 引擎为例：

```
Backend → AiEngineClient.startTask()
  │
  ├─ @CircuitBreaker("yoloCallback")
  │   → CLOSED: 放行，记录成功/失败
  │   → OPEN: 直接调用 fallback → HTTP 503
  │   → HALF_OPEN: 试探放行，成功→CLOSED，失败→OPEN
  │
  ├─ @Retry("yoloCallback")
  │   → 失败后自动重试，最多 3 次，间隔 500ms
  │   → 仅对 IOException/TimeoutException/ConnectException 重试
  │
  └─ fallback: onStartTaskFailure()
      → log.error + throw BusinessException(SYSTEM_ERROR, "AI引擎服务暂时不可用")
```

---

## 十五、对现有功能链路的影响

### 15.1 前端功能链路

- 前端 API 调用 — **新增限流保护，正常使用不受影响**，仅在极端高频时返回 429
- 前端需处理新增的 HTTP 429 响应（当前未做前端适配，后续阶段处理）

### 15.2 YOLO → Backend 回调链路

- HTTP 回调 — **新增双重限流保护**（Resilience4j 10 req/s + 自定义 60 req/min/IP）
- RabbitMQ 消费 — **推送异常隔离**，不影响数据落库

### 15.3 Backend → YOLO 调用链路

- AI 引擎调用 — **新增熔断 + 重试保护**，失败自动重试，持续失败熔断降级

### 15.4 向后兼容性

- 所有新增限流和容错功能都有开关（`app.rate-limit.enabled`、Resilience4j 实例名）
- 限流阈值设定宽松，正常使用不会触发
- 限流触发时的响应体遵循统一信封格式 `{ code, data, message, requestId }`

---

## 十六、后续待办

| 待办项                                     | 优先级 | 说明                                                              |
| ------------------------------------------ | ------ | ----------------------------------------------------------------- |
| 解决 Bucket4j-redis 与 Redisson API 兼容性 | 中     | 需要适配 `CommandAsyncExecutor` 接口，或切换为 Jedis/Lettuce 后端 |
| 前端适配 HTTP 429 响应                     | 中     | 前端全局拦截 429，展示"请求过于频繁"提示                          |
| 更多接口添加 @RateLimit                    | 低     | 文件上传、数据导出等高资源消耗接口                                |
| 限流指标接入 Prometheus                    | 低     | 限流触发次数、令牌桶利用率等指标                                  |
| 限流白名单机制                             | 低     | 内部服务 IP 跳过限流                                              |
