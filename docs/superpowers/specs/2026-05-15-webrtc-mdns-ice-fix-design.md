# WebRTC mDNS ICE 候选替换修复设计

## 1. 问题根因

WebRTC 建立连接时，浏览器和 video-hub-service 需要互相交换网络地址（ICE 候选）来配对通信路径。当浏览器从非 localhost 地址访问页面时，会启用 **mDNS 隐私保护**，将候选地址中的真实 IP 替换为匿名域名（如 `acad7439-1bcf-4505-b454-adeb98d0768c.local`）。

aiortc（Python WebRTC 库）不支持解析 `.local` 域名，导致：

```
浏览器 offer:  "我在 *.local 这个地址"      ← mDNS 匿名化
aiortc answer: "我在 192.168.0.181 这个地址"  ← 真实 IP

配对结果：双方地址无法匹配 → ICE 卡在 checking → 黑屏
```

即使从 localhost 访问（offer 包含真实 IP），ICE 仍卡在 checking，原因可能是本机多网卡路由问题。

## 2. 修复方案

**核心思路**：在浏览器发出 SDP offer 之前，将其中的 `.local` 匿名地址替换为可配置的真实 IP，确保 offer 和 answer 的 ICE 候选都是对方可达的真实 IP。

### 2.1 前端：ICE 候选替换

**配置项**（`.env` / `.env.development` / `.env.production`）：

```env
# WebRTC ICE 候选替换 IP 列表（逗号分隔）
# 填写 video-hub-service 所在机器的局域网 IP
# 浏览器会将 offer 中的 .local mDNS 地址替换为此列表中的 IP
VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
```

**替换逻辑**（`WebRtcWhepPlayer.vue`）：

1. `setLocalDescription` 完成后、发送 offer 之前
2. 扫描 SDP 中所有 `a=candidate:` 行
3. 对于地址为 `*.local` 的候选行，将其替换为 `VITE_WEBRTC_CANDIDATE_IPS` 中的 IP
4. 替换规则：
   - 候选行的地址字段（第 5 个分片）从 `acad7439-xxxx.local` 替换为 `192.168.0.181`
   - 保留候选行的其他字段（component、transport、priority、port 等）不变
5. 如果候选行地址已经是可路由 IP（非 `.local`、非 `127.0.0.1`、非 `169.254.x.x`），则不替换
6. 如果 `VITE_WEBRTC_CANDIDATE_IPS` 未配置或为空，则不替换（降级为原始行为）

**`onicecandidate` 回调补充**：

当 `event.candidate` 的候选地址为 `.local` 时，也做同样替换后再添加到 offer SDP 中。

### 2.2 服务端：answer 候选可路由 IP 回退

**当前行为**：`_pin_answer_candidates` 从 offer 中提取首选 IPv4 作为 answer 候选的绑定地址。但如果 offer 中所有候选都是 `.local`，`_extract_preferred_offer_ipv4` 返回 `None`，answer 候选无法绑定到可路由 IP。

**修复**：

1. `AppVideoHubProperties` 新增 `preferredIp` 配置项（默认为空）
2. `application.yml` 新增 `app.video-hub.preferred-ip` 配置
3. `WebrtcSessionManager.create_whip_session` 接受 `preferred_ip` 参数
4. WHEP 端点从 backend 代理传入的 `source_url` 或配置中获取 `preferred_ip`，传入 `create_whip_session`
5. `_pin_answer_candidates` 当 `preferred_address` 为 `None` 时，改用 `preferred_ip` 配置值作为回退

### 2.3 服务端配置

`application.yml` 新增：

```yaml
app:
  video-hub:
    base-url: http://127.0.0.1:5100
    timeout-ms: 5000
    preferred-ip: 192.168.0.181   # video-hub-service 所在机器的局域网 IP
```

`video-hub-service` 的配置（环境变量或 `app/config.py`）新增：

```python
VIDEO_HUB_PREFERRED_IP = "192.168.0.181"  # 同一台机器的局域网 IP
```

WHIP 端点从请求头或环境变量中读取此 IP，传入 `create_whip_session`。

### 2.4 前端配置

```env
# .env.development
VITE_CAMERA_PREVIEW_MODE=webrtc
VITE_WEBRTC_WHEP_BASE_URL=/api
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_WEBRTC_APPEND_TOKEN_QUERY=true
VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
```

```env
# .env.production
VITE_CAMERA_PREVIEW_MODE=webrtc
VITE_WEBRTC_WHEP_BASE_URL=/api
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_WEBRTC_APPEND_TOKEN_QUERY=true
VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
```

## 3. 改动文件清单

### 前端

| 文件 | 改什么 |
|------|--------|
| `frontend/.env` | 新增 `VITE_WEBRTC_CANDIDATE_IPS` 配置项 |
| `frontend/.env.development` | `VITE_CAMERA_PREVIEW_MODE` 改回 `webrtc`，新增 `VITE_WEBRTC_CANDIDATE_IPS` |
| `frontend/.env.production` | 同上 |
| `frontend/.env.example` | 新增 `VITE_WEBRTC_CANDIDATE_IPS` 示例 |
| `frontend/src/components/business/WebRtcWhepPlayer.vue` | 新增 ICE 候选替换逻辑：扫描 SDP 中的 `.local` 地址替换为配置 IP |

### 服务端（video-hub-service）

| 文件 | 改什么 |
|------|--------|
| `video-hub-service/app/video_hub/webrtc_session.py` | `create_whip_session` 接受 `preferred_ip` 参数；当 offer 无真实 IPv4 候选时用其回退 |
| `video-hub-service/app/api/video_hub_webrtc.py` | WHIP 端点从环境变量/Flask config 读取 `preferred_ip`，传入 `create_whip_session` |
| `video-hub-service/app/__init__.py` | Flask config 新增 `VIDEO_HUB_PREFERRED_IP` |

### 服务端（backend）

| 文件 | 改什么 |
|------|--------|
| `backend/src/main/java/com/springboot/config/AppVideoHubProperties.java` | 新增 `preferredIp` 字段 |
| `backend/src/main/resources/application.yml` | 新增 `app.video-hub.preferred-ip` |
| `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java` | WHIP 反代时将 `preferredIp` 通过 query param 或 header 传递给 video-hub-service |

## 4. ICE 候选替换的具体实现

### 前端 SDP 替换算法

```typescript
// 伪代码，实际实现在 WebRtcWhepPlayer.vue 中
function replaceMdnsCandidates(sdp: string, candidateIps: string[]): string {
  if (!candidateIps.length) return sdp;
  
  const lines = sdp.split("\r\n");
  let ipIndex = 0;
  
  return lines.map(line => {
    if (!line.startsWith("a=candidate:")) return line;
    
    const parts = line.split(" ");
    const address = parts[4];
    
    // 检测 mDNS 地址（*.local）
    if (address.endsWith(".local")) {
      // 从 candidateIps 列表中轮流取 IP（多候选时分布到不同 IP）
      const replacement = candidateIps[ipIndex % candidateIps.length];
      ipIndex++;
      parts[4] = replacement;
      return parts.join(" ");
    }
    
    return line;
  }).join("\r\n");
}
```

### 前端 `onicecandidate` 替换

```typescript
// 在 icecandidate 事件回调中：
pc.addEventListener("icecandidate", (event) => {
  if (!event.candidate) return;
  const candidate = event.candidate.candidate;
  // 如果包含 .local 地址，创建替换后的候选
  // 此处不需要手动添加到 SDP，因为 setLocalDescription 后
  // 我们会统一扫描整个 SDP 做替换
});
```

实际实现中，选择在 `setLocalDescription` 后、`fetch` 发送前统一替换整个 SDP，而不是逐个候选替换。这样更简单可靠。

### 服务端 answer 回退逻辑

```python
# webrtc_session.py 中
async def create_whip_session(self, camera_id, sdp_offer, session, preferred_ip=None):
    # ... 现有逻辑 ...
    
    # _pin_answer_candidates 中：
    # 如果 preferred_address 为 None（offer 无真实 IPv4），
    # 使用 preferred_ip 参数作为回退
```

## 5. 不改动的部分

- MJPEG 后端代理链路（已验证可用，保持不变作为降级）
- video-hub-service 的拉流/熔断/状态机（保持不变）
- 后端 JWT 白名单（已包含 `/api/video-hub/**`，保持不变）
- yolo-service 的推理取帧链路（保持不变）
- 前端 `VITE_CAMERA_PREVIEW_MODE` 可在 `webrtc` 和 `backend_proxy` 之间切换（降级路径保留）

## 6. 验证标准

1. 从 `192.168.0.181:5173` 访问监控页，WebRTC 模式下画面正常出图
2. 浏览器 DevTools console 中无 WebRTC 连接错误（`iceConnectionState` 应达到 `connected`/`completed`）
3. SDP offer 中不再包含 `.local` 地址（替换为 `192.168.0.181`）
4. SDP answer 中 ICE 候选为可路由的真实 IP
5. `VITE_CAMERA_PREVIEW_MODE=backend_proxy` 仍可作为降级方案正常工作
6. `VITE_WEBRTC_CANDIDATE_IPS` 为空时，不替换任何候选（降级行为）