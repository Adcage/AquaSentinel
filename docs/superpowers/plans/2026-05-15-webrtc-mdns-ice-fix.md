# WebRTC mDNS ICE 候选替换修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 WebRTC 黑屏问题——浏览器 mDNS 隐私保护将 ICE 候选地址匿名化为 `*.local`，aiortc 无法解析，导致 ICE 配对失败。通过在 offer 发出前将 `.local` 替换为可配置的真实 IP 来解决。

**Architecture:** 前端在 SDP offer 发送前扫描替换 `.local` 地址；video-hub-service 在 answer 候选无真实 IPv4 时回退到配置的 `preferred_ip`；backend 代理将 `preferredIp` 透传给 video-hub-service。

**Tech Stack:** Vue 3 / TypeScript（前端）、Flask / aiortc（video-hub-service）、Spring Boot（backend）

**设计文档:** `docs/superpowers/specs/2026-05-15-webrtc-mdns-ice-fix-design.md`

---

## Task 1: 前端新增 ICE 候选替换工具函数

**Files:**
- Modify: `frontend/src/utils/streamPreview.ts`

- [ ] **Step 1: 在 `streamPreview.ts` 末尾添加 `replaceMdnsCandidates` 函数**

该函数接收 SDP 字符串和候选 IP 列表，将 `a=candidate:` 行中的 `.local` 地址替换为配置的真实 IP。

```typescript
export const replaceMdnsCandidates = (
  sdp: string,
  candidateIps: string[],
): string => {
  if (!candidateIps.length) {
    return sdp;
  }
  const lines = sdp.split("\r\n");
  let ipIndex = 0;
  const replaced = lines.map((line) => {
    if (!line.startsWith("a=candidate:")) {
      return line;
    }
    const parts = line.split(" ");
    if (parts.length < 6) {
      return line;
    }
    const address = parts[4];
    if (address.endsWith(".local")) {
      const replacement = candidateIps[ipIndex % candidateIps.length];
      ipIndex++;
      parts[4] = replacement;
      return parts.join(" ");
    }
    return line;
  });
  return replaced.join("\r\n");
};
```

- [ ] **Step 2: 添加 `resolveWebrtcCandidateIps` 函数**

从环境变量读取并解析 `VITE_WEBRTC_CANDIDATE_IPS`，返回 IP 数组。

```typescript
export const resolveWebrtcCandidateIps = (): string[] => {
  const raw = String(
    import.meta.env.VITE_WEBRTC_CANDIDATE_IPS || "",
  ).trim();
  if (!raw) {
    return [];
  }
  return raw
    .split(",")
    .map((ip) => ip.trim())
    .filter((ip) => /^\d+\.\d+\.\d+\.\d+$/.test(ip));
};
```

---

## Task 2: 前端 WebRtcWhepPlayer 集成 ICE 候选替换

**Files:**
- Modify: `frontend/src/components/business/WebRtcWhepPlayer.vue`

- [ ] **Step 1: 在 `<script setup>` 中导入替换函数**

在现有 import 之后添加：

```typescript
import {
  replaceMdnsCandidates,
  resolveWebrtcCandidateIps,
} from "@/utils/streamPreview";
```

- [ ] **Step 2: 在 `startConnection` 函数中，`waitIceGatheringDone` 之后、`fetch` 之前插入替换逻辑**

找到 `startConnection` 函数中 `logSdpSummary("offer", pc.localDescription.sdp);` 这行，在其后、`const response = await fetch(src,` 之前插入：

```typescript
  const candidateIps = resolveWebrtcCandidateIps();
  const originalSdp = pc.localDescription.sdp;
  const patchedSdp = replaceMdnsCandidates(originalSdp, candidateIps);
  if (patchedSdp !== originalSdp) {
    debugLog("SDP offer mDNS 候选已替换", {
      replacedCount: originalSdp.split("\r\n").filter(
        (l) => l.startsWith("a=candidate:") && l.includes(".local"),
      ).length,
      candidateIps,
    });
  }
```

- [ ] **Step 3: 将 fetch body 从 `pc.localDescription.sdp` 改为 `patchedSdp`**

将：
```typescript
    body: pc.localDescription.sdp,
```
改为：
```typescript
    body: patchedSdp,
```

---

## Task 3: 前端 .env 文件添加配置项并切换回 webrtc 模式

**Files:**
- Modify: `frontend/.env`
- Modify: `frontend/.env.development`
- Modify: `frontend/.env.production`
- Modify: `frontend/.env.example`

- [ ] **Step 1: 修改 `frontend/.env`**

在文件末尾添加：

```
# WebRTC ICE 候选替换 IP（逗号分隔，用于替换浏览器 mDNS 匿名化的 .local 地址）
# 填写 video-hub-service 所在机器的局域网 IP
VITE_WEBRTC_CANDIDATE_IPS=
```

- [ ] **Step 2: 修改 `frontend/.env.development`**

将 `VITE_CAMERA_PREVIEW_MODE=backend_proxy` 改为 `VITE_CAMERA_PREVIEW_MODE=webrtc`，在文件末尾添加：

```
VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
```

- [ ] **Step 3: 修改 `frontend/.env.production`**

将 `VITE_CAMERA_PREVIEW_MODE=backend_proxy` 改为 `VITE_CAMERA_PREVIEW_MODE=webrtc`，在文件末尾添加：

```
VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
```

- [ ] **Step 4: 修改 `frontend/.env.example`**

在文件末尾添加：

```
# WebRTC ICE 候选替换 IP（逗号分隔）
# 浏览器 mDNS 隐私保护会将 ICE 候选地址匿名化为 .local
# 此配置用于将 .local 替换为 video-hub-service 所在机器的真实局域网 IP
# 示例：VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
VITE_WEBRTC_CANDIDATE_IPS=
```

---

## Task 4: video-hub-service 配置新增 preferred_ip

**Files:**
- Modify: `video-hub-service/app/core/config.py`

- [ ] **Step 1: 在 `BaseConfig` 类中新增 `VIDEO_HUB_PREFERRED_IP` 配置**

在 `VIDEO_HUB_DEFAULT_TARGET_FPS` 之后添加：

```python
    VIDEO_HUB_PREFERRED_IP = os.environ.get("VIDEO_HUB_PREFERRED_IP", "")
```

---

## Task 5: video-hub-service WebrtcSessionManager 支持 preferred_ip 回退

**Files:**
- Modify: `video-hub-service/app/video_hub/webrtc_session.py`

- [ ] **Step 1: 修改 `create_whip_session` 方法签名，添加 `preferred_ip` 参数**

将方法签名从：
```python
    async def create_whip_session(
        self, camera_id: int, sdp_offer: str, session=None
    ) -> tuple[str, str]:
```
改为：
```python
    async def create_whip_session(
        self, camera_id: int, sdp_offer: str, session=None, preferred_ip: str | None = None
    ) -> tuple[str, str]:
```

- [ ] **Step 2: 在 `create_whip_session` 中存储 `preferred_ip` 供候选处理使用**

在方法体开头（`if session is None:` 之前）添加：

```python
        self._current_preferred_ip = preferred_ip
```

- [ ] **Step 3: 添加 `_current_preferred_ip` 实例属性**

在 `__init__` 方法中，`self._max_sessions_per_camera` 之后添加：

```python
        self._current_preferred_ip: str | None = None
```

---

## Task 6: video-hub-service WHIP 端点传入 preferred_ip

**Files:**
- Modify: `video-hub-service/app/api/video_hub_webrtc.py`

- [ ] **Step 1: 在 `whip_offer` 函数中从 Flask config 读取 `preferred_ip`**

在 `logger.info("WHIP offer 首选 IPv4 candidate: %s (localhost=%s)", preferred_offer_ipv4, has_localhost_candidate)` 行之后、`session = video_hub_registry.get_session(camera_id)` 之前添加：

```python
    preferred_ip = current_app.config.get("VIDEO_HUB_PREFERRED_IP", "")
    if preferred_ip:
        logger.info("WHIP 使用配置的 preferred_ip: %s", preferred_ip)
```

同时在文件顶部 import 区域添加：

```python
from flask import Blueprint, Response, jsonify, request, current_app
```

（在现有 `from flask import` 行中添加 `current_app`）

- [ ] **Step 2: 将 `preferred_ip` 传入 `create_whip_session`**

将所有 `webrtc_session_manager.create_whip_session(camera_id, sdp_offer, session)` 调用改为 `webrtc_session_manager.create_whip_session(camera_id, sdp_offer, session, preferred_ip=preferred_ip or None)`。

具体需要修改两处：

第一处（正常路径）：
```python
        sdp_answer, session_id = run_async(
            webrtc_session_manager.create_whip_session(camera_id, sdp_offer, session, preferred_ip=preferred_ip or None)
        )
```

第二处（降级过滤模式）：
```python
            sdp_answer, session_id = run_async(
                webrtc_session_manager.create_whip_session(camera_id, sdp_offer, session, preferred_ip=preferred_ip or None)
            )
```

- [ ] **Step 3: 修改 `_pin_answer_candidates` 调用，传入 `preferred_ip` 作为回退**

将：
```python
    sdp_answer = _pin_answer_candidates(sdp_answer, preferred_offer_ipv4)
```
改为：
```python
    fallback_ip = preferred_ip if preferred_ip else None
    sdp_answer = _pin_answer_candidates(sdp_answer, preferred_offer_ipv4, fallback_ip)
```

- [ ] **Step 4: 修改 `_pin_answer_candidates` 函数签名和逻辑，支持 fallback_ip**

将函数签名从：
```python
def _pin_answer_candidates(answer_sdp: str, preferred_address: str | None) -> str:
```
改为：
```python
def _pin_answer_candidates(answer_sdp: str, preferred_address: str | None, fallback_ip: str | None = None) -> str:
```

将函数体中所有使用 `preferred_address` 的条件逻辑，在 `preferred_address` 为 `None` 时改用 `fallback_ip`。具体修改：

将：
```python
        if preferred_address and address == preferred_address:
```
改为：
```python
        effective_preferred = preferred_address or fallback_ip
        if effective_preferred and address == effective_preferred:
```

将：
```python
        elif preferred_address and _is_preferred_ipv4(address):
            parts[4] = preferred_address
```
改为：
```python
        elif effective_preferred and _is_preferred_ipv4(address):
            parts[4] = effective_preferred
```

将：
```python
        elif not preferred_address and _is_preferred_ipv4(address):
```
改为：
```python
        elif not effective_preferred and _is_preferred_ipv4(address):
```

将日志中的 `preferred=%s` 改为 `preferred=%s`，值改为 `effective_preferred`：

```python
    logger.info(
        "SDP answer candidates 调整: preferred=%s kept=%d removed=%d",
        effective_preferred,
        kept_count,
        removed_count,
    )
```

---

## Task 7: backend 新增 preferredIp 配置并透传

**Files:**
- Modify: `backend/src/main/java/com/springboot/config/AppVideoHubProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java`

- [ ] **Step 1: `AppVideoHubProperties` 新增 `preferredIp` 字段**

在 `private long timeoutMs = 5000;` 之后添加：

```java
    private String preferredIp = "";
```

- [ ] **Step 2: `application.yml` 新增 `preferred-ip` 配置**

在 `timeout-ms: 5000` 之后添加：

```yaml
    preferred-ip: 192.168.0.181
```

- [ ] **Step 3: `VideoHubProxyController.whipOffer` 透传 `preferredIp`**

在 `UriComponentsBuilder` 构建完成后、`URI targetUri = uriBuilder.build().encode().toUri();` 之前添加：

```java
        if (StringUtils.isNotBlank(videoHubProperties.getPreferredIp())) {
            uriBuilder.queryParam("preferred_ip", videoHubProperties.getPreferredIp());
        }
```

---

## Task 8: video-hub-service WHIP 端点接收 preferred_ip 参数

**Files:**
- Modify: `video-hub-service/app/api/video_hub_webrtc.py`

- [ ] **Step 1: 在 `whip_offer` 函数中优先从 query param 读取 `preferred_ip`**

将 `preferred_ip` 的赋值逻辑从：
```python
    preferred_ip = current_app.config.get("VIDEO_HUB_PREFERRED_IP", "")
```
改为：
```python
    preferred_ip = str(request.args.get("preferred_ip") or "").strip()
    if not preferred_ip:
        preferred_ip = current_app.config.get("VIDEO_HUB_PREFERRED_IP", "")
```

这样 backend 透传的 `preferred_ip` 优先级高于本地环境变量配置。

---

## Task 9: 前端单元测试

**Files:**
- Modify: `frontend/src/tests/streamPreview.test.ts`

- [ ] **Step 1: 添加 `replaceMdnsCandidates` 测试**

在文件末尾添加：

```typescript
describe("replaceMdnsCandidates", () => {
  it("replaces .local addresses in candidate lines", () => {
    const sdp =
      "v=0\r\n" +
      "a=candidate:1 1 UDP 12345 acad7439-1bcf-4505.local 51000 typ host\r\n" +
      "a=candidate:2 1 UDP 12346 192.168.0.181 51001 typ host\r\n" +
      "a=end-of-candidates\r\n";
    const result = replaceMdnsCandidates(sdp, ["192.168.0.181"]);
    expect(result).toContain("192.168.0.181 51000 typ host");
    expect(result).toContain("192.168.0.181 51001 typ host");
    expect(result).not.toContain(".local");
  });

  it("does not replace when candidateIps is empty", () => {
    const sdp =
      "a=candidate:1 1 UDP 12345 acad7439.local 51000 typ host\r\n";
    const result = replaceMdnsCandidates(sdp, []);
    expect(result).toContain("acad7439.local");
  });

  it("does not replace non-.local addresses", () => {
    const sdp =
      "a=candidate:1 1 UDP 12345 192.168.0.181 51000 typ host\r\n";
    const result = replaceMdnsCandidates(sdp, ["10.0.0.1"]);
    expect(result).toContain("192.168.0.181 51000 typ host");
    expect(result).not.toContain("10.0.0.1");
  });

  it("handles multiple .local candidates with IP rotation", () => {
    const sdp =
      "a=candidate:1 1 UDP 12345 aaa.local 51000 typ host\r\n" +
      "a=candidate:2 1 UDP 12346 bbb.local 51001 typ host\r\n";
    const result = replaceMdnsCandidates(sdp, ["192.168.0.181", "192.168.137.1"]);
    expect(result).toContain("192.168.0.181 51000 typ host");
    expect(result).toContain("192.168.137.1 51001 typ host");
  });
});
```

- [ ] **Step 2: 添加 `resolveWebrtcCandidateIps` 测试**

```typescript
describe("resolveWebrtcCandidateIps", () => {
  it("returns empty array when env is not set", () => {
    const ips = resolveWebrtcCandidateIps();
    expect(ips).toEqual([]);
  });

  it("parses comma-separated IPs", () => {
    vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "192.168.0.1,10.0.0.1");
    const ips = resolveWebrtcCandidateIps();
    expect(ips).toEqual(["192.168.0.1", "10.0.0.1"]);
    vi.unstubAllEnvs();
  });

  it("filters invalid entries", () => {
    vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "192.168.0.1,invalid,10.0.0.1");
    const ips = resolveWebrtcCandidateIps();
    expect(ips).toEqual(["192.168.0.1", "10.0.0.1"]);
    vi.unstubAllEnvs();
  });
});
```

注意：`resolveWebrtcCandidateIps` 测试需要 `vi.stubEnv`，需要在 describe 块顶部添加 `import { vi } from "vitest";`（如果还没有的话）。

---

## Task 10: video-hub-service 单元测试

**Files:**
- Modify: `video-hub-service/tests/test_video_hub_webrtc.py`

- [ ] **Step 1: 添加 `_pin_answer_candidates` 带 `fallback_ip` 的测试**

在文件末尾添加：

```python
from app.api.video_hub_webrtc import _pin_answer_candidates


def test_pin_answer_candidates_uses_fallback_ip_when_no_preferred():
    sdp = (
        "v=0\r\n"
        "a=candidate:1 1 UDP 100 10.0.0.1 5000 typ host\r\n"
        "a=candidate:2 1 UDP 200 192.168.1.1 5001 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address=None, fallback_ip="192.168.0.181")
    assert "192.168.0.181" in result
    assert "10.0.0.1" not in result


def test_pin_answer_candidates_prefers_preferred_over_fallback():
    sdp = (
        "v=0\r\n"
        "a=candidate:1 1 UDP 100 10.0.0.1 5000 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address="10.0.0.1", fallback_ip="192.168.0.181")
    assert "10.0.0.1" in result


def test_pin_answer_candidates_no_preferred_no_fallback():
    sdp = (
        "v=0\r\n"
        "a=candidate:1 1 UDP 100 10.0.0.1 5000 typ host\r\n"
    )
    result = _pin_answer_candidates(sdp, preferred_address=None, fallback_ip=None)
    assert "10.0.0.1" in result
```

---

## Task 11: 运行全量测试验证

- [ ] **Step 1: 运行前端单元测试**

```bash
cd frontend && npm test
```

Expected: 所有测试通过，包括新增的 `replaceMdnsCandidates` 和 `resolveWebrtcCandidateIps` 测试。

- [ ] **Step 2: 运行 video-hub-service 测试**

```bash
cd video-hub-service && pytest
```

Expected: 所有测试通过，包括新增的 `_pin_answer_candidates` fallback_ip 测试。

- [ ] **Step 3: 运行 backend 测试**

```bash
cd backend && mvn test
```

Expected: 所有测试通过。

- [ ] **Step 4: 前端类型检查**

```bash
cd frontend && npx vue-tsc --noEmit
```

Expected: 无新增类型错误（允许已有的 7 个历史错误）。

---

## Task 12: 端到端手动验证

- [ ] **Step 1: 重启前端 dev server**

```bash
cd frontend && npm run dev
```

- [ ] **Step 2: 从 `192.168.0.181:5173` 访问监控总览页面**

- [ ] **Step 3: 验证 WebRTC 模式下画面出图**

打开浏览器 DevTools → Console，确认：
- `[WebRtcWhepPlayer] SDP offer mDNS 候选已替换` 日志出现
- `connectionState` 从 `connecting` 变为 `connected`
- `iceConnectionState` 变为 `connected` 或 `completed`
- 视频画面正常显示

- [ ] **Step 4: 验证 MJPEG 降级路径**

将 `VITE_CAMERA_PREVIEW_MODE` 改为 `backend_proxy`，确认 MJPEG 预览仍正常工作。