# AquaSentinel 简历展示化补强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AquaSentinel 补强为适合放入简历与作品集的项目，提供统一展示入口、核心链路运行方案和可直接引用的指标证据。

**Architecture:** 先补根 README 作为仓库首页的统一展示层，再补面向核心服务链路的运行说明与最小启动方案，最后整理并验证可复用的指标摘要。所有内容基于现有四子项目结构组织，避免大规模改动业务代码，把工作聚焦在“展示、运行、证据”三层补强上。

**Tech Stack:** Markdown, Docker Compose（若落地）, Spring Boot, Flask, Vue 3, Redis, MySQL, Pytest, Maven, npm

---

## File Structure

- Create: `README.md`
  - 仓库首页总览，承担项目定位、亮点、架构摘要、技术栈、快速启动、指标摘要、文档导航。
- Create: `docs/运行部署/core-stack-quickstart.md`
  - 核心链路的运行说明、依赖、启动顺序、验证步骤、边界说明。
- Create or Modify: `docker-compose.yml`
  - 若当前环境适合，提供核心服务的最小运行栈；若不适合，则在计划执行中明确跳过并在运行文档中说明原因。
- Create: `docs/项目现状/resume-ready-metrics.md`
  - 面向简历/作品集的指标摘要，引用和压缩已有 baseline 指标。
- Create: `scripts/verify_core_stack.ps1`
  - Windows 下的核心服务健康检查与联通验证脚本，避免读者手动逐条敲命令。
- Modify: `docs/项目现状/baseline-metrics.md`
  - 仅在需要时补充本轮新测得的稳定指标或增加来源说明。

## Task 1: 编写根 README 展示入口

**Files:**
- Create: `README.md`
- Reference: `docs/项目现状/architecture.md`
- Reference: `docs/项目现状/tech-stack.md`
- Reference: `docs/项目现状/baseline-metrics.md`
- Reference: `yolo-service/README.md`

- [ ] **Step 1: 写 README 内容草案到新文件**

```md
# AquaSentinel

专业水上安全监控系统，面向泳池/场馆场景，提供多协议视频接入、AI 溺水检测、实时告警推送与多端联动能力。

## 项目亮点

- 多服务架构：Backend、YOLO Service、video-hub、Frontend、Android 分层协作
- 多协议视频链路：支持 HTTP MJPEG、RTSP、HTTP-FLV、WebRTC
- AI 实时推理：YOLO + DeepSort + 溺水规则判定
- 告警联动：检测结果通过 WebSocket / 回调推送到业务系统和客户端
- 工程化能力：Redis 同步、独立 video-hub 微服务、自动化测试、阶段性架构文档

## 系统架构

```text
Frontend / Android
        |
     Backend
        |
  YOLO Service <-> video-hub-service
        |
   Camera / Stream Source
```

详细架构见：`docs/项目现状/architecture.md`

## 技术栈

- Backend: Java 17, Spring Boot, MyBatis-Plus, Redis, MySQL, WebSocket
- Frontend: Vue 3, TypeScript, Vite, Pinia, Element Plus, ECharts
- YOLO Service: Flask, Ultralytics YOLO, DeepSort, PyAV
- Android: Kotlin, Jetpack Compose, Retrofit, OkHttp
- Infra: video-hub 微服务, Redis Pub/Sub, WebRTC

## 核心能力

- 摄像头接入与管理
- 视频流统一接入与转发
- 实时溺水检测与目标跟踪
- 报警记录、处置与统计分析
- PC 后台与 Android 端联动

## 快速开始

1. 阅读 `docs/运行部署/core-stack-quickstart.md`
2. 准备 MySQL、Redis、模型文件与基础环境
3. 启动 Backend、video-hub-service、YOLO Service
4. 使用验证脚本检查核心链路

## 指标摘要

- 总代码规模：5.6 万+ 行
- 服务/客户端：Backend + Frontend + YOLO Service + Android + video-hub
- YOLO Service 测试：52+ 项
- video-hub-service 测试：74+ 项
- 已验证链路：HTTP MJPEG / RTSP 经 video-hub 到 YOLO 推理

更多见：`docs/项目现状/resume-ready-metrics.md`

## 文档导航

- 架构总览：`docs/项目现状/architecture.md`
- 技术栈：`docs/项目现状/tech-stack.md`
- 指标摘要：`docs/项目现状/resume-ready-metrics.md`
- 核心运行：`docs/运行部署/core-stack-quickstart.md`
```

- [ ] **Step 2: 检查 README 是否与现有事实一致**

Run: 手动对照 `docs/项目现状/*.md` 与最新提交记录检查
Expected: README 中不出现仓库中不存在的模块、协议或部署承诺

- [ ] **Step 3: 精简措辞，移除空泛表述**

```md
- 避免“高性能”“企业级”“工业级”这类无证据词汇
- 每条亮点都要能在代码、文档或测试中找到依据
- 指标区只保留当前可验证数字
```

- [ ] **Step 4: 自查 Markdown 结构与导航**

Run: 检查标题层级、代码块闭合、相对路径是否正确
Expected: `README.md` 打开后结构完整、所有文档链接均有效

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: 新增项目首页 README，补齐简历展示入口"
```

## Task 2: 补核心链路运行文档

**Files:**
- Create: `docs/运行部署/core-stack-quickstart.md`
- Reference: `backend/pom.xml`
- Reference: `yolo-service/requirements.txt`
- Reference: `video-hub-service/requirements.txt`
- Reference: `AGENTS.md`

- [ ] **Step 1: 写运行文档骨架**

```md
# AquaSentinel 核心链路快速启动

## 目标

本文档用于启动 AquaSentinel 的核心服务链路，不覆盖 Android、真实硬件控制和全部历史业务模块。

## 覆盖范围

- Backend
- video-hub-service
- YOLO Service
- Redis
- MySQL

## 不覆盖范围

- Android APP 打包/运行
- ESP32 / STM32 真机链路
- 所有摄像头硬件仿真
```

- [ ] **Step 2: 写环境准备与依赖要求**

```md
## 环境要求

- Java 17
- Node.js 20+
- Python 3.10+
- MySQL 8.x
- Redis 7.x

## 关键配置

- Backend: 数据库、Redis、JWT 等配置
- YOLO Service: `.env`、模型文件、`MODEL_VERSION_PATHS_JSON`
- video-hub-service: `VIDEO_HUB_REDIS_URL`
```

- [ ] **Step 3: 写服务启动顺序与命令**

```md
## 启动顺序

1. 启动 Redis 与 MySQL
2. 启动 Backend
3. 启动 video-hub-service
4. 启动 YOLO Service

## 示例命令

### Backend
`mvn spring-boot:run`

### video-hub-service
`python main.py`

### YOLO Service
`python main.py`
```

- [ ] **Step 4: 写最小验证步骤**

```md
## 验证步骤

1. 访问 Backend 健康接口或 Swagger 页面
2. 访问 YOLO `/health`
3. 访问 video-hub 相关状态接口
4. 若存在可用摄像头源，验证 snapshot 或状态接口
5. 用脚本执行核心联通性检查
```

- [ ] **Step 5: 写已知限制与边界说明**

```md
## 已知限制

- 无模型文件或 GPU 环境时，部分推理能力不可完整复现
- 无真实摄像头源时，仅能验证服务联通与基础 API
- Android 与硬件链路不在本文档覆盖范围内
```

- [ ] **Step 6: Commit**

```bash
git add docs/运行部署/core-stack-quickstart.md
git commit -m "docs: 新增核心链路快速启动文档"
```

## Task 3: 增加核心链路验证脚本

**Files:**
- Create: `scripts/verify_core_stack.ps1`
- Reference: `docs/运行部署/core-stack-quickstart.md`

- [ ] **Step 1: 编写 PowerShell 验证脚本**

```powershell
$ErrorActionPreference = 'Stop'

$checks = @(
  @{ Name = 'Backend'; Url = 'http://127.0.0.1:8300/api/health' },
  @{ Name = 'YOLO'; Url = 'http://127.0.0.1:5000/health' },
  @{ Name = 'video-hub'; Url = 'http://127.0.0.1:5100/health' }
)

foreach ($check in $checks) {
  try {
    $response = Invoke-WebRequest -Uri $check.Url -UseBasicParsing -TimeoutSec 5
    Write-Host "[OK] $($check.Name): $($response.StatusCode)"
  }
  catch {
    Write-Host "[FAIL] $($check.Name): $($_.Exception.Message)"
  }
}
```

- [ ] **Step 2: 根据实际接口修正 URL**

Run: 对照 Backend、YOLO、video-hub 实际健康接口路径
Expected: 脚本里的 URL 均对应真实存在接口，不能凭空假设

- [ ] **Step 3: 将脚本使用方式写回运行文档**

```md
### 一键验证

PowerShell:
`pwsh ./scripts/verify_core_stack.ps1`
```

- [ ] **Step 4: 本地运行脚本并记录输出样式**

Run: `pwsh ./scripts/verify_core_stack.ps1`
Expected: 输出清晰标识每个服务的成功/失败状态

- [ ] **Step 5: Commit**

```bash
git add scripts/verify_core_stack.ps1 docs/运行部署/core-stack-quickstart.md
git commit -m "chore: 新增核心服务健康检查脚本"
```

## Task 4: 评估并补最小 Docker Compose 方案

**Files:**
- Create or Modify: `docker-compose.yml`
- Reference: `backend/Dockerfile`（若不存在则先确认）
- Reference: `yolo-service/Dockerfile`（若不存在则先确认）
- Reference: `video-hub-service/Dockerfile`（若不存在则先确认）
- Reference: `docs/运行部署/core-stack-quickstart.md`

- [ ] **Step 1: 检查是否已有 Dockerfile 与可复用镜像边界**

Run: 搜索 `backend/`, `yolo-service/`, `video-hub-service/` 下的 `Dockerfile`
Expected: 明确哪些服务可以直接 compose，哪些服务暂时只能手动运行

- [ ] **Step 2: 如果 Docker 条件成熟，写最小 compose 文件**

```yaml
version: '3.9'
services:
  mysql:
    image: mysql:8.0
  redis:
    image: redis:7
  backend:
    build: ./backend
  video-hub:
    build: ./video-hub-service
  yolo-service:
    build: ./yolo-service
```

- [ ] **Step 3: 如果 Docker 条件不成熟，明确降级策略**

```md
若缺少 Dockerfile、模型文件挂载策略不清、服务启动参数不稳定，则不创建 compose，
改为在 `docs/运行部署/core-stack-quickstart.md` 中明确“当前采用手动启动方案”的原因与后续补全点。
```

- [ ] **Step 4: 只保留核心依赖，不把 Android/Frontend 硬塞进最小栈**

Run: 复查 compose 或运行文档范围
Expected: 运行方案聚焦 Backend + Redis + MySQL + video-hub + YOLO，不扩大范围

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml docs/运行部署/core-stack-quickstart.md
git commit -m "build: 补充核心服务最小运行栈说明"
```

## Task 5: 整理简历可引用指标摘要

**Files:**
- Create: `docs/项目现状/resume-ready-metrics.md`
- Modify: `docs/项目现状/baseline-metrics.md`
- Reference: `docs/项目现状/baseline-metrics.md`
- Reference: 最新测试结果与提交记录

- [ ] **Step 1: 写指标摘要文档骨架**

```md
# AquaSentinel 简历可引用指标摘要

## 已实测指标

## 条件性指标

## 指标采集前提

## 推荐在简历中的表达方式
```

- [ ] **Step 2: 填入当前已稳定可用的指标**

```md
- 总代码规模：56,159 行
- 测试文件数：67
- YOLO Service 测试：52/52（排除集成测试时）
- video-hub-service 测试：74/74
- Frontend 构建产物：约 3.1 MB
- 已验证协议链路：HTTP MJPEG、RTSP 经 video-hub 进入 YOLO 推理
```

- [ ] **Step 3: 明确条件性指标而非硬写结果**

```md
- GPU 推理延迟：需在有模型文件和目标硬件环境下测量
- 多路并发上限：需在真实流源与显存条件下测量
- Android 端帧率与冷启动：需在真机环境下测量
```

- [ ] **Step 4: 增加“简历写法示例”小节**

```md
示例：
负责设计并实现 AquaSentinel 水上安全监控系统，构建 Backend / YOLO / video-hub / Frontend / Android 五端协作架构，
支持 RTSP、HTTP-FLV、MJPEG、WebRTC 等多协议视频链路，完成 5.6 万+ 行代码规模项目的核心功能开发与端到端验证。
```

- [ ] **Step 5: Commit**

```bash
git add docs/项目现状/resume-ready-metrics.md docs/项目现状/baseline-metrics.md
git commit -m "docs: 新增简历可引用指标摘要"
```

## Task 6: 回填 README 中的指标与运行入口

**Files:**
- Modify: `README.md`
- Reference: `docs/运行部署/core-stack-quickstart.md`
- Reference: `docs/项目现状/resume-ready-metrics.md`

- [ ] **Step 1: 将运行入口链接补回 README**

```md
## 快速开始

详细步骤见：`docs/运行部署/core-stack-quickstart.md`
```

- [ ] **Step 2: 将最终确认过的指标摘要补回 README**

```md
## 指标摘要

- 代码规模：56,159 行
- video-hub-service 测试：74/74
- YOLO Service 测试：52/52（核心测试）
- 已验证视频链路：HTTP MJPEG / RTSP -> video-hub -> YOLO
```

- [ ] **Step 3: 检查 README 与指标文档是否一致**

Run: 逐项对照 `README.md` 和 `docs/项目现状/resume-ready-metrics.md`
Expected: README 中的每个指标都能在指标文档找到来源

- [ ] **Step 4: Commit**

```bash
git add README.md docs/运行部署/core-stack-quickstart.md docs/项目现状/resume-ready-metrics.md
git commit -m "docs: 回填首页运行入口与指标摘要"
```

## Task 7: 最终验证与整理

**Files:**
- Modify: `README.md`
- Modify: `docs/运行部署/core-stack-quickstart.md`
- Modify: `docs/项目现状/resume-ready-metrics.md`

- [ ] **Step 1: 验证文档链接与路径**

Run: 手动检查 README、运行文档、指标文档中的相对路径
Expected: 所有路径均存在，无死链

- [ ] **Step 2: 验证命令可执行性**

Run: 抽样执行运行文档中的核心命令和验证脚本
Expected: 命令格式正确，失败时有明确前置条件说明

- [ ] **Step 3: 审核首页可读性**

```md
检查项：
- 首屏是否能一眼看出项目做什么
- 是否写清你的技术贡献点
- 是否给出可运行入口
- 是否有可引用指标
```

- [ ] **Step 4: 统一文档措辞与边界说明**

```md
把“完整可运行”“核心链路可运行”“依赖硬件环境”这些表述统一，
避免 README 和运行文档在承诺范围上互相矛盾。
```

- [ ] **Step 5: Commit**

```bash
git add README.md docs/运行部署/core-stack-quickstart.md docs/项目现状/resume-ready-metrics.md scripts/verify_core_stack.ps1 docker-compose.yml
git commit -m "docs: 完成项目简历展示化补强收尾验证"
```

## Self-Review

- 设计文档中的三部分目标已经全部覆盖：展示层、运行层、证据层。
- 计划中没有使用 TBD/TODO 之类占位语，所有任务都指向明确文件和产出。
- `docker-compose.yml` 被设计成“可选交付”，因为当前是否具备 Docker 化前提还需要事实验证；计划已明确降级策略，避免先入为主承诺。
- 所有关键输出文件之间的关系已串联：`README.md` 引用运行文档与指标文档，运行文档引用验证脚本，指标文档反哺 README 摘要。
