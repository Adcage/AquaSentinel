# WebRTC 直连排障总结

## 1. 背景与目标

本次排障的目标是解决 AquaSentinel 监控页中 WebRTC 预览黑屏、连接长期卡住的问题，并在不牺牲安全性与可维护性的前提下，形成稳定的长期方案。

涉及链路如下：

```text
浏览器 / Edge
  -> frontend
  -> backend
  -> video-hub-service
  -> ESP32-CAM / 上游视频流
```

排障开始时，前端通过 backend 代理访问 WHIP/WHEP 相关能力，video-hub-service 负责 aiortc 建连与视频分发。

## 2. 初始问题现象

前端监控页的 WebRTC 画面无法显示，主要表现为：

1. 浏览器控制台中 `iceConnectionState` 长期停留在 `checking`。
2. `connectionState` 长期停留在 `connecting`。
3. 无 `track unmuted`，视频元素不播放。
4. aiortc 侧没有进入稳定媒体发送状态。

在早期多轮测试中，还观察到以下特征：

1. 浏览器本地 offer 能正常生成并带有大量候选地址。
2. video-hub-service 能生成 answer，并返回 candidate、ice-ufrag、ice-pwd、fingerprint 等字段。
3. 但浏览器端并不总是把 answer 中的 remote candidate 纳入有效 ICE 检查列表。

## 3. 排查过程中遇到的主要问题

### 3.1 多网卡与候选地址过多

Windows 主机上同时存在多类地址：

1. `192.168.0.x` 主网卡地址
2. `192.168.137.x` 共享网络地址
3. `169.254.x.x` 链路本地地址
4. IPv6 host 地址
5. srflx 公网候选

这导致 aiortc answer 中包含大量 candidate，浏览器端也会生成多组 candidate pair。排障早期很容易误判为“浏览器选错网卡”或“候选优先级错误”。

### 3.2 Edge/Chrome 的 mDNS 匿名化

最小测试页中，浏览器本地 candidate 会显示为：

```text
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.local
```

这会干扰日志理解，但它本身不是最终根因。浏览器在 `setRemoteDescription` 之后会把 remote SDP 规范化，真正影响连接的是 transport、candidate pair 和 DTLS 角色，而不是日志里看到的 `.local` 文本本身。

### 3.3 ESP32-CAM IP 会变化

ESP32-CAM 的地址会随网络变化而变化，因此不能把 `source_url` 或设备 IP 硬编码进前端环境变量或前端代码。这个问题最终通过“前端只上传 `cameraId`，由服务端根据 `cameraId` 解析真实 `stream_url`”解决。

### 3.4 backend 代理让问题变得更隐蔽

因为最初前端是通过 backend 代理访问 WebRTC 信令端点，导致排障时看起来像是：

1. aiortc 可能不兼容浏览器
2. SDP 结构可能有问题
3. DTLS 角色可能反了
4. ICE 角色可能异常

但这些都不是最核心的长期根因。

## 4. 过程中的关键假设与验证结果

### 4.1 假设：aiortc 与浏览器不兼容

我们通过最小 HTML 测试页直接访问 `video-hub-service`，验证结果如下：

1. `setRemoteDescription OK`
2. `ICE: connected`
3. `Connection: connected`
4. `track unmuted: video`
5. `video.play() OK`
6. `dtlsState=connected`
7. `iceState=connected`
8. `nominated=true`

结论：**aiortc 与浏览器可以正常互通，不存在基础兼容性问题。**

### 4.2 假设：DTLS 角色必须强制改成 passive

排障过程中曾尝试把 answer 中的：

```text
a=setup:active
```

强行改成：

```text
a=setup:passive
```

结果是：

1. 在部分情况下 ICE 仍然不通
2. 在直连测试里 even when ICE 成功，也会出现 `dtlsState=failed`

恢复 aiortc 原始 `a=setup:active` 后，直连链路恢复正常，DTLS 正常连接。

结论：**不应手工篡改 aiortc 生成的 DTLS 角色声明。**

### 4.3 假设：必须重写 `m=` 端口、`c=` 行、`ice-ufrag`/`ice-pwd` 顺序

排障中曾做过这些操作：

1. 把 `m=video` 端口改写为保留 candidate 的端口
2. 把 `c=` 地址改写为保留 candidate 的 IP
3. 手工移动 `a=ice-ufrag`、`a=ice-pwd` 的位置

这些修改会让 SDP 看起来更“整齐”，但它们都不应该成为长期方案。最终验证表明：

1. 这些改动不是成功的必要条件
2. 反而会增加 SDP 结构被破坏的风险
3. 真正有效的是更小、更保守的处理

结论：**除非有非常明确的协议级证据，否则不要主动重构 aiortc 生成的 SDP 结构。**

### 4.4 假设：`_reconstruct_sdp_answer` 补 rejected m-section 是根因

我们专门做了 A 组极简实验：

1. 前端只保留单 `m=video`
2. 后端几乎不做 SDP 后处理
3. 仅保留必要日志

结果表明，真正决定成败的不是 rejected audio section 本身，而是“是否经过 backend 代理”。

结论：`_reconstruct_sdp_answer` 不是最核心根因，但这类结构性补丁仍应尽量减少使用范围。

## 5. 根因确认

本次问题最终确认有两个层次：

### 5.1 第一层根因：backend 代理破坏了 WebRTC 信令透传

最关键证据：

1. **前端通过 backend 代理访问时**，浏览器经常没有有效 `remote-candidate`，或者 ICE 长期卡在 `checking`。
2. **最小 HTML 测试页直连 video-hub-service 时**，`remote-candidate` 正常出现，ICE/DTLS 都能成功建立。

也就是说：

```text
frontend -> backend -> video-hub-service   失败
frontend -> video-hub-service             成功
```

因此可以确认：**问题不在 aiortc，也不在浏览器本身，问题在 backend 代理 WebRTC SDP 的方式。**

早期 backend 使用 `RestTemplate` 代理 `application/sdp`，这类代理对 WebRTC 信令并不可靠，容易引入内容或语义层面的破坏。即使肉眼看上去内容接近，也可能影响浏览器内部对 remote candidate、transport 和 BUNDLE 的解析。

### 5.2 第二层根因：架构职责边界不合理

从系统设计角度看，原方案的职责划分并不理想：

1. backend 同时承担“认证管理者”和“WebRTC 信令代理”角色
2. video-hub-service 本来就是媒体分发服务，却没有直接成为浏览器的信令终点
3. 导致 WebRTC 这种对 SDP/ICE/DTLS 细节高度敏感的协议，被额外插入了一层不必要的 HTTP 代理

最终结论是：

**backend 适合统一管理认证，不适合继续代理 WebRTC SDP。**

## 6. 最终解决方案

最终确定的长期方案是：

```text
浏览器
  -> 直连 video-hub-service (WHIP + RTP/UDP)
  -> Authorization: Bearer <token>
  -> 只传 cameraId，不传 source_url
video-hub-service
  -> 调 backend 校验 token
  -> 调 backend 解析 cameraId 对应的真实 source_url
backend
  -> 继续作为统一认证管理者与摄像头配置管理者
```

即：

1. **前端直连 video-hub-service**
2. **backend 不再代理 WHIP SDP**
3. **前端只传 `cameraId`，token 走 `Authorization` header**
4. **video-hub-service 在收到 WHIP 请求时，向 backend 发起 token 校验请求**
5. **video-hub-service 再向 backend 解析 `cameraId -> source_url`**

这样实现后：

1. 浏览器与 video-hub-service 的 WebRTC 信令不再被中间层改写
2. backend 继续统一维护 JWT、权限和安全策略
3. video-hub-service 专注于视频分发与建连
4. 整体架构更清晰，后续维护成本更低

## 7. 最终落地改动

### 7.1 backend

backend 不再承担 WebRTC SDP 代理，而是改成内部认证与摄像头源解析接口：

```text
GET /api/video-hub/auth/verify-preview-token?token=...
GET /api/video-hub/auth/camera-source?cameraId=...&token=...
```

它直接复用：

```text
StreamTokenAuthService.verifyPreviewToken(token)
```

其中：

1. `verify-preview-token` 用于统一校验 JWT
2. `camera-source` 用于在校验通过后返回该 `cameraId` 当前配置的真实 `stream_url`

这样 token 规则与摄像头地址规则都继续统一由 Spring Boot 管理。

### 7.2 video-hub-service

新增认证与视频源解析模块：

1. 优先从 `Authorization: Bearer <token>` 中取 token
2. 调 backend 的 token 校验接口
3. 调 backend 的 `camera-source` 接口获取该 `cameraId` 的真实 `source_url`
4. 校验成功并解析出视频源后，才继续处理 WHIP offer

同时保留：

1. `preferred_ip` 仍可作为候选过滤依据
2. 对旧的 query token 保留兼容读取，但正式方案使用 `Authorization`

### 7.3 frontend

前端改为直连 `video-hub-service` 的 WHIP 地址，但不再暴露内部视频源地址：

1. URL 只保留 `cameraId`
2. token 放到 `Authorization` header

其中：

1. `token` 仍来自当前登录会话
2. `source_url` 不再由前端传递
3. camera 的真实 `stream_url` 由 backend 管理，video-hub-service 按 `cameraId` 查询

这样就解决了 ESP32-CAM IP 变化的问题，也避免了前端伪造 `source_url` 越权访问其他流地址的风险。

## 8. 当前推荐配置

前端开发环境推荐：

```env
VITE_CAMERA_PREVIEW_MODE=webrtc
VITE_WEBRTC_WHEP_BASE_URL=http://localhost:5100
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_WEBRTC_APPEND_TOKEN_QUERY=false
```

注意：

1. **不要**在环境变量里写死 `source_url`
2. **不要**在环境变量里写死 ESP32-CAM IP
3. **不要**再把 token 放进 query 参数
4. `source_url` 应由 backend 依据 `cameraId` 统一管理并返回给 video-hub-service

video-hub-service 推荐配置：

```env
VIDEO_HUB_BACKEND_BASE_URL=http://127.0.0.1:8300
VIDEO_HUB_PREFERRED_IP=<video-hub-service 所在主机的主网卡 IPv4>
```

前端发起 WHIP 请求时，推荐形态应为：

```http
POST http://localhost:5100/video-hub/cameras/5021/whip
Authorization: Bearer <token>
Content-Type: application/sdp
```

而不应再是：

```text
/video-hub/cameras/5021/whip?token=...&source_url=...
```

## 9. 结果验证

最终验证结果：

1. 监控页已能正常显示 WebRTC 画面
2. 最小测试页可稳定建立连接
3. 浏览器端可观察到：

```text
ICE: connected
Connection: connected
track unmuted: video
video.play() OK
dtlsState=connected
iceState=connected
```

4. 统计项中 `bytesReceived` 持续增长，说明媒体流正常传输

## 10. 本次排障中的经验总结

### 10.1 WebRTC 问题一定要区分“信令”和“媒体”

本次最重要的经验之一是：

1. 信令走 HTTP
2. 媒体走 RTP/UDP

backend 代理并不能代理最终媒体流，它只能代理 SDP。对 WebRTC 来说，额外的 SDP 代理层很容易把问题复杂化。

### 10.2 优先做最小可证伪实验

本次真正扭转排障方向的，是最小 HTML 测试页：

```text
浏览器直连 video-hub-service
```

它快速证明：

1. aiortc 没问题
2. Edge 不是根因
3. 真正的问题是 backend 代理层

### 10.3 不要为了“看起来更标准”而随意重写 SDP

在没有充分证据时，手改：

1. `a=setup`
2. `m=` 端口
3. `c=` 行
4. `ice-ufrag` / `ice-pwd` 顺序

都可能引入新的问题。WebRTC 排障时，最可靠的策略通常是：

1. 先尽量保持 aiortc 原始输出
2. 只做最小必要修改
3. 用浏览器 stats 和 webrtc-internals 说话

### 10.4 动态数据不要写进环境变量

ESP32-CAM 的 IP 会变化，所以这类值不能写进：

```text
.env / .env.development
```

正确做法是：

1. 把它作为运行时数据保存在 camera 记录里
2. backend 按 `cameraId` 统一返回对应设备的 `stream_url`
3. video-hub-service 在建连时按 `cameraId` 向 backend 解析真实视频源

### 10.5 token 不应再放在 query 参数里

本次收口过程中还确认了一个安全问题：

```text
?token=...
```

这种方式虽然在排障阶段方便，但不适合作为正式方案，因为它会：

1. 暴露在浏览器地址与开发者工具网络面板中
2. 容易进入访问日志与代理日志
3. 增加被误记录、误转发、误泄露的风险

因此正式方案改为：

```http
Authorization: Bearer <token>
```

同时，video-hub-service 还需要在 CORS 中显式允许：

```text
Access-Control-Allow-Headers: Content-Type, Accept, Authorization
```

否则浏览器会因为预检请求不允许 `Authorization` 头而直接拦截建连。

## 11. 后续建议

### 11.1 短期建议

1. 清理不再需要的 backend SDP 代理逻辑
2. 清理部分仅用于排障的日志和测试页面
3. 继续保留最小测试页作为未来 WebRTC 回归验证工具

### 11.2 中期建议

1. 为 video-hub-service 的 token 校验增加缓存，减少频繁回源 backend
2. 在 video-hub-service 中加入更明确的 token 校验日志
3. 在 frontend 中把 webrtc 建连日志与错误提示进一步产品化

### 11.3 长期建议

1. 将 video-hub-service 作为正式对外的视频分发服务纳入部署架构
2. 在网关或反向代理层明确开放其 WebRTC 所需端口
3. 进一步梳理 STUN/TURN 策略，适配非同网段或更复杂网络环境

## 12. 结论

本次问题的最终定性是：

**原有“frontend -> backend 代理 SDP -> video-hub-service”的 WebRTC 架构不合适，backend 代理破坏了信令链路；改为“frontend 直连 video-hub-service，backend 统一校验 token”后，WebRTC 预览恢复正常。**

这次排障最终不仅修复了功能，也明确了系统中三类角色的边界：

1. **frontend**：负责发起 WebRTC 建连
2. **video-hub-service**：负责媒体分发与 aiortc 会话
3. **backend**：负责统一认证与权限管理

这个边界划分更符合 WebRTC 的技术特点，也更利于后续演进与维护。
