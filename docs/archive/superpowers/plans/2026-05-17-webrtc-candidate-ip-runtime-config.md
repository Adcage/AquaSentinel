# WebRTC 候选 IP 运行时配置实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 WebRTC ICE 候选替换 IP 从构建时 Vite 环境变量改为运行时从后端 API 获取，配合 Docker Compose `.env` 实现部署时单点配置。

**Architecture:** 项目根目录 `.env` 定义 `HOST_IP`，docker-compose 通过变量注入到 backend 和 video-hub。后端新增公开接口 `/api/system/webrtc-config` 返回 `preferredIp`。前端运行时从 API 获取候选 IP 并缓存，`VITE_WEBRTC_CANDIDATE_IPS` 作为开发环境降级后备。

**Tech Stack:** Java 17 / Spring Boot 3 / Vue 3 / TypeScript / Docker Compose

**设计文档:** `docs/archive/superpowers/specs/2026-05-17-webrtc-candidate-ip-runtime-config-design.md`

---

### Task 1: 后端 — 新增 webrtc-config 接口

**Files:**
- Modify: `backend/src/main/java/com/springboot/controller/VideoHubProxyController.java`
- Create: `backend/src/test/java/com/springboot/controller/VideoHubProxyControllerWebrtcConfigTest.java`

- [ ] **Step 1: 编写测试**

在 `VideoHubProxyControllerWebrtcConfigTest.java` 中：

```java
package com.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.springboot.common.BaseResponse;
import com.springboot.config.AppVideoHubProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class VideoHubProxyControllerWebrtcConfigTest {

    private VideoHubProxyController controller;

    private AppVideoHubProperties properties;

    @BeforeEach
    void setUp() {
        controller = new VideoHubProxyController();
        properties = new AppVideoHubProperties();
        ReflectionTestUtils.setField(controller, "appVideoHubProperties", properties);
    }

    @Test
    void getWebrtcConfigReturnsPreferredIpWhenSet() {
        properties.setPreferredIp("192.168.0.221");

        BaseResponse<Map<String, String>> result = controller.getWebrtcConfig();

        assertEquals(0, result.getCode());
        assertEquals("192.168.0.221", result.getData().get("preferredIp"));
    }

    @Test
    void getWebrtcConfigReturnsEmptyWhenNotSet() {
        properties.setPreferredIp("");

        BaseResponse<Map<String, String>> result = controller.getWebrtcConfig();

        assertEquals(0, result.getCode());
        assertEquals("", result.getData().get("preferredIp"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn test -Dtest="VideoHubProxyControllerWebrtcConfigTest" -pl .`
Expected: FAIL — `getWebrtcConfig()` 方法不存在

- [ ] **Step 3: 在 VideoHubProxyController 中实现接口**

在 `VideoHubProxyController.java` 中：

1. 添加 `@Resource private AppVideoHubProperties appVideoHubProperties;` 字段
2. 添加方法：

```java
@GetMapping("/webrtc-config")
public BaseResponse<Map<String, String>> getWebrtcConfig() {
    Map<String, String> data = new java.util.HashMap<>();
    data.put("preferredIp", appVideoHubProperties.getPreferredIp());
    return ResultUtils.success(data);
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn test -Dtest="VideoHubProxyControllerWebrtcConfigTest" -pl .`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/springboot/controller/VideoHubProxyController.java backend/src/test/java/com/springboot/controller/VideoHubProxyControllerWebrtcConfigTest.java
git commit -m "feat(backend): 新增 /video-hub/webrtc-config 接口返回候选 IP"
```

---

### Task 2: 后端 — 放行 webrtc-config 接口路径

**Files:**
- Modify: `backend/src/main/java/com/springboot/security/JwtAuthInterceptor.java`

- [ ] **Step 1: 在白名单中添加路径**

在 `JwtAuthInterceptor.java` 的 `WHITE_LIST` 中，`"/api/video-hub/**"` 已存在，它使用 `AntPathMatcher` 匹配，`/api/video-hub/webrtc-config` 会被 `"/api/video-hub/**"` 通配符覆盖，无需额外添加。

验证：`/api/video-hub/webrtc-config` 匹配 `"/api/video-hub/**"` → 已被放行。

- [ ] **Step 2: 确认无需改动，跳过提交**

此步骤无需代码变更，`/api/video-hub/**` 已覆盖新接口路径。

---

### Task 3: 前端 — 新增 webrtcConfigService 服务

**Files:**
- Create: `frontend/src/services/webrtcConfigService.ts`
- Create: `frontend/src/tests/webrtcConfigService.test.ts`

- [ ] **Step 1: 编写测试**

在 `webrtcConfigService.test.ts` 中：

```typescript
import { beforeEach, describe, expect, it, vi } from "vitest";

import { fetchWebrtcCandidateIps, resolveWebrtcCandidateIps, resetWebrtcConfigCache } from "@/services/webrtcConfigService";

describe("webrtcConfigService", () => {
  beforeEach(() => {
    resetWebrtcConfigCache();
  });

  describe("fetchWebrtcCandidateIps", () => {
    it("returns IPs from API when preferredIp is set", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: "192.168.0.221" } }),
      });
      vi.stubGlobal("fetch", mockFetch);
      vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "");

      const ips = await fetchWebrtcCandidateIps();

      expect(ips).toEqual(["192.168.0.221"]);
      expect(mockFetch).toHaveBeenCalledWith("/api/video-hub/webrtc-config");

      vi.unstubAllEnvs();
      vi.unstubGlobal("fetch");
    });

    it("falls back to VITE env when API returns empty preferredIp", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: "" } }),
      });
      vi.stubGlobal("fetch", mockFetch);
      vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "10.0.0.1,10.0.0.2");

      const ips = await fetchWebrtcCandidateIps();

      expect(ips).toEqual(["10.0.0.1", "10.0.0.2"]);

      vi.unstubAllEnvs();
      vi.unstubGlobal("fetch");
    });

    it("falls back to VITE env when API request fails", async () => {
      const mockFetch = vi.fn().mockRejectedValue(new Error("network error"));
      vi.stubGlobal("fetch", mockFetch);
      vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "192.168.0.181");

      const ips = await fetchWebrtcCandidateIps();

      expect(ips).toEqual(["192.168.0.181"]);

      vi.unstubAllEnvs();
      vi.unstubGlobal("fetch");
    });

    it("returns empty array when both API and VITE env are unavailable", async () => {
      const mockFetch = vi.fn().mockRejectedValue(new Error("network error"));
      vi.stubGlobal("fetch", mockFetch);
      vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "");

      const ips = await fetchWebrtcCandidateIps();

      expect(ips).toEqual([]);

      vi.unstubAllEnvs();
      vi.unstubGlobal("fetch");
    });

    it("caches result after first fetch", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: "192.168.0.221" } }),
      });
      vi.stubGlobal("fetch", mockFetch);
      vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "");

      await fetchWebrtcCandidateIps();
      await fetchWebrtcCandidateIps();

      expect(mockFetch).toHaveBeenCalledTimes(1);

      vi.unstubAllEnvs();
      vi.unstubGlobal("fetch");
    });
  });

  describe("resolveWebrtcCandidateIps", () => {
    it("returns cached IPs after fetch", async () => {
      const mockFetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ code: 0, data: { preferredIp: "192.168.0.100" } }),
      });
      vi.stubGlobal("fetch", mockFetch);
      vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", "");

      await fetchWebrtcCandidateIps();
      const ips = resolveWebrtcCandidateIps();

      expect(ips).toEqual(["192.168.0.100"]);

      vi.unstubAllEnvs();
      vi.unstubGlobal("fetch");
    });

    it("returns empty array before fetch", () => {
      const ips = resolveWebrtcCandidateIps();
      expect(ips).toEqual([]);
    });
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd frontend && npx vitest run src/tests/webrtcConfigService.test.ts`
Expected: FAIL — 模块不存在

- [ ] **Step 3: 实现 webrtcConfigService**

在 `webrtcConfigService.ts` 中：

```typescript
interface WebrtcConfigResponse {
  code?: number;
  data?: { preferredIp?: string };
  message?: string;
}

const IPV4_PATTERN = /^\d+\.\d+\.\d+\.\d+$/;

const parseIpList = (raw: string): string[] => {
  if (!raw.trim()) {
    return [];
  }
  return raw
    .split(",")
    .map((ip) => ip.trim())
    .filter((ip) => IPV4_PATTERN.test(ip));
};

let cachedIps: string[] | null = null;
let fetchPromise: Promise<string[]> | null = null;

const getFallbackIps = (): string[] => {
  const raw = String(import.meta.env.VITE_WEBRTC_CANDIDATE_IPS || "").trim();
  return parseIpList(raw);
};

export const fetchWebrtcCandidateIps = async (): Promise<string[]> => {
  if (cachedIps !== null) {
    return cachedIps;
  }
  if (fetchPromise) {
    return fetchPromise;
  }
  fetchPromise = (async () => {
    try {
      const response = await fetch("/api/video-hub/webrtc-config");
      if (response.ok) {
        const payload: WebrtcConfigResponse = await response.json();
        if (payload.code === 0 && payload.data?.preferredIp) {
          const ips = parseIpList(payload.data.preferredIp);
          if (ips.length > 0) {
            cachedIps = ips;
            return cachedIps;
          }
        }
      }
    } catch {}
    const fallback = getFallbackIps();
    cachedIps = fallback;
    return cachedIps;
  })();
  fetchPromise.finally(() => {
    fetchPromise = null;
  });
  return fetchPromise;
};

export const resolveWebrtcCandidateIps = (): string[] => {
  return cachedIps ?? [];
};

export const resetWebrtcConfigCache = () => {
  cachedIps = null;
  fetchPromise = null;
};
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd frontend && npx vitest run src/tests/webrtcConfigService.test.ts`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add frontend/src/services/webrtcConfigService.ts frontend/src/tests/webrtcConfigService.test.ts
git commit -m "feat(frontend): 新增 webrtcConfigService 运行时获取候选 IP"
```

---

### Task 4: 前端 — 修改 streamPreview.ts 使用新服务

**Files:**
- Modify: `frontend/src/utils/streamPreview.ts`
- Modify: `frontend/src/tests/streamPreview.test.ts`

- [ ] **Step 1: 修改 resolveWebrtcCandidateIps 为从缓存读取**

在 `streamPreview.ts` 中，替换现有的 `resolveWebrtcCandidateIps` 实现：

原代码（第 193-204 行）：
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

新代码：
```typescript
import { resolveWebrtcCandidateIps as resolveFromCache } from "@/services/webrtcConfigService";

export { resolveFromCache as resolveWebrtcCandidateIps };
```

同时保留 `replaceMdnsCandidates` 不变。

**注意**：`resolveWebrtcCandidateIps` 的签名保持 `(): string[]` 不变，但从缓存读取。缓存由 `fetchWebrtcCandidateIps()` 填充（在 `WebRtcWhepPlayer.vue` 中调用）。

- [ ] **Step 2: 更新 streamPreview.test.ts**

现有测试中 `resolveWebrtcCandidateIps` 从 `import.meta.env` 读取。现在它从缓存读取，需要先通过 `fetchWebrtcCandidateIps` 填充缓存，或直接测试新服务。

由于 `resolveWebrtcCandidateIps` 现在是 `webrtcConfigService` 的重导出，原测试文件中的 `resolveWebrtcCandidateIps` 测试应迁移到 `webrtcConfigService.test.ts`（已在 Task 3 完成）。

修改 `streamPreview.test.ts`：
1. 移除 `resolveWebrtcCandidateIps` 的 import 和 3 个相关测试用例
2. 移除 `vi.stubEnv("VITE_WEBRTC_CANDIDATE_IPS", ...)` 相关调用
3. 保留 `replaceMdnsCandidates` 和 `resolveCameraPreviewTarget` 的测试不变

- [ ] **Step 3: 运行测试确认通过**

Run: `cd frontend && npx vitest run src/tests/streamPreview.test.ts src/tests/webrtcConfigService.test.ts`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add frontend/src/utils/streamPreview.ts frontend/src/tests/streamPreview.test.ts
git commit -m "refactor(frontend): resolveWebrtcCandidateIps 改为从运行时缓存读取"
```

---

### Task 5: 前端 — 修改 WebRtcWhepPlayer 使用异步获取

**Files:**
- Modify: `frontend/src/components/business/WebRtcWhepPlayer.vue`

- [ ] **Step 1: 修改组件**

在 `WebRtcWhepPlayer.vue` 中：

1. 修改 import（第 14-22 行），替换：

```typescript
import {
  replaceMdnsCandidates,
  resolveWebrtcCandidateIps,
} from "@/utils/streamPreview";
```

为：

```typescript
import {
  fetchWebrtcCandidateIps,
  resolveWebrtcCandidateIps,
} from "@/services/webrtcConfigService";
import { replaceMdnsCandidates } from "@/utils/streamPreview";
```

2. 修改 `startConnection` 函数中第 233-237 行，替换：

```typescript
  const candidateIps = resolveWebrtcCandidateIps();
  let sdpToSend = pc.localDescription.sdp;
  if (candidateIps.length > 0) {
    sdpToSend = replaceMdnsCandidates(sdpToSend, candidateIps);
  }
```

为：

```typescript
  const candidateIps = await fetchWebrtcCandidateIps();
  let sdpToSend = pc.localDescription.sdp;
  if (candidateIps.length > 0) {
    sdpToSend = replaceMdnsCandidates(sdpToSend, candidateIps);
  }
```

- [ ] **Step 2: 运行类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 3: 提交**

```bash
git add frontend/src/components/business/WebRtcWhepPlayer.vue
git commit -m "feat(frontend): WebRtcWhepPlayer 运行时从 API 获取候选 IP"
```

---

### Task 6: 前端 — 更新环境变量文件

**Files:**
- Modify: `frontend/.env.production`
- Modify: `frontend/.env.development`
- Modify: `frontend/.env.example`

- [ ] **Step 1: 修改 .env.production**

移除 `VITE_WEBRTC_CANDIDATE_IPS=192.168.0.221` 行，并添加注释说明：

将第 14 行 `VITE_WEBRTC_CANDIDATE_IPS=192.168.0.221` 删除，因为生产环境通过 API 获取，不需要构建时注入。

- [ ] **Step 2: 修改 .env.development**

保留 `VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181`，但添加注释说明其作为降级后备：

将第 17 行 `VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181` 前的注释更新为：
```
# WebRTC ICE 候选替换 IP（开发降级后备，生产环境通过 /api/video-hub/webrtc-config 获取）
```

- [ ] **Step 3: 修改 .env.example**

更新第 29-33 行的注释，说明两种配置方式：

```
# WebRTC ICE 候选替换 IP（逗号分隔）
# 生产环境：通过后端 API /api/video-hub/webrtc-config 运行时获取，无需配置此项
# 开发环境：作为降级后备，填写 video-hub-service 所在机器的局域网 IP
# 示例：VITE_WEBRTC_CANDIDATE_IPS=192.168.0.181
VITE_WEBRTC_CANDIDATE_IPS=
```

- [ ] **Step 4: 提交**

```bash
git add frontend/.env.production frontend/.env.development frontend/.env.example
git commit -m "docs(frontend): 更新环境变量注释，说明候选 IP 两种配置方式"
```

---

### Task 7: Docker — 创建 .env.example 并更新 docker-compose.yml

**Files:**
- Create: `.env.example`
- Modify: `docker-compose.yml`

- [ ] **Step 1: 创建项目根目录 .env.example**

```env
# AquaSentinel Docker Compose 环境变量
# 复制此文件为 .env 并根据实际部署环境修改

# 宿主机局域网 IP（WebRTC ICE 候选用）
# 前端和 video-hub 均通过此值获知宿主机 IP，用于替换浏览器 mDNS 匿名化的 .local 地址
# 示例：HOST_IP=192.168.0.221
HOST_IP=
```

- [ ] **Step 2: 修改 docker-compose.yml**

1. backend 服务 environment 中新增（在第 48 行 `APP_VIDEO_HUB_BASE_URL` 后）：

```yaml
      APP_VIDEO_HUB_PREFERRED_IP: ${HOST_IP:-}
```

2. video-hub 服务 environment 中替换（第 64 行）：

将 `VIDEO_HUB_PREFERRED_IP: "192.168.0.221"` 替换为：
```yaml
      VIDEO_HUB_PREFERRED_IP: ${HOST_IP:-}
```

- [ ] **Step 3: 提交**

```bash
git add .env.example docker-compose.yml
git commit -m "feat(docker): HOST_IP 统一注入，候选 IP 不再硬编码"
```

---

### Task 8: 网络链路验证

**Files:** 无代码变更，仅验证

- [ ] **Step 1: 后端编译验证**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 后端全量测试**

Run: `cd backend && mvn test`
Expected: 全部通过

- [ ] **Step 3: 前端类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: 前端全量测试**

Run: `cd frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 5: Docker Compose 配置验证**

Run: `docker compose config`
Expected: 正常输出，`APP_VIDEO_HUB_PREFERRED_IP` 和 `VIDEO_HUB_PREFERRED_IP` 正确引用 `${HOST_IP:-}`

- [ ] **Step 6: 验证链路完整性**

确认以下链路在代码层面正确连接：

1. `.env` 中 `HOST_IP` → docker-compose `APP_VIDEO_HUB_PREFERRED_IP` → `AppVideoHubProperties.preferredIp` → `VideoHubProxyController.getWebrtcConfig()` → 返回 `{ preferredIp: "..." }`
2. `.env` 中 `HOST_IP` → docker-compose `VIDEO_HUB_PREFERRED_IP` → video-hub config
3. 前端 `fetchWebrtcCandidateIps()` → `GET /api/video-hub/webrtc-config` → 缓存 → `replaceMdnsCandidates()`
4. `/api/video-hub/webrtc-config` 被 `/api/video-hub/**` 白名单覆盖，无需鉴权
