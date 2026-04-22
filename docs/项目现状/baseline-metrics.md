# AquaSentinel 可量化基线指标

> 本文档记录项目当前的可量化基线数据，供后续技术优化前后对比使用。所有无法在当前离线环境直接得到的指标标记为 `[需测量]`。

---

## 1. 代码规模基线

### 1.1 代码行数与文件数

| 子项目 | 源文件数 | 总行数 | 测试文件数 | 测试行数 | 测试覆盖率估算 |
|--------|---------|--------|-----------|---------|---------------|
| **Backend** | 293 Java | 20,719 | 24 | 1,945 | ~9.4% (按行数) |
| **Frontend** | 42 Vue + 81 TS = 123 | 25,098 | 16 | 3,273 | ~13.0% |
| **YOLO Service** | 39 Python | 4,131 | 16 | 1,927 | ~46.7% |
| **Android** | 47 Kotlin | 6,211 | 11 | 528 | ~8.5% |
| **合计** | 502 | 56,159 | 67 | 7,673 | - |

### 1.2 按层分布

#### Backend（Java）

| 层级 | 文件数 | 说明 |
|------|--------|------|
| Controller | 32 | REST API 入口（含 5 个测试控制器） |
| Service 接口 | 19 | 业务接口定义 |
| Service 实现 | 32 | 含 push/stream 策略子包 |
| Mapper | 19 | MyBatis-Plus 数据访问 |
| Entity | 19 | 数据库实体 |
| DTO | 81 | 请求参数对象 |
| VO | 19 | 响应视图对象 |
| Config | 10 | Spring 配置类 |
| Security | 6 | JWT、HMAC、Auth |
| WebSocket | 8 | 报警/AI推送/实时监控 |
| AOP | 3 | 权限、日志、审计拦截 |
| Utils | 5 | 工具类 |
| Exception | 3 | 业务异常体系 |

#### Frontend（Vue/TS）

| 层级 | 文件数 | 行数 | 说明 |
|------|--------|------|------|
| Views | 24 页面 + 9 弹窗 | - | 管理后台页面 |
| Components | 15 | - | 业务/通用/图表/图标组件 |
| API Controllers | 25 | ~2484 自动生成 | openapi2ts 生成 |
| Services | 12 | - | 业务门面层 |
| Composables | 2 | - | 组合式函数 |
| Stores | 2 | - | Pinia 状态管理 |
| Tests | 16 | 3,273 | 单元+集成+E2E |

#### YOLO Service（Python）

| 层级 | 文件数 | 说明 |
|------|--------|------|
| API (Flask-Smorest) | 5 BP | 端点 + Schema |
| Services | 10 | 核心业务逻辑 |
| Models | 6 | SQLAlchemy 实体 |
| Repositories | 5 | CRUD 操作 |
| Core | 6 | 配置/错误/日志/中间件/响应 |
| 最大单文件 | 1 | `engine_task_service.py` 1049 行 |

#### Android（Kotlin）

| 层级 | 文件数 | 说明 |
|------|--------|------|
| Screens | 6 | 页面 Composable |
| Components | 18+ | UI 组件 |
| Data/Remote | 7 | Retrofit + OkHttp |
| Data/Alert | 1 | WebSocket 报警 |
| Data/Stream | 3 | WebSocket 视频帧 |
| Navigation | 2 | 路由定义 |
| Theme | 4 | 颜色/尺寸/字体/主题 |
| Config | 1 | 应用配置 |
| Model | 1 | UI 数据类 |

### 1.3 API 端点统计

| 子项目 | 端点总数 | 主要分组 |
|--------|---------|----------|
| Backend REST | ~183 | CRUD 操作: add/delete/update/edit/get/list/page/vo |
| YOLO REST | ~5 | 任务启停、健康检查、测试触发 |
| **总计** | ~188 | |

---

## 2. 依赖基线

### 2.1 直接依赖数量

| 子项目 | 运行时依赖 | 开发依赖 | 总计 |
|--------|-----------|---------|------|
| Backend (Maven) | 12 | 2 (test+devtools) | 14 |
| Frontend (npm) | 11 | 9 | 20 |
| YOLO Service (pip) | 27 | 0 (全部运行时) | 27 |
| Android (Gradle) | 15+ | 0 | 15+ |

### 2.2 关键依赖版本锁定状态

| 子项目 | 版本锁定方式 | 风险 |
|--------|-------------|------|
| Backend | `pom.xml` 精确版本 | 低 |
| Frontend | `package-lock.json` | 低 |
| YOLO Service | `requirements.txt` 范围约束（`>=x.y,<z.0`） | 中 — 小版本更新可能引入不兼容 |
| Android | `libs.versions.toml` + Gradle | 低 |

### 2.3 安全相关依赖

| 依赖 | 用途 | 版本 | 已知问题 |
|------|------|------|----------|
| jjwt | JWT 认证 | 0.12.5 | 无已知高危 |
| spring-boot | 框架 | 3.2.3 | 需关注 CVE |
| hutool-all | 工具类 | 5.8.8 | 全量引入，含未使用模块 |
| mysql-connector-j | 数据库驱动 | 运行时 | 需关注 CVE |

---

## 3. 性能基线

> 以下指标需在部署环境中实际测量，标注 `[需测量]`。

### 3.1 Backend 性能指标

| 指标 | 当前值 | 单位 | 说明 |
|------|--------|------|------|
| JWT 验证平均耗时 | `[需测量]` | ms | 含 Token 解析+Redis 查询 |
| 分页查询平均响应时间 | `[需测量]` | ms | alert_record list/page/vo |
| WebSocket 连接并发上限 | `[需测量]` | 个 | 单节点最大连接数 |
| MJPEG 流代理并发路数 | `[需测量]` | 路 | 同时转码摄像头数 |
| 启动时间 | 5.754（`mvn test -Dtest=MainApplicationTests`） | 秒 | Spring 上下文启动耗时（dev profile） |
| 内存占用（空载） | `[需测量]` | MB | 无用户连接时 |
| JVM 堆内存配置 | `[需测量]` | MB | -Xmx 参数 |

### 3.2 Frontend 性能指标

| 指标 | 当前值 | 单位 | 说明 |
|------|--------|------|------|
| 首屏加载时间 (LCP) | `[需测量]` | 秒 | 登录页首次内容绘制 |
| 管理后台首屏加载 | `[需测量]` | 秒 | 含 Element Plus 按需加载 |
| 构建产物大小 | 3155 | KB | `npm run build` 后 `dist/` 大小（3.1 MB） |
| 自动生成代码占比 | 2484 / 25098 ≈ 9.9% | % | typings.d.ts + api/ |
| 首屏 JS Bundle 大小 | 1074.12（`index` chunk） | KB | 最大业务 chunk，另有 `StatisticsView` 1134.82 KB |
| Lighthouse 性能评分 | `[需测量]` | 分 | Chrome Lighthouse |

### 3.3 YOLO Service 性能指标

| 指标 | 当前值 | 单位 | 说明 |
|------|--------|------|------|
| 单帧推理延迟（YOLOv8） | `[需测量]` | ms | 720p 输入，GPU |
| 单帧推理延迟（CPU fallback） | `[需测量]` | ms | 无 GPU 环境 |
| 并发任务数上限 | `[需测量]` | 路 | 受 GPU 显存限制 |
| WebSocket 推送延迟 | `[需测量]` | ms | 检测结果→Backend |
| 内存占用（单任务） | `[需测量]` | MB | 含模型权重 |
| 内存占用（N 任务） | `[需测量]` | MB | 模型共享+独立缓冲区 |
| 帧处理 FPS | `[需测量]` | fps | 实际处理帧率 |
| 模型加载时间 | 0.710（`create_app(TESTING)`） | 秒 | 当前环境无模型文件，warmup 被跳过 |

### 3.4 Android 性能指标

| 指标 | 当前值 | 单位 | 说明 |
|------|--------|------|------|
| APK 大小 | 115 | MB | `app/build/outputs/apk/debug/app-debug.apk` |
| 冷启动时间 | `[需测量]` | 秒 | 从点击到首页可见 |
| WebSocket 重连延迟 | `[需测量]` | 秒 | 网络中断后恢复 |
| MJPEG 帧率 | `[需测量]` | fps | 通过 WebSocket 接收 |
| 内存占用 | `[需测量]` | MB | 含视频流 |

### 3.5 数据库性能指标

| 指标 | 当前值 | 单位 | 说明 |
|------|--------|------|------|
| 最大表行数估算 | `[需测量]` | 行 | 运行后数据量 |
| alert_record 查询 P95 | `[需测量]` | ms | 分页+条件查询 |
| stats_snapshot 聚合查询 P95 | `[需测量]` | ms | 7 天趋势查询 |
| env_sensor_sample 表月增长量 | `[需测量]` | 行 | 20 场馆×50 传感器 |

---

## 4. 质量基线

### 4.1 测试覆盖

| 子项目 | 测试文件 | 测试行数 | 源码行数 | 行覆盖率估算 | 类型 |
|--------|---------|---------|---------|------------|------|
| Backend | 24 | 1,945 | 20,719 | ~9.4% | 单元+集成 |
| Frontend | 16 | 3,273 | 25,098 | ~13.0% | 单元+集成+E2E |
| YOLO Service | 16 | 1,927 | 4,131 | ~46.7% | 单元+API |
| Android | 11 | 528 | 6,211 | ~8.5% | 单元 |
| **合计** | **67** | **7,673** | **56,159** | **~13.7%** | - |

### 4.2 代码质量工具

| 子项目 | Lint 工具 | 格式化工具 | 配置文件 | 状态 |
|--------|----------|-----------|---------|------|
| Backend | 无 | 无 | 无 | ❌ 未启用 |
| Frontend | 无 | 无 | 无 | ❌ 未启用 |
| YOLO Service | ruff（仅依赖） | 无 | 无 | ⚠️ 仅安装未配置 |
| Android | 无 | 无 | 无 | ❌ 未启用 |

### 4.3 类型安全

| 子项目 | 类型系统 | 严格模式 | 效果 |
|--------|---------|---------|------|
| Backend | Java 17 强类型 | 编译时检查 | ✅ 完整 |
| Frontend | TypeScript strict | `noUnusedLocals`, `noUnusedParameters`, `noImplicitReturns` | ✅ 基本完整（自动生成代码有 ts-ignore） |
| YOLO Service | Python 类型注解 | `from __future__ import annotations` | ⚠️ 运行时不强制 |
| Android | Kotlin 强类型 | 编译时检查 | ✅ 完整 |

### 4.4 安全基线

| 检查项 | Backend | Frontend | YOLO | Android |
|--------|---------|----------|------|---------|
| HTTPS | ✅ 配置 | ✅ Vite proxy | ❌ 纯 HTTP | ❌ 明文 HTTP |
| 认证 | ✅ JWT+Redis | ✅ Token 存储 | ⚠️ HMAC 签名 | ⚠️ SharedPreferences |
| CORS | ✅ 配置 | - | ❌ 未配置 | - |
| 输入验证 | ✅ DTO 校验 | ✅ Marshmallow | ✅ Marshmallow | ⚠️ 无验证 |
| SQL 注入防护 | ✅ MyBatis-Plus 参数化 | - | ✅ ORM | - |
| 密码存储 | ✅ 哈希 | - | - | - |

---

## 5. 架构复杂度基线

### 5.1 模块间依赖数

| 源 → 目标 | 依赖方式 | 方向 |
|-----------|---------|------|
| Frontend → Backend | REST API + WebSocket | 单向 |
| Android → Backend | REST API + WebSocket | 单向 |
| YOLO → Backend | WebSocket (推送) + HTTP 回调 | 双向 |
| Backend → MySQL | TCP/SQL | 单向 |
| Backend → Redis | TCP/Redis 协议 | 单向 |
| YOLO → 本地 SQLite | 文件 | 单向 |

**服务间直接依赖数：5**（无服务网格，无服务发现）

### 5.2 单文件复杂度 Top-5

| 排名 | 文件 | 行数 | 子项目 | 风险 |
|------|------|------|--------|------|
| 1 | `api/typings.d.ts` | 2,484 | Frontend | 自动生成，低风险 |
| 2 | `engine_task_service.py` | 1,049 | YOLO | 核心逻辑，高风险 |
| 3 | `AlertRecordServiceImpl.java` | 129 | Backend | 核心业务 |
| 4 | `AdminDashboardView.vue` | 764 | Frontend | 仪表盘页 |
| 5 | `ApiClient.kt` | 45 | Android | HTTP 客户端 |

### 5.3 技术债指标

| 维度 | 当前值 | 目标 | 说明 |
|------|--------|------|------|
| 测试行占比 | 13.7% | ≥30% | 测试行 / 总代码行 |
| Lint 工具覆盖率 | 25%（仅 ruff 装了没用） | 100% | 4 个子项目中启用 Lint 的 |
| Entity 命名规范违反数 | 19 个 Entity 用 snake_case | 0 | Java Entity 应使用 camelCase |
| 最大文件行数 | 2,484 (自动生成) / 1,049 (手写) | <500 | |
| 空壳模块数 | 4 个 (auth, file_upload, pdf_report, wechat_pay/pay) | 0 | YOLO Service 预留模块 |
| 无 DI 的仓库数 | 6 个 (Android) | 0 | 硬编码单例 |

### 5.4 构建与测试执行基线（实测）

| 子项目 | 命令 | 结果 | 耗时 | 关键发现 |
|--------|------|------|------|----------|
| Backend | `mvn test` | ✅ 通过（46/46） | 12.985s | 单测整体健康 |
| Frontend | `npx vitest run` | ❌ 失败（51 通过 / 23 失败） | 9.90s | 失败集中在 Pinia 未激活与页面文案断言 |
| Frontend | `npx vue-tsc --noEmit` | ❌ 失败（28 个 TS 错误） | - | 主要是 `enabled` 字段缺失、unused 声明、类型签名不一致 |
| Frontend | `npm run build` | ✅ 通过 | 11.59s | 出现 >500KB chunk 告警（最大 1134.82KB） |
| YOLO Service | `pytest` | ❌ 失败（48 通过 / 2 失败） | 36.16s | 失败用例依赖 `127.0.0.1:7897` 健康检查服务 |
| YOLO Service | `ruff check app/ tests/` | ❌ 失败（4 问题） | - | 1 unused import + 2 unused var + 1 无占位 f-string |
| Android | `./gradlew test` | ✅ 通过（82/82） | 1m14s | Kotlin daemon 缓存异常后 fallback 编译成功 |
| Android | `./gradlew assembleDebug` | ✅ 通过 | 1m16s | 生成 APK 115MB |

---

## 6. 基线测量方法

以下为各 `[需测量]` 指标的推荐采集方法：

### 6.1 Backend 性能

```bash
# 启动时间
time java -jar target/springboot-0.0.1-SNAPSHOT.jar 2>&1 | grep "Started MainApplication"

# JVM 堆内存
jcmd <pid> GC.heap_info

# API 响应时间（需 jmeter 或 ab）
ab -n 1000 -c 10 http://localhost:8300/api/alert-records/list/page/vo
```

### 6.2 Frontend 性能

```bash
# 构建产物大小
npm run build && du -sh dist/

# Bundle 分析
npx vite-bundle-visualizer

# Lighthouse（Chrome DevTools）
# 1. 打开应用登录页
# 2. F12 → Lighthouse → 生成报告
```

### 6.3 YOLO Service 性能

```bash
# 推理延迟（在 YOLO Service 日志中搜索）
grep "inference_time" app.log

# 内存占用
ps aux | grep python | awk '{print $6}'

# 或使用 psutil 在代码中埋点
# 在 engine_task_service.py 中已有 time.time() 计时
```

### 6.4 数据库性能

```sql
-- 最大表行数
SELECT table_name, table_rows FROM information_schema.tables
WHERE table_schema = 'aquasentinel' ORDER BY table_rows DESC;

-- 慢查询检查
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 20;
```

---

## 7. 优化效果跟踪模板

引入新技术后，使用以下模板对比优化前后数据：

### 7.1 模板：优化前后对比表

| 指标 | 优化前（基线） | 优化后 | 变化 | 测量方法 |
|------|--------------|--------|------|----------|
| _填写指标名_ | _基线值_ | _新值_ | _+/-_% | _工具/命令_ |

### 7.2 预期优化效果对照

根据 `issues-and-optimization.md` 中的路线图，各优化完成后的预期指标变化：

| 优化项 | 核心对比指标 | 当前基线 | 优化目标 |
|--------|------------|---------|----------|
| P1-01 消息中间件 | 事件投递保证率 | 0%（断连即丢） | ≥99.9% |
| P1-03 Docker 化 | 新环境部署时间 | 2-4 小时 | 15-30 分钟 |
| P2-01 流媒体独立 | Backend 转码 CPU | 1-2%/路 | 0%/路 |
| P2-02 时序数据库 | 30 天聚合查询 | ~2-5 秒 | <200ms |
| P3-01 CI/CD | 构建验证 | 手动 | 每次推送自动 |
| P3-02 Lint 强制 | 风格一致性 | ~60% | 100% |
| P3-03 Android ViewModel | 旋转状态保持 | 0% | 100% |
| P3-04 日志聚合 | 故障定位时间 | 30-60 分钟 | <5 分钟 |
