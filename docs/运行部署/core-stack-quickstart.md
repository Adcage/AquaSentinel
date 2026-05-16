# AquaSentinel 核心链路快速启动

本文档描述如何在本地启动 AquaSentinel 核心链路，使系统的基础功能可运行和验证。

## 1 目标与覆盖范围

覆盖的核心服务：

| 服务 | 用途 | 端口 |
|------|------|------|
| MySQL 8.x | 业务数据库 | 3306 |
| Redis 7.x | 会话缓存、Redisson 分布式锁、video-hub 流同步 | 6379 |
| Backend (Spring Boot) | 业务后端 API、WebSocket、定时任务 | 8300 |
| video-hub-service (Flask) | 摄像头流管理、WebRTC 信令、帧代理 | 5100 |
| YOLO Service (Flask) | AI 推理引擎（溺水检测、目标跟踪） | 5000 |

不在本文范围内：

- Android 客户端
- 硬件设备（摄像头、边缘网关）
- Frontend 前端服务（可独立启动，不影响核心链路）
- RabbitMQ（可选，未配置时消息队列功能不可用但不影响启动）
- AI 智能助手功能（需要配置大模型 API Key）

## 2 环境要求

| 依赖 | 最低版本 | 版本锁定来源 |
|------|---------|-------------|
| Java JDK | 17 | `backend/pom.xml` 中 `<java.version>17</java.version>` |
| Maven | 3.8+ | 后端构建工具 |
| Python | 3.10+ | `yolo-service/requirements.txt` 使用 `from __future__ import annotations` |
| Node.js | 20.19+ 或 22.12+ | `frontend/package.json`（仅用于 openapi 代码生成，非启动必需） |
| MySQL | 8.x | `application.yml` 中 `com.mysql.cj.jdbc.Driver` |
| Redis | 7.x | `spring-boot-starter-data-redis` + Bucket4j Redis + Redisson |

操作系统：Windows / macOS / Linux 均可，启动命令以 Windows PowerShell 为主，附带 Linux/macOS 等效命令。

### 2.1 Python 虚拟环境

yolo-service 和 video-hub-service 分别独立管理依赖，建议各自创建虚拟环境：

```powershell
# yolo-service
cd yolo-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt

# video-hub-service
cd ..\video-hub-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### 2.2 Java 构建依赖

后端编译需要 Maven，首次构建会下载依赖：

```powershell
cd backend
mvn compile
```

## 3 关键配置

### 3.1 MySQL

默认连接信息（`backend/src/main/resources/application.yml`）：

| 参数 | 默认值 |
|------|-------|
| URL | `jdbc:mysql://localhost:3306/aqua_sentinel` |
| 用户名 | `root` |
| 密码 | `123456` |

初始化数据库：

```powershell
# 连接 MySQL 后执行建表脚本
mysql -u root -p123456 < backend/sql/create_table.sql
mysql -u root -p123456 < backend/sql/seed_data.sql
```

如需修改连接信息，可编辑 `application.yml` 中的 `spring.datasource` 段，或通过环境变量覆盖：

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/aqua_sentinel"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

### 3.2 Redis

默认连接信息（`application.yml`）：

| 参数 | 默认值 |
|------|-------|
| 地址 | `redis://:123456@127.0.0.1:6379/1` |
| 密码 | `123456` |
| 数据库编号 | 1 |

video-hub-service 使用相同的 Redis 实例（`VIDEO_HUB_REDIS_URL` 默认值与后端一致）。

如需修改密码，需同步更新以下三个位置：

1. Backend `application.yml` → `spring.data.redis.password` 和 `spring.redisson.config`
2. video-hub-service 环境变量 `VIDEO_HUB_REDIS_URL`

### 3.3 Backend 配置文件入口

| 文件 | 用途 |
|------|------|
| `backend/src/main/resources/application.yml` | 主配置（含公共默认值） |
| `backend/src/main/resources/application-local.yml` | 本地私密配置（已加入 .gitignore，含 AI API Key 等） |

后端默认激活 `local` Profile（`spring.profiles.active: local`）。

敏感配置通过环境变量注入：

| 环境变量 | 用途 | 默认值 |
|---------|------|-------|
| `AI_API_KEY` | AI 推理 API Key | 空（不影响核心链路启动） |
| `AI_BASE_URL` | AI 推理 API 地址 | `https://api.openai.com` |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `guest` |

### 3.4 YOLO Service 配置文件入口

| 文件 | 用途 |
|------|------|
| `yolo-service/.env.example` | 配置模板（复制为 `.env` 使用） |
| `yolo-service/.env` | 实际配置（已加入 .gitignore） |

关键配置项：

| 环境变量 | 用途 | 默认值 |
|---------|------|-------|
| `APP_ENV` | 运行环境 | `dev` |
| `JAVA_BACKEND_BASE_URL` | Backend 地址 | `http://127.0.0.1:8300` |
| `VIDEO_HUB_BASE_URL` | video-hub 地址 | `http://127.0.0.1:5100` |
| `DATABASE_URL` | 数据库连接 | `sqlite:///instance/app.db`（开发可用 SQLite） |
| `MODEL_PATH` | YOLO 模型文件路径 | `model/best.pt` |
| `MODEL_CONF_THRESHOLD` | 推理置信度阈值 | `0.25` |

### 3.5 video-hub-service 配置文件入口

video-hub-service 通过 `app/core/config.py` 读取环境变量，无独立 `.env` 文件。

关键配置项：

| 环境变量 | 用途 | 默认值 |
|---------|------|-------|
| `VIDEO_HUB_PORT` | 服务监听端口 | `5100` |
| `VIDEO_HUB_BACKEND_BASE_URL` | Backend 地址 | `http://127.0.0.1:8300` |
| `VIDEO_HUB_REDIS_URL` | Redis 连接 | `redis://:123456@127.0.0.1:6379/1` |
| `VIDEO_HUB_PREFERRED_IP` | WebRTC 候选 IP | 空（自动检测） |

## 4 启动顺序与命令

必须按以下顺序启动，因为存在服务间依赖。

### 4.1 第一步：MySQL

确保 MySQL 服务正在运行，且 `aqua_sentinel` 数据库已初始化：

```powershell
# 检查 MySQL 是否运行
mysqladmin -u root -p123456 ping

# 创建数据库（如尚未创建）
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS aqua_sentinel DEFAULT CHARACTER SET utf8mb4;"

# 初始化表结构
mysql -u root -p123456 aqua_sentinel < backend/sql/create_table.sql
mysql -u root -p123456 aqua_sentinel < backend/sql/seed_data.sql
```

### 4.2 第二步：Redis

确保 Redis 服务正在运行并监听 6379 端口：

```powershell
# 检查 Redis 是否运行
redis-cli -a 123456 ping
# 预期输出: PONG

# 如果 Redis 未运行（Windows 可使用 WSL 或 Redis for Windows）
redis-server
```

如果 Redis 密码不是 `123456`，需要同步修改 Backend 和 video-hub-service 的 Redis 密码配置。

### 4.3 第三步：Backend

```powershell
cd backend

# 首次需要编译
mvn compile

# 启动（默认激活 local Profile）
mvn spring-boot:run

# 或指定 Profile 启动
# mvn spring-boot:run -Dspring-boot.run.profiles=local
```

启动成功标志：日志中出现 `Started SpringbootApplication` 且无错误，服务监听 `0.0.0.0:8300`。

### 4.4 第四步：video-hub-service

```powershell
cd video-hub-service

# 激活虚拟环境（如已创建）
.\.venv\Scripts\Activate.ps1

# 启动（开发模式，支持热重载）
python main.py --dev

# 生产模式启动
# python main.py
```

启动成功标志：日志中出现 `Running on http://0.0.0.0:5100`。

### 4.5 第五步：YOLO Service

```powershell
cd yolo-service

# 从模板创建配置文件（首次）
Copy-Item .env.example .env

# 激活虚拟环境（如已创建）
.\.venv\Scripts\Activate.ps1

# 开发模式启动（支持热重载）
python main.py --dev

# 生产模式启动
# python main.py
```

启动成功标志：日志中出现 `Running on http://0.0.0.0:5000`。

## 5 最小验证步骤

所有服务启动后，按以下步骤验证核心链路联通性。

### 5.1 基础健康检查

```powershell
# Backend 健康检查（Actuator）
Invoke-RestMethod http://localhost:8300/api/actuator/health
# 预期：status 为 "UP"，各组件（db、redis）状态正常

# Backend API 文档（Knife4j）
# 浏览器打开 http://localhost:8300/api/doc.html

# video-hub-service 健康检查
Invoke-RestMethod http://localhost:5100/api/health
# 预期：返回服务状态信息

# YOLO Service 健康检查
Invoke-RestMethod http://localhost:5000/api/health
# 预期：返回服务状态信息
```

### 5.2 服务间联通性验证

```powershell
# Backend -> MySQL 联通：检查 Actuator health 的 db 组件
(Invoke-RestMethod http://localhost:8300/api/actuator/health).components.db.status
# 预期: "UP"

# Backend -> Redis 联通：检查 Actuator health 的 redis 组件
(Invoke-RestMethod http://localhost:8300/api/actuator/health).components.redis.status
# 预期: "UP"
```

### 5.3 自动化验证脚本

可使用项目根目录下的自动化验证脚本一键检查所有服务状态：

```powershell
# 后续将在 Task 3 中创建
.\scripts\verify_core_stack.ps1
```

该脚本将依次检查 MySQL、Redis、Backend、video-hub、YOLO Service 的可用性，并输出各服务状态报告。

## 6 已知限制与边界说明

| 限制 | 说明 |
|------|------|
| 无 YOLO 模型文件 | YOLO Service 启动不需要模型文件，但推理请求将返回错误。模型文件（`model/best.pt` 或 `model/drowning_ext.pt`）需单独获取并放置到 `yolo-service/model/` 目录 |
| 无 GPU | YOLO 推理在 CPU 上可运行但速度缓慢，不满足实时检测要求。生产环境建议使用 NVIDIA GPU + CUDA |
| 无真实摄像头源 | video-hub-service 可正常启动，但无流可代理。仅能验证服务本身联通，无法验证推流链路 |
| 无 RabbitMQ | 消息队列相关功能（报警通知、分析消费）不可用，但不会阻止服务启动。如需完整功能，需额外安装 RabbitMQ |
| 无 AI API Key | AI 智能助手功能（聊天、报警分析）不可用。核心链路的报警检测和流管理不受影响 |
| SQLite 模式 | YOLO Service 默认使用 SQLite（`DATABASE_URL=sqlite:///instance/app.db`），可用于开发验证。生产环境建议切换 MySQL |
| JWT Secret | 默认 JWT Secret 为 `change-this-jwt-secret-change-this-jwt-secret`，仅限本地开发。生产环境必须更换 |
| Redis 密码 | 默认密码为 `123456`，仅限本地开发。生产环境必须更换 |
| 数据库迁移 | `backend/sql/` 目录下有增量迁移脚本（如 `migration_*.sql`），需按文件名顺序执行 |

## 7 部署方式说明

当前采用手动启动方案，原因如下：

- 各子项目尚未提供 `Dockerfile`，直接编写 `docker-compose.yml` 缺乏基础
- YOLO Service 依赖模型文件（体积大、需单独获取），不适合无条件打包进镜像
- GPU/CUDA 依赖在容器中配置复杂，与"最小可运行"目标冲突
- 核心链路的手动启动步骤已在本文档中完整覆盖

后续可按需补全 `Dockerfile` 和 `docker-compose.yml`，优先级为：Backend > video-hub-service > YOLO Service。

## 8 快速参考：端口与地址汇总

| 服务 | 地址 | 说明 |
|------|------|------|
| MySQL | `localhost:3306` | 数据库 `aqua_sentinel` |
| Redis | `localhost:6379` | 数据库编号 1，密码 `123456` |
| Backend API | `http://localhost:8300/api` | 上下文路径 `/api` |
| Backend Actuator | `http://localhost:8300/api/actuator` | 健康检查与指标 |
| Backend API 文档 | `http://localhost:8300/api/doc.html` | Knife4j 接口文档 |
| video-hub-service | `http://localhost:5100` | 流管理服务 |
| YOLO Service | `http://localhost:5000` | AI 推理服务 |