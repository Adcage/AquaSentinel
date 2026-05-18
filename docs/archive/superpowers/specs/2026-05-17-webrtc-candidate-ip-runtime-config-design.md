# WebRTC 候选 IP 运行时配置设计

## 背景

当前 WebRTC ICE 候选替换 IP 通过 Vite 环境变量 `VITE_WEBRTC_CANDIDATE_IPS` 在构建时写死到前端产物中。每次部署到不同机器或主机 IP 变更时，必须修改环境变量并重新构建前端镜像，违背了 Docker 镜像可复用的核心原则。

同时 `docker-compose.yml` 中 `video-hub` 服务的 `VIDEO_HUB_PREFERRED_IP` 也硬编码了 `192.168.0.221`，两者本质是同一个值——宿主机局域网 IP。

## 方案

采用 **Docker Compose `.env` + 后端 API** 方案：

1. 项目根目录 `.env` 定义 `HOST_IP`，docker-compose 通过变量注入到 backend 和 video-hub
2. 后端新增公开接口 `/api/system/webrtc-config` 返回 `preferredIp`
3. 前端运行时从 API 获取候选 IP，替代构建时环境变量
4. 开发环境保留 `VITE_WEBRTC_CANDIDATE_IPS` 作为降级后备

## 详细设计

### 1. 项目根目录 `.env`

docker-compose 自动读取项目根目录的 `.env` 文件。新增：

```env
# 宿主机局域网 IP（WebRTC ICE 候选用，部署时按实际 IP 修改）
HOST_IP=192.168.0.221
```

同时提供 `.env.example` 作为模板，`.gitignore` 中已有 `.env` 不会被提交。

### 2. docker-compose.yml

替换硬编码为变量引用：

```yaml
backend:
  environment:
    # ... 已有配置 ...
    APP_VIDEO_HUB_PREFERRED_IP: ${HOST_IP:-}

video-hub:
  environment:
    # ... 已有配置 ...
    VIDEO_HUB_PREFERRED_IP: ${HOST_IP:-}
```

`${HOST_IP:-}` 中 `:-` 表示未定义时默认为空字符串，不会导致 compose 报错。

### 3. 后端改动

`AppVideoHubProperties.preferredIp` 已存在（绑定 `app.video-hub.preferred-ip`），Spring Boot 自动映射环境变量 `APP_VIDEO_HUB_PREFERRED_IP`。

新增公开接口（无需鉴权）：

```
GET /api/system/webrtc-config
Response: { "code": 0, "data": { "preferredIp": "192.168.0.221" }, "message": "ok" }
```

- 在现有 Controller 中新增方法，读取 `AppVideoHubProperties.preferredIp` 返回
- 放行路径：`/api/system/webrtc-config` 加入白名单（已有 `/api/video-hub/**` 等先例）

### 4. 前端改动

#### 新增 `src/services/webrtcConfigService.ts`

- 调用 `/api/system/webrtc-config` 获取 `preferredIp`
- 结果缓存为单例 Promise，避免重复请求
- API 失败时静默降级，回退到 `VITE_WEBRTC_CANDIDATE_IPS`，最终降级为空数组

#### 修改 `src/utils/streamPreview.ts`

- `resolveWebrtcCandidateIps()` 保持同步签名，从内存缓存读取
- 新增 `fetchWebrtcCandidateIps()` async 函数，先请求 API，失败则读 Vite 环境变量
- `webrtcConfigService` 初始化时调用 `fetchWebrtcCandidateIps()` 写入缓存

#### 修改 `WebRtcWhepPlayer.vue`

- 在 `startConnection` 中先调用 `fetchWebrtcCandidateIps()` 确保缓存就绪
- 后续 `resolveWebrtcCandidateIps()` 从缓存同步读取

#### 修改环境变量文件

- `.env.production`：移除 `VITE_WEBRTC_CANDIDATE_IPS`（生产环境走 API）
- `.env.development`：保留 `VITE_WEBRTC_CANDIDATE_IPS` 作为开发降级
- `.env.example`：更新注释说明两种配置方式

### 5. 数据流

```
部署时: .env (HOST_IP) → docker-compose → backend (APP_VIDEO_HUB_PREFERRED_IP) + video-hub (VIDEO_HUB_PREFERRED_IP)
运行时: 浏览器 → GET /api/system/webrtc-config → { preferredIp }
         → replaceMdnsCandidates(sdp, [preferredIp])
降级:   API 不可用 → VITE_WEBRTC_CANDIDATE_IPS (仅开发环境) → 空数组（不替换）
```

### 6. 网络链路验证

实现完成后需验证以下链路：

| 链路 | 验证方式 |
|------|----------|
| `.env` → docker-compose → backend 环境变量 | backend 日志或 actuator 确认 `preferredIp` 值 |
| `.env` → docker-compose → video-hub 环境变量 | video-hub 日志确认 `VIDEO_HUB_PREFERRED_IP` |
| 前端 → backend `/api/system/webrtc-config` | 浏览器 Network 面板确认响应 |
| 前端获取 preferredIp → replaceMdnsCandidates | WebRTC debug 日志确认 SDP 中 .local 被替换 |
| video-hub WHIP 端 preferred_ip 生效 | video-hub 日志确认 `preferred_ip` 使用 |

### 7. 受影响文件

| 文件 | 改动 |
|------|------|
| `.env.example`（新建） | 模板文件，含 `HOST_IP` 说明 |
| `docker-compose.yml` | 两处硬编码 IP → `${HOST_IP:-}` |
| `backend/.../controller/` | 新增 webrtc-config 接口方法 |
| `backend/.../security/` | 放行 `/api/system/webrtc-config` |
| `frontend/src/services/webrtcConfigService.ts`（新建） | API 调用 + 缓存 + 降级逻辑 |
| `frontend/src/utils/streamPreview.ts` | 新增 fetch 函数 + 缓存读取 |
| `frontend/src/components/business/WebRtcWhepPlayer.vue` | 初始化时获取候选 IP |
| `frontend/.env.production` | 移除 `VITE_WEBRTC_CANDIDATE_IPS` |
| `frontend/.env.development` | 保留，加注释说明降级 |
| `frontend/.env.example` | 更新注释 |
