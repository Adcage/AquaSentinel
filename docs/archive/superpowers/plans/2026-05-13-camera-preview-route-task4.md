# Backend Camera Preview Route Task 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 backend 明确区分设备原始地址与平台预览地址，对前端下发正式平台预览入口，同时保持 PTZ 控制继续指向 ESP32 设备基地址。

**Architecture:** 继续以 `CameraDevice.stream_url` 暂存 ESP32 原始 HTTP 地址，把“平台预览入口”作为派生字段由 `CameraPreviewRouteService` 统一生成。`CameraDeviceController` 和 `CameraDeviceServiceImpl` 负责把这个派生字段放进 `CameraDeviceVO`，`Esp32PtzControlService` 继续只解析设备原始地址，不读取平台预览地址。

**Tech Stack:** Spring Boot 3 / Java 17 / JUnit 5 / Maven

---

## 文件结构与职责映射

**Files:**
- Modify: `backend/src/main/java/com/springboot/model/vo/CameraDeviceVO.java`
- Modify: `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java`
- Modify: `backend/src/main/java/com/springboot/service/impl/CameraDeviceServiceImpl.java`
- Modify: `backend/src/main/java/com/springboot/service/Esp32PtzControlService.java`
- Modify: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`
- Create: `backend/src/test/java/com/springboot/service/Esp32PtzControlServiceTest.java`

职责分配：

1. `CameraDeviceVO.java` 新增 `previewUrl` 与 `deviceBaseUrl`，让前端直接拿正式预览入口，同时保留设备寻址信息用于调试或管理页展示。
2. `CameraPreviewRouteService.java` 统一生成 `previewUrl`、`deviceBaseUrl`，并继续负责 `video_hub` 代理 URL 编排。
3. `CameraDeviceServiceImpl.java` 在组装 VO 时补齐平台预览地址和设备基地址，避免前端自己拼接。
4. `Esp32PtzControlService.java` 显式声明“PTZ 控制基地址来自设备原始 stream_url 的 host/port 解析”，避免未来误用 `previewUrl`。
5. 测试覆盖 URL 编排、HTTP/RTSP 行为差异、PTZ 基地址解析行为。

## Task 1: 扩展预览路由服务的派生字段能力

**Files:**
- Modify: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`
- Modify: `backend/src/main/java/com/springboot/service/CameraPreviewRouteService.java`

- [ ] **Step 1: 写失败测试，定义平台预览地址与设备基地址行为**

```java
@Test
void buildPreviewUrlShouldReturnPlatformPreviewEndpoint() {
    CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

    String previewUrl = service.buildPreviewUrl(httpCamera());

    assertEquals("/streams/cameras/1001/preview", URI.create(previewUrl).getPath());
    assertTrue(previewUrl.contains("token="));
}

@Test
void buildDeviceBaseUrlShouldReturnEsp32Origin() {
    CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

    assertEquals("http://192.168.1.88", service.buildDeviceBaseUrl(httpCamera()));
}

@Test
void buildPreviewUrlShouldReturnBlankForUnsupportedCamera() {
    CameraPreviewRouteService service = new CameraPreviewRouteService(newProperties());

    assertEquals("", service.buildPreviewUrl(rtspCamera()));
}
```

- [ ] **Step 2: 运行测试，确认当前行为不满足新契约**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest`
Expected: FAIL，提示 `buildPreviewUrl` / `buildDeviceBaseUrl` 方法不存在或断言不匹配。

- [ ] **Step 3: 在服务层实现 previewUrl 与 deviceBaseUrl 生成**

```java
public String buildPreviewUrl(CameraDevice cameraDevice) {
    if (cameraDevice == null || cameraDevice.getId() == null) {
        return "";
    }
    if (!supportsVideoHub(cameraDevice)) {
        return "";
    }
    return "/streams/cameras/" + cameraDevice.getId() + "/preview";
}

public String buildDeviceBaseUrl(CameraDevice cameraDevice) {
    if (cameraDevice == null) {
        return "";
    }
    String streamUrl = StringUtils.defaultString(cameraDevice.getStream_url()).trim();
    if (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")) {
        return "";
    }
    URI uri = URI.create(streamUrl);
    if (StringUtils.isBlank(uri.getScheme()) || StringUtils.isBlank(uri.getHost())) {
        return "";
    }
    String base = uri.getScheme() + "://" + uri.getHost();
    if (uri.getPort() > 0) {
        base += ":" + uri.getPort();
    }
    return base;
}
```

说明：这里保持 `buildVideoHubStreamUri()` 不变，因为它是 backend 访问 yolo-service 的内部 URL 编排；新增的 `buildPreviewUrl()` 是给前端的正式外部平台入口，和修改前直接暴露设备流地址不同。

- [ ] **Step 4: 运行测试，确认预览路由契约通过**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest`
Expected: PASS

## Task 2: 让 CameraDeviceVO 直接携带平台预览地址

**Files:**
- Modify: `backend/src/main/java/com/springboot/model/vo/CameraDeviceVO.java`
- Modify: `backend/src/main/java/com/springboot/service/impl/CameraDeviceServiceImpl.java`
- Modify: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`

- [ ] **Step 1: 写失败测试，定义 VO 输出字段**

```java
@Test
void getCameraDeviceVOShouldIncludePreviewAndDeviceBaseUrl() {
    AppAiEngineProperties properties = newProperties();
    CameraPreviewRouteService previewRouteService = new CameraPreviewRouteService(properties);
    CameraDeviceServiceImpl service = new CameraDeviceServiceImpl();
    ReflectionTestUtils.setField(service, "cameraPreviewRouteService", previewRouteService);

    CameraDeviceVO vo = service.getCameraDeviceVO(httpCamera());

    assertEquals("/streams/cameras/1001/preview", vo.getPreviewUrl());
    assertEquals("http://192.168.1.88", vo.getDeviceBaseUrl());
}
```

- [ ] **Step 2: 运行测试，确认 VO 还没输出这些字段**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest`
Expected: FAIL，提示 `previewUrl` / `deviceBaseUrl` 字段不存在，或 `cameraPreviewRouteService` 未注入。

- [ ] **Step 3: 最小实现 VO 字段与组装逻辑**

```java
// CameraDeviceVO.java
private String previewUrl;

private String deviceBaseUrl;
```

```java
// CameraDeviceServiceImpl.java
@Resource
private CameraPreviewRouteService cameraPreviewRouteService;

@Override
public CameraDeviceVO getCameraDeviceVO(CameraDevice cameraDevice) {
    if (cameraDevice == null) {
        return null;
    }
    CameraDeviceVO cameraDeviceVO = new CameraDeviceVO();
    cameraDeviceVO.setId(cameraDevice.getId());
    cameraDeviceVO.setVenueId(cameraDevice.getVenue_id());
    cameraDeviceVO.setZoneId(cameraDevice.getZone_id());
    cameraDeviceVO.setCameraCode(cameraDevice.getCamera_code());
    cameraDeviceVO.setCameraName(cameraDevice.getCamera_name());
    cameraDeviceVO.setStreamUrl(cameraDevice.getStream_url());
    cameraDeviceVO.setPreviewUrl(cameraPreviewRouteService.buildPreviewUrl(cameraDevice));
    cameraDeviceVO.setDeviceBaseUrl(cameraPreviewRouteService.buildDeviceBaseUrl(cameraDevice));
    cameraDeviceVO.setProtocol(cameraDevice.getProtocol());
    cameraDeviceVO.setDeviceStatus(cameraDevice.getDevice_status());
    cameraDeviceVO.setHealthStatus(cameraDevice.getHealth_status());
    cameraDeviceVO.setEnabled(cameraDevice.getEnabled());
    cameraDeviceVO.setLastHeartbeatAt(cameraDevice.getLast_heartbeat_at());
    cameraDeviceVO.setCreatedAt(cameraDevice.getCreated_at());
    cameraDeviceVO.setUpdatedAt(cameraDevice.getUpdated_at());
    return cameraDeviceVO;
}
```

说明：这里保留 `streamUrl` 原值，是为了避免阶段一直接大改数据库语义；新增 `previewUrl`/`deviceBaseUrl` 才是把修改前“前端猜设备流地址”的模式收口到 backend 编排。

- [ ] **Step 4: 运行测试，确认前端可直接拿正式平台入口**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest`
Expected: PASS

## Task 3: 固化 PTZ 设备寻址只走设备原始地址

**Files:**
- Create: `backend/src/test/java/com/springboot/service/Esp32PtzControlServiceTest.java`
- Modify: `backend/src/main/java/com/springboot/service/Esp32PtzControlService.java`

- [ ] **Step 1: 写失败测试，定义设备基地址解析规则**

```java
@Test
void resolveDeviceBaseUrlShouldExtractOriginFromHttpStreamUrl() {
    Esp32PtzControlService service = new Esp32PtzControlService();
    CameraDevice cameraDevice = new CameraDevice();
    cameraDevice.setStream_url("http://192.168.137.86:81/stream");

    String baseUrl = ReflectionTestUtils.invokeMethod(service, "resolveDeviceBaseUrl", cameraDevice);

    assertEquals("http://192.168.137.86:81", baseUrl);
}

@Test
void resolveDeviceBaseUrlShouldRejectPlatformPreviewPath() {
    Esp32PtzControlService service = new Esp32PtzControlService();
    CameraDevice cameraDevice = new CameraDevice();
    cameraDevice.setStream_url("/streams/cameras/1001/preview");

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> ReflectionTestUtils.invokeMethod(service, "resolveDeviceBaseUrl", cameraDevice));

    assertTrue(exception.getMessage().contains("设备 streamUrl 需为 ESP32 HTTP 地址"));
}
```

- [ ] **Step 2: 运行测试，确认当前规则没有显式文档化约束**

Run: `mvn test -Dtest=Esp32PtzControlServiceTest`
Expected: FAIL，测试类不存在或断言不匹配。

- [ ] **Step 3: 在 PTZ 服务中显式注释并保持设备地址优先**

```java
private String resolveDeviceBaseUrl(CameraDevice cameraDevice) {
    // 阶段一仍由 stream_url 保存 ESP32 原始 HTTP 地址，PTZ 控制必须始终指向设备本体，
    // 不能使用 backend 平台预览地址或 yolo-service video_hub 地址。
    String streamUrl = StringUtils.defaultString(cameraDevice.getStream_url()).trim();
    if (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://")) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备 streamUrl 需为 ESP32 HTTP 地址");
    }
    URI uri = URI.create(streamUrl);
    String scheme = uri.getScheme();
    String host = uri.getHost();
    int port = uri.getPort();
    if (StringUtils.isBlank(scheme) || StringUtils.isBlank(host)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "设备 streamUrl 无法解析地址");
    }
    String base = scheme + "://" + host;
    if (port > 0) {
        base += ":" + port;
    }
    return base;
}
```

说明：实现上基本保持原逻辑不变，重点是把“当前阶段 stream_url 暂存设备原始地址”的约束写清楚，防止未来把 `previewUrl` 误接进 PTZ 控制。

- [ ] **Step 4: 运行测试，确认 PTZ 仍然只寻址 ESP32 本体**

Run: `mvn test -Dtest=Esp32PtzControlServiceTest`
Expected: PASS

## Task 4: 做一次 Task 4 回归验证

**Files:**
- Verify: `backend/src/test/java/com/springboot/service/CameraPreviewRouteServiceTest.java`
- Verify: `backend/src/test/java/com/springboot/service/Esp32PtzControlServiceTest.java`
- Verify: `backend/src/test/java/com/springboot/service/stream/StreamProviderRouterTest.java`

- [ ] **Step 1: 运行聚合回归测试**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest,Esp32PtzControlServiceTest,StreamProviderRouterTest`
Expected: PASS，平台预览地址编排、PTZ 基地址解析、现有 stream provider 路由均通过。

- [ ] **Step 2: 手工联调检查点**

Run: `mvn test -Dtest=CameraPreviewRouteServiceTest,Esp32PtzControlServiceTest,StreamProviderRouterTest`
Expected: PASS

手工检查：

1. 调 `GET /cameras/get/vo?id=<cameraId>` 时，响应里同时有 `streamUrl`、`previewUrl`、`deviceBaseUrl`。
2. `previewUrl` 指向 `/streams/cameras/{cameraId}/preview`，不再要求前端自己拼 `video_hub` 地址。
3. 打平台预览时，PTZ 控制仍然命中 ESP32 设备基地址，不会指到 platform preview。

---

## 自检

1. 规格覆盖：已覆盖 Task 4 的三个核心要求：平台预览地址概念、PTZ 设备基地址隔离、前端可拿正式预览入口。
2. 占位检查：无 TBD / TODO / “自行实现” 类占位。
3. 类型一致性：统一使用 `previewUrl`、`deviceBaseUrl`、`streamUrl` 三个字段名，避免后续计划中混用。
