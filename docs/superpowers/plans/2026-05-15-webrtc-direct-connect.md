# WebRTC 直连架构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 前端直连 video-hub-service 的 WHIP 端点，由 backend 统一管理 token 校验，彻底解决 SDP 代理破坏问题。

**Architecture:** 浏览器直连 video-hub-service:5100 发起 WHIP 信令（带 token + source_url），video-hub-service 收到后调用 backend:8300 内部 API 校验 token，校验通过后才处理 SDP。视频流（RTP/UDP）直接在浏览器和 video-hub-service 之间传输，不经 backend。Backend 保持认证管理者的角色。

**Tech Stack:** Python/Flask（video-hub-service）、Java/Spring Boot（backend）、Vue 3/TypeScript（frontend）

---

## 文件结构

| 文件 | 职责 | 操作 |
|------|------|------|
| `backend/.../controller/VideoHubProxyController.java` | 旧的 WHIP 代理，改为内部 token 校验接口 | 修改 |
| `backend/.../config/AppVideoHubProperties.java` | 保持不变 | — |
| `video-hub-service/app/core/config.py` | 新增 BACKEND_BASE_URL 配置 | 修改 |
| `video-hub-service/app/security/__init__.py` | 新模块 | 创建 |
| `video-hub-service/app/security/token_verifier.py` | 调 backend 校验 token | 创建 |
| `video-hub-service/app/api/video_hub_webrtc.py` | WHIP 端点加 token 校验 | 修改 |
| `video-hub-service/app/__init__.py` | CORS 已有，无需改 | — |
| `frontend/.env.development` | WHIP BASE_URL 改为直连 video-hub-service | 修改 |
| `frontend/src/utils/streamPreview.ts` | webrtc 模式下拼 source_url | 修改 |
| `video-hub-service/tests/test_token_verifier.py` | token 校验测试 | 创建 |

---

### Task 1: Backend 新增内部 token 校验接口

**Files:**
- Modify: `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java`

**说明:** 将原来的 WHIP 代理端点替换为内部 token 校验接口。Video-hub-service 调用此接口校验前端传来的 token 是否合法。接口路径 `GET /api/video-hub/auth/verify-preview-token`，参数 `token`，成功返回 200 + `{code: 0, data: true}`，失败返回 401。

- [ ] **Step 1: 重写 VideoHubProxyController**

将原来的 WHIP 代理代码替换为 token 校验接口。保留 `DELETE /sessions/{sessionId}` 端点（后续可废弃，暂保留）。

```java
package com.springboot.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.config.AppVideoHubProperties;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.CameraDevice;
import com.springboot.security.StreamTokenAuthService;
import com.springboot.service.CameraDeviceService;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video-hub")
public class VideoHubProxyController {

    @Resource private AppVideoHubProperties videoHubProperties;

    @Resource private StreamTokenAuthService streamTokenAuthService;

    @Resource private CameraDeviceService cameraDeviceService;

    @GetMapping("/auth/verify-preview-token")
    public BaseResponse<Boolean> verifyPreviewToken(
            @RequestParam String token) {
        streamTokenAuthService.verifyPreviewToken(token);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Boolean> deleteWhipSession(@PathVariable String sessionId) {
        String targetUrl =
                videoHubProperties.getBaseUrl() + "/video-hub/sessions/" + sessionId;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .DELETE()
                            .timeout(Duration.ofSeconds(10))
                            .build();
            client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.OPERATION_ERROR, "删除 WHEP 会话失败: " + e.getMessage());
        }
        return ResultUtils.success(true);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd backend && mvn spotless:apply && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/springboot/controller/VideoHubProxyController.java
git commit -m "feat: 将 WHIP 代理改为内部 token 校验接口"
```

---

### Task 2: Video-hub-service 新增 token 校验模块

**Files:**
- Create: `video-hub-service/app/security/__init__.py`
- Create: `video-hub-service/app/security/token_verifier.py`
- Modify: `video-hub-service/app/core/config.py`

**说明:** Video-hub-service 通过调用 backend 的 `GET /api/video-hub/auth/verify-preview-token?token=xxx` 校验 token。校验失败抛 401 BusinessError。

- [ ] **Step 1: 创建 security 包**

创建 `video-hub-service/app/security/__init__.py`，内容为空。

- [ ] **Step 2: 新增 BACKEND_BASE_URL 配置**

在 `video-hub-service/app/core/config.py` 的 `BaseConfig` 类中新增：

```python
VIDEO_HUB_BACKEND_BASE_URL = os.environ.get(
    "VIDEO_HUB_BACKEND_BASE_URL", "http://127.0.0.1:8300"
)
```

- [ ] **Step 3: 创建 token_verifier.py**

```python
from __future__ import annotations

import logging

import requests
from flask import current_app

from app.common.errors import BusinessError

logger = logging.getLogger(__name__)

_verify_session = requests.Session()


def verify_preview_token(token: str) -> None:
    if not token:
        raise BusinessError(
            "缺少视频流访问令牌",
            status_code=401,
            code="TOKEN_MISSING",
        )
    backend_url = current_app.config.get("VIDEO_HUB_BACKEND_BASE_URL", "")
    if not backend_url:
        logger.warning("未配置 VIDEO_HUB_BACKEND_BASE_URL，跳过 token 校验")
        return
    url = f"{backend_url}/api/video-hub/auth/verify-preview-token"
    try:
        resp = _verify_session.get(url, params={"token": token}, timeout=5)
    except requests.RequestException as exc:
        logger.error("Token 校验请求失败: %s", exc)
        raise BusinessError(
            "Token 校验服务不可用",
            status_code=503,
            code="TOKEN_VERIFY_ERROR",
        )
    if resp.status_code != 200:
        raise BusinessError(
            "视频流访问令牌无效或已过期",
            status_code=401,
            code="TOKEN_INVALID",
        )
```

- [ ] **Step 4: 提交**

```bash
git add video-hub-service/app/security/ video-hub-service/app/core/config.py
git commit -m "feat: video-hub-service 新增 token 校验模块"
```

---

### Task 3: WHIP 端点集成 token 校验

**Files:**
- Modify: `video-hub-service/app/api/video_hub_webrtc.py`

**说明:** WHIP 端点从 query 参数取 token，调用 `verify_preview_token` 校验。只在 `VIDEO_HUB_BACKEND_BASE_URL` 有值时校验，否则跳过（向后兼容）。

- [ ] **Step 1: 在 whip_offer 函数开头加 token 校验**

在 `whip_offer` 函数的 `original_sdp = request.get_data(as_text=True)` 之前插入：

```python
    from app.security.token_verifier import verify_preview_token

    token = str(request.args.get("token") or "").strip()
    verify_preview_token(token)
```

- [ ] **Step 2: 运行现有测试确认不破坏**

Run: `cd video-hub-service && python -m pytest tests/test_video_hub_webrtc.py -v -k "pin_answer or force_setup or stream_track" --timeout=30`
Expected: 全部 PASS

- [ ] **Step 3: 提交**

```bash
git add video-hub-service/app/api/video_hub_webrtc.py
git commit -m "feat: WHIP 端点集成 token 校验"
```

---

### Task 4: 前端直连 video-hub-service

**Files:**
- Modify: `frontend/.env.development`
- Modify: `frontend/src/utils/streamPreview.ts`

**说明:** 修改环境配置让 WHIP 请求直连 video-hub-service。修改 `resolveWebrtcUrl` 和 `resolveCameraPreviewTarget`，让 webrtc 模式下把 `source_url` 和 `token` 拼到 WHIP URL 中。

- [ ] **Step 1: 修改 .env.development**

将 `VITE_WEBRTC_WHEP_BASE_URL` 改为 video-hub-service 地址：

```
VITE_WEBRTC_WHEP_BASE_URL=http://localhost:5100
VITE_WEBRTC_WHEP_PATH_TEMPLATE=video-hub/cameras/{cameraId}/whip
VITE_WEBRTC_APPEND_TOKEN_QUERY=true
```

- [ ] **Step 2: 修改 streamPreview.ts 的 resolveWebrtcUrl**

当前 `resolveWebrtcUrl` 不接收 `streamUrl` 和 `token` 参数。修改函数签名，让它把 `source_url` 和 `token` 拼到 URL 上：

```typescript
const resolveWebrtcUrl = (
  streamUrl?: string,
  cameraCode?: string,
  cameraId?: number,
  token?: string,
): string => {
  const normalized = (streamUrl || "").trim();
  if (/^https?:\/\/.*\/whep(\?|$)/i.test(normalized)) {
    return normalized;
  }
  const baseUrl = resolveWhepByTemplate(cameraCode, cameraId);
  if (!baseUrl) {
    return "";
  }
  const params: string[] = [];
  if (streamUrl) {
    params.push(`source_url=${encodeURIComponent(streamUrl)}`);
  }
  if (token) {
    const shouldAppendToken =
      String(import.meta.env.VITE_WEBRTC_APPEND_TOKEN_QUERY || "false").toLowerCase() ===
      "true";
    if (shouldAppendToken) {
      params.push("token=" + encodeURIComponent(token));
    }
  }
  if (params.length === 0) {
    return baseUrl;
  }
  return baseUrl + (baseUrl.includes("?") ? "&" : "?") + params.join("&");
};
```

- [ ] **Step 3: 修改 resolveCameraPreviewTarget 传递 token 和 streamUrl**

在 webrtc 分支中，把 token 传给 resolveWebrtcUrl：

```typescript
  if (previewMode === "webrtc") {
    const whepUrl = resolveWebrtcUrl(
      options.streamUrl,
      options.cameraCode,
      options.cameraId,
      token,
    );
    if (whepUrl) {
      return {
        protocol: "webrtc",
        url: whepUrl,
      };
    }
  }
```

- [ ] **Step 4: 前端编译验证**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add frontend/.env.development frontend/src/utils/streamPreview.ts
git commit -m "feat: 前端直连 video-hub-service，WHIP URL 带 token 和 source_url"
```

---

### Task 5: 清理后处理 + 恢复候选过滤

**Files:**
- Modify: `video-hub-service/app/api/video_hub_webrtc.py`

**说明:** 现在直连架构下 offer 只有 1 个 m=video，不需要 `_reconstruct_sdp_answer` 补 rejected audio section。但保留函数以兼容可能的旧 offer。恢复候选过滤，只保留 preferred IP 的 UDP host 候选。保持 `a=setup` 不改（直连测试确认 `active` 是正确的）。

- [ ] **Step 1: 确认 whip_offer 中的后处理流程**

确认当前代码流程为：
1. `_reconstruct_sdp_answer`（保留，兼容旧 offer）
2. `_pin_answer_candidates`（保留，过滤候选）
3. 不改 `a=setup`

如果当前代码已是这样，无需修改。确认文件内容后决定。

- [ ] **Step 2: 运行全部测试**

Run: `cd video-hub-service && python -m pytest tests/ -v --timeout=30`
Expected: 全部 PASS

- [ ] **Step 3: 提交（如有修改）**

```bash
git add video-hub-service/app/api/video_hub_webrtc.py
git commit -m "chore: 确认 SDP 后处理流程"
```

---

### Task 6: 全链路集成测试

**Files:** 无代码修改

**说明:** 启动所有服务，在 Vue 前端页面验证 WebRTC 视频播放。

- [ ] **Step 1: 启动 backend、video-hub-service、前端 dev server**

- [ ] **Step 2: 打开 Vue 监控页面，切换到 webrtc 预览模式**

- [ ] **Step 3: 验证浏览器控制台日志**
  - `iceConnectionState` 应变为 `connected`
  - `connectionState` 应变为 `connected`
  - `track unmuted: video` 应出现
  - 不应出现 `dtlsState=failed`

- [ ] **Step 4: 用错误 token 测试，确认返回 401**

- [ ] **Step 5: 用测试页面 http://localhost:8888/test_webrtc_minimal.html 对比确认直连仍然正常**

- [ ] **Step 6: 最终提交**

```bash
git add -A
git commit -m "feat: WebRTC 直连架构完成，前端直连 video-hub-service，backend 统一认证"
```
