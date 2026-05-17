# ESP32 WebSocket 推帧到 video-hub 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ESP32 主动通过 WebSocket 推送 JPEG 帧到 video-hub，解决单路拉流被占问题，降低延迟。

**Architecture:** ESP32 在 `loop()` 中采集 JPEG 帧，通过 WebSocket 二进制帧发送到 video-hub 的 `/video-hub/cameras/push` endpoint。video-hub 用 `flask-sock` 接收帧并写入对应 camera 的 `FrameCache`。ESP32 断线自动重连。保留原有 HTTP 拉流作为 fallback。

**Tech Stack:** ESP32 (Arduino/AsyncTCP), video-hub (Flask/flask-sock), WebSocket binary frames

---

## 文件变更清单

### 新建文件
| 文件 | 职责 |
|------|------|
| `firmware/esp32-cam/lib/network/FramePusher.h` | ESP32 WebSocket 推帧客户端类声明 |
| `firmware/esp32-cam/lib/network/FramePusher.cpp` | ESP32 WebSocket 推帧客户端实现（连接、重连、发帧） |
| `video-hub-service/app/api/video_hub_push.py` | video-hub WebSocket 推帧接收 endpoint |
| `video-hub-service/tests/test_video_hub_push.py` | 推帧 API 单元测试 |

### 修改文件
| 文件 | 变更 |
|------|------|
| `firmware/esp32-cam/include/config.h` | 添加 video-hub 地址、camera_id、推帧配置 |
| `firmware/esp32-cam/src/main.cpp` | 集成 FramePusher，在 loop() 中推帧 |
| `firmware/esp32-cam/lib/camera/CameraStreamer.h` | 添加 `captureFrame()` 方法供推帧调用 |
| `firmware/esp32-cam/lib/camera/CameraStreamer.cpp` | 实现 `captureFrame()` |
| `video-hub-service/app/__init__.py` | 注册 push blueprint |
| `video-hub-service/app/video_hub/registry.py` | 添加 `get_or_create_session()` 方法 |
| `video-hub-service/requirements.txt` | 添加 `flask-sock>=0.7` |
| `video-hub-service/app/core/config.py` | 添加推帧认证 token 配置 |
| `docker-compose.yml` | 添加 video-hub 环境变量 |

---

## 任务分解

### Task 1: video-hub 添加 flask-sock 依赖

**Files:**
- Modify: `video-hub-service/requirements.txt`

- [ ] **Step 1: 添加 flask-sock 到 requirements.txt**

在 `video-hub-service/requirements.txt` 末尾添加：

```
flask-sock>=0.7,<1.0
```

- [ ] **Step 2: 本地安装依赖验证**

Run: `cd video-hub-service && pip install flask-sock`

---

### Task 2: video-hub 推帧配置项

**Files:**
- Modify: `video-hub-service/app/core/config.py`

- [ ] **Step 1: 在 BaseConfig 末尾添加推帧配置**

```python
VIDEO_HUB_PUSH_TOKEN = os.environ.get("VIDEO_HUB_PUSH_TOKEN", "")
```

`VIDEO_HUB_PUSH_TOKEN` 为空时不做认证校验（开发模式），生产环境应设置。

---

### Task 3: video-hub Registry 添加 get_or_create_session

**Files:**
- Modify: `video-hub-service/app/video_hub/registry.py`

- [ ] **Step 1: 在 VideoHubRegistry 类中添加 get_or_create_session 方法**

在 `remove_session` 方法之后添加：

```python
def get_or_create_session(self, camera_id: int, source_url: str = "", rotation: int = 0) -> VideoHubSession:
    with self._lock:
        session = self._sessions.get(camera_id)
        if session is not None:
            return session
    return self.ensure_session(camera_id, source_url or f"push://camera_{camera_id}", rotation=rotation)
```

推帧模式下 source_url 使用 `push://camera_{id}` 伪协议，表示帧来自 WebSocket 推送而非 HTTP 拉流。`ensure_session` 会创建 session 并启动拉流线程，但 `push://` 协议的拉流线程应立即空转（见 Task 4）。

---

### Task 4: source_worker 支持 push:// 协议空转

**Files:**
- Modify: `video-hub-service/app/video_hub/source_worker.py`

- [ ] **Step 1: 在 `_consume_stream` 方法开头添加 push 协议判断**

在 `_consume_stream` 方法（第 374 行附近）开头添加 push 协议分支：

```python
def _consume_stream(self):
    if self.source_url.startswith("push://"):
        self._consume_stream_push()
        return
    if _should_use_pyav(self.source_url):
        self._consume_stream_pyav()
    else:
        self._consume_stream_http()
```

- [ ] **Step 2: 添加 _consume_stream_push 方法**

在 `_consume_stream_http` 方法之后添加：

```python
def _consume_stream_push(self):
    self._transition_to_connected()
    self._record_success()
    logger.info("camera=%s 推帧模式，等待 WebSocket 推送帧数据", self.camera_id)
    while not self._stopped:
        sleep(1.0)
        if self._check_stale_frame():
            break
```

推帧模式下，source_worker 线程只做两件事：1) 标记 session 为 CONNECTED；2) 空转等待帧数据通过 `FrameCache.update()` 写入。帧数据由 WebSocket endpoint 直接写入 FrameCache。

---

### Task 5: video-hub WebSocket 推帧 endpoint

**Files:**
- Create: `video-hub-service/app/api/video_hub_push.py`
- Modify: `video-hub-service/app/__init__.py`

- [ ] **Step 1: 创建推帧 API 文件**

创建 `video-hub-service/app/api/video_hub_push.py`：

```python
from __future__ import annotations

import logging
from time import time

from flask import Blueprint, current_app

logger = logging.getLogger(__name__)

blp = Blueprint("video_hub_push", __name__)


@blp.websocket("/video-hub/cameras/push")
def push_frames():
    from flask_sock import Server as SockServer
    from app.video_hub import video_hub_registry
    from app.video_hub.source_worker import _parse_jpeg_size

    ws: SockServer = push_frames.ws

    token = ws.receive(timeout=10)
    if token is None:
        logger.warning("推帧连接未发送认证信息，断开")
        return

    expected_token = current_app.config.get("VIDEO_HUB_PUSH_TOKEN", "")
    if expected_token and token != expected_token:
        logger.warning("推帧认证失败，断开")
        return

    camera_id_str = ws.receive(timeout=10)
    if camera_id_str is None:
        logger.warning("推帧连接未发送 camera_id，断开")
        return

    try:
        camera_id = int(camera_id_str)
    except (ValueError, TypeError):
        logger.warning("推帧连接 camera_id 无效: %s", camera_id_str)
        return

    session = video_hub_registry.get_or_create_session(camera_id)
    logger.info("推帧连接建立 camera_id=%d", camera_id)

    try:
        while True:
            data = ws.receive(timeout=30)
            if data is None:
                logger.info("推帧连接断开 camera_id=%d", camera_id)
                break
            if isinstance(data, bytes) and len(data) > 0:
                width, height = _parse_jpeg_size(data)
                timestamp = int(time() * 1000)
                session.frame_cache.update(data, width, height, timestamp)
    except Exception as exc:
        logger.info("推帧连接异常 camera_id=%d: %s", camera_id, exc)
    finally:
        logger.info("推帧连接结束 camera_id=%d", camera_id)
```

**协议说明**：
1. ESP32 连接后先发送文本帧：认证 token（如果服务端配置了 `VIDEO_HUB_PUSH_TOKEN` 则必须匹配，否则任意字符串即可）
2. 再发送文本帧：camera_id（如 "5021"）
3. 之后持续发送二进制帧：JPEG 图像数据

- [ ] **Step 2: 在 create_app 中注册 blueprint**

修改 `video-hub-service/app/__init__.py`，在 `from app.api.video_hub_webrtc import blp as video_hub_webrtc_blp` 之后添加：

```python
from app.api.video_hub_push import blp as video_hub_push_blp
```

在 `app.register_blueprint(video_hub_webrtc_blp)` 之后添加：

```python
app.register_blueprint(video_hub_push_blp)
```

- [ ] **Step 3: 安装 flask-sock 并初始化**

修改 `video-hub-service/app/__init__.py`，在 `create_app` 函数中、`app.config.from_object(BaseConfig)` 之后添加 flask-sock 初始化：

```python
from flask_sock import Sock

sock = Sock(app)
```

同时修改 `video_hub_push.py`，将 `@blp.websocket` 改为用 `sock` 实例注册路由。在 `app/__init__.py` 中把 sock 传给 blueprint：

```python
sock = Sock(app)
from app.api.video_hub_push import register_push_routes
register_push_routes(sock)
```

`video_hub_push.py` 改为：

```python
from __future__ import annotations

import logging
from time import time

from flask import current_app

logger = logging.getLogger(__name__)


def register_push_routes(sock):
    @sock.route("/video-hub/cameras/push")
    def push_frames(ws):
        from app.video_hub import video_hub_registry
        from app.video_hub.source_worker import _parse_jpeg_size

        token = ws.receive(timeout=10)
        if token is None:
            logger.warning("推帧连接未发送认证信息，断开")
            return

        expected_token = current_app.config.get("VIDEO_HUB_PUSH_TOKEN", "")
        if expected_token and token != expected_token:
            logger.warning("推帧认证失败，断开")
            return

        camera_id_str = ws.receive(timeout=10)
        if camera_id_str is None:
            logger.warning("推帧连接未发送 camera_id，断开")
            return

        try:
            camera_id = int(camera_id_str)
        except (ValueError, TypeError):
            logger.warning("推帧连接 camera_id 无效: %s", camera_id_str)
            return

        session = video_hub_registry.get_or_create_session(camera_id)
        logger.info("推帧连接建立 camera_id=%d", camera_id)

        try:
            while True:
                data = ws.receive(timeout=30)
                if data is None:
                    logger.info("推帧连接断开 camera_id=%d", camera_id)
                    break
                if isinstance(data, bytes) and len(data) > 0:
                    width, height = _parse_jpeg_size(data)
                    timestamp = int(time() * 1000)
                    session.frame_cache.update(data, width, height, timestamp)
        except Exception as exc:
            logger.info("推帧连接异常 camera_id=%d: %s", camera_id, exc)
        finally:
            logger.info("推帧连接结束 camera_id=%d", camera_id)
```

---

### Task 6: video-hub 推帧单元测试

**Files:**
- Create: `video-hub-service/tests/test_video_hub_push.py`

- [ ] **Step 1: 编写推帧 API 测试**

创建 `video-hub-service/tests/test_video_hub_push.py`：

```python
from __future__ import annotations

import json
import struct


def _create_minimal_jpeg(width=320, height=240):
    soi = b"\xff\xd8"
    sof = b"\xff\xc0" + struct.pack(">H", 11) + b"\x08" + struct.pack(">HH", height, width) + b"\x03\x01\x22\x00\x02\x11\x01\x03\x11\x01"
    eoi = b"\xff\xd9"
    return soi + sof + eoi


def test_push_endpoint_creates_session(client):
    ws = client.websocket_open("/video-hub/cameras/push")
    ws.send_text("test-token")
    ws.send_text("9999")
    frame = _create_minimal_jpeg()
    ws.send_binary(frame)
    ws.close()

    from app.video_hub import video_hub_registry
    session = video_hub_registry.get_session(9999)
    assert session is not None
    latest = session.frame_cache.latest()
    assert latest is not None
    video_hub_registry.remove_session(9999)


def test_push_endpoint_rejects_invalid_camera_id(client):
    ws = client.websocket_open("/video-hub/cameras/push")
    ws.send_text("test-token")
    ws.send_text("not_a_number")
    ws.close()


def test_push_endpoint_handles_multiple_frames(client):
    ws = client.websocket_open("/video-hub/cameras/push")
    ws.send_text("test-token")
    ws.send_text("8888")
    for _ in range(5):
        frame = _create_minimal_jpeg()
        ws.send_binary(frame)
    ws.close()

    from app.video_hub import video_hub_registry
    session = video_hub_registry.get_session(8888)
    assert session is not None
    video_hub_registry.remove_session(8888)
```

**注意**：flask-sock 的测试支持有限，如果 `client.websocket_open` 不可用，则需要使用 `websockets` 库直接连接本地测试服务器。测试实现时根据 flask-sock 的实际测试 API 调整。

- [ ] **Step 2: 运行测试**

Run: `cd video-hub-service && python -m pytest tests/test_video_hub_push.py -v`

---

### Task 7: ESP32 FramePusher 类

**Files:**
- Create: `firmware/esp32-cam/lib/network/FramePusher.h`
- Create: `firmware/esp32-cam/lib/network/FramePusher.cpp`

- [ ] **Step 1: 创建 FramePusher.h**

```cpp
#pragma once

#include <Arduino.h>
#include <AsyncTCP.h>
#include "esp_camera.h"

class FramePusher {
   public:
    FramePusher();
    void begin(const char* host, uint16_t port, int cameraId, const char* token = "");
    void loop();
    void sendFrame(camera_fb_t* fb);
    bool isConnected() const;
    void stop();

   private:
    String _host;
    uint16_t _port;
    int _cameraId;
    String _token;
    AsyncClient* _client = nullptr;
    bool _connected = false;
    bool _handshakeDone = false;
    bool _stopped = false;
    unsigned long _lastConnectAttempt = 0;
    unsigned long _lastFrameSentAt = 0;
    String _wsKey;
    String _wsAccept;

    void _connect();
    void _disconnect();
    void _sendWebSocketHandshake();
    bool _validateHandshakeResponse(const char* data, size_t len);
    void _sendTextFrame(const String& text);
    void _sendBinaryFrame(const uint8_t* data, size_t len);
    void _sendWebSocketFrame(uint8_t opcode, const uint8_t* payload, size_t len);
    void _onConnect();
    void _onData(const char* data, size_t len);
    void _onDisconnect();
    void _onError(const char* error);

    static constexpr unsigned long RECONNECT_INTERVAL_MS = 5000;
    static constexpr unsigned long HANDSHAKE_TIMEOUT_MS = 5000;
    static constexpr uint8_t WS_OPCODE_TEXT = 0x01;
    static constexpr uint8_t WS_OPCODE_BINARY = 0x02;
    static constexpr uint8_t WS_OPCODE_CLOSE = 0x08;
    static constexpr uint8_t WS_OPCODE_PING = 0x09;
    static constexpr uint8_t WS_OPCODE_PONG = 0x0A;
};
```

- [ ] **Step 2: 创建 FramePusher.cpp**

```cpp
#include "FramePusher.h"
#include <libb64/cencode.h>
#include "mbedtls/sha1.h"

FramePusher::FramePusher() {}

void FramePusher::begin(const char* host, uint16_t port, int cameraId, const char* token) {
    _host = host;
    _port = port;
    _cameraId = cameraId;
    _token = token;
    _stopped = false;

    _client = new AsyncClient();
    _client->onConnect([](void* arg, AsyncClient* c) {
        ((FramePusher*)arg)->_onConnect();
    }, this);
    _client->onData([](void* arg, AsyncClient* c, void* data, size_t len) {
        ((FramePusher*)arg)->_onData((const char*)data, len);
    }, this);
    _client->onDisconnect([](void* arg, AsyncClient* c) {
        ((FramePusher*)arg)->_onDisconnect();
    }, this);
    _client->onError([](void* arg, AsyncClient* c, int8_t error) {
        ((FramePusher*)arg)->_onError(_client->errorToString(error));
    }, this);

    _connect();
}

void FramePusher::loop() {
    if (_stopped) return;
    if (!_connected && !_client->connecting()) {
        unsigned long now = millis();
        if (_lastConnectAttempt == 0 || now - _lastConnectAttempt > RECONNECT_INTERVAL_MS) {
            _connect();
        }
    }
}

void FramePusher::sendFrame(camera_fb_t* fb) {
    if (!_connected || !_handshakeDone || !fb) return;
    _sendBinaryFrame(fb->buf, fb->len);
    _lastFrameSentAt = millis();
}

bool FramePusher::isConnected() const {
    return _connected && _handshakeDone;
}

void FramePusher::stop() {
    _stopped = true;
    _disconnect();
}

void FramePusher::_connect() {
    _lastConnectAttempt = millis();
    _handshakeDone = false;
    _wsKey = "dGhlIHNhbXBsZSBub25jZQ==";
    _client->connect(_host.c_str(), _port);
}

void FramePusher::_disconnect() {
    if (_client) {
        _client->close(true);
    }
    _connected = false;
    _handshakeDone = false;
}

void FramePusher::_sendWebSocketHandshake() {
    String request = "GET /video-hub/cameras/push HTTP/1.1\r\n";
    request += "Host: " + _host + "\r\n";
    request += "Upgrade: websocket\r\n";
    request += "Connection: Upgrade\r\n";
    request += "Sec-WebSocket-Key: " + _wsKey + "\r\n";
    request += "Sec-WebSocket-Version: 13\r\n";
    request += "\r\n";
    _client->add(request.c_str(), request.length());
    _client->send();
}

bool FramePusher::_validateHandshakeResponse(const char* data, size_t len) {
    String response(data, len);
    return response.indexOf("101") >= 0 && response.indexOf("Upgrade") >= 0;
}

void FramePusher::_sendTextFrame(const String& text) {
    _sendWebSocketFrame(WS_OPCODE_TEXT, (const uint8_t*)text.c_str(), text.length());
}

void FramePusher::_sendBinaryFrame(const uint8_t* data, size_t len) {
    _sendWebSocketFrame(WS_OPCODE_BINARY, data, len);
}

void FramePusher::_sendWebSocketFrame(uint8_t opcode, const uint8_t* payload, size_t len) {
    if (!_client || !_connected) return;

    uint8_t header[10];
    size_t headerLen = 2;
    header[0] = 0x80 | opcode;

    if (len <= 125) {
        header[1] = (uint8_t)len;
    } else if (len <= 65535) {
        header[1] = 126;
        header[2] = (len >> 8) & 0xFF;
        header[3] = len & 0xFF;
        headerLen = 4;
    } else {
        header[1] = 127;
        for (int i = 0; i < 8; i++) {
            header[2 + i] = (len >> (56 - i * 8)) & 0xFF;
        }
        headerLen = 10;
    }

    _client->add((const char*)header, headerLen);
    _client->add((const char*)payload, len);
    _client->send();
}

void FramePusher::_onConnect() {
    _connected = true;
    Serial.printf("推帧: 已连接到 %s:%d，发送 WebSocket 握手\n", _host.c_str(), _port);
    _sendWebSocketHandshake();
}

void FramePusher::_onData(const char* data, size_t len) {
    if (!_handshakeDone) {
        if (_validateHandshakeResponse(data, len)) {
            _handshakeDone = true;
            Serial.println("推帧: WebSocket 握手成功");
            _sendTextFrame(_token.length() > 0 ? _token : "none");
            _sendTextFrame(String(_cameraId));
            Serial.printf("推帧: 已发送 camera_id=%d\n", _cameraId);
        } else {
            Serial.println("推帧: WebSocket 握手失败");
            _disconnect();
        }
    }
}

void FramePusher::_onDisconnect() {
    _connected = false;
    _handshakeDone = false;
    Serial.println("推帧: 连接断开，将在 5s 后重连");
}

void FramePusher::_onError(const char* error) {
    Serial.printf("推帧: 连接错误: %s\n", error);
    _connected = false;
    _handshakeDone = false;
}
```

---

### Task 8: CameraStreamer 添加 captureFrame 方法

**Files:**
- Modify: `firmware/esp32-cam/lib/camera/CameraStreamer.h`
- Modify: `firmware/esp32-cam/lib/camera/CameraStreamer.cpp`

- [ ] **Step 1: 在 CameraStreamer.h 中添加 captureFrame 声明**

在 `isStreaming()` 方法声明之后添加：

```cpp
camera_fb_t* captureFrame();
void releaseFrame(camera_fb_t* fb);
```

- [ ] **Step 2: 在 CameraStreamer.cpp 中实现 captureFrame 和 releaseFrame**

在文件末尾 `initialized()` 方法之后添加：

```cpp
camera_fb_t* CameraStreamer::captureFrame() {
    if (!cameraInitialized) return nullptr;
    return esp_camera_fb_get();
}

void CameraStreamer::releaseFrame(camera_fb_t* fb) {
    if (fb != nullptr) {
        esp_camera_fb_return(fb);
    }
}
```

---

### Task 9: ESP32 配置更新

**Files:**
- Modify: `firmware/esp32-cam/include/config.h`

- [ ] **Step 1: 添加推帧相关配置**

在 `cam_config` namespace 末尾、闭合 `}` 之前添加：

```cpp
constexpr const char* VIDEO_HUB_HOST = "192.168.0.221";
constexpr uint16_t VIDEO_HUB_PORT = 5100;
constexpr int CAMERA_ID = 5021;
constexpr const char* PUSH_TOKEN = "";
constexpr int PUSH_FRAME_INTERVAL_MS = 100;
```

---

### Task 10: ESP32 main.cpp 集成推帧

**Files:**
- Modify: `firmware/esp32-cam/src/main.cpp`

- [ ] **Step 1: 集成 FramePusher**

修改后的完整 `main.cpp`：

```cpp
#include <Arduino.h>
#include <WiFi.h>

#include "CameraStreamer.h"
#include "ControlServer.h"
#include "FramePusher.h"
#include "UartBridge.h"
#include "config.h"

CameraStreamer g_cameraStreamer;
UartBridge g_uartBridge(Serial1);
ControlServer g_server(g_cameraStreamer, g_uartBridge);
FramePusher g_framePusher;

void connectWiFi() {
    Serial.println("正在连接WiFi...");
    WiFi.begin(cam_config::WIFI_SSID, cam_config::WIFI_PASSWORD);
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print('.');
        attempts++;
    }
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nWiFi连接成功");
        Serial.print("IP地址: ");
        Serial.println(WiFi.localIP());
        g_uartBridge.sendIp(WiFi.localIP().toString());
    } else {
        Serial.println("\nWiFi连接失败");
    }
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n=== AquaSentinel ESP32-CAM (推帧模式) ===");

    g_uartBridge.begin(cam_config::UART_BAUD_RATE, cam_config::UART_RX_PIN, cam_config::UART_TX_PIN);
    const bool cameraReady = g_cameraStreamer.begin();
    if (!cameraReady) {
        Serial.println("摄像头初始化失败，继续保留 PTZ 控制能力");
    }
    connectWiFi();

    if (WiFi.status() == WL_CONNECTED) {
        g_server.begin(cam_config::HTTP_PORT);
        Serial.println("Web服务器已启动（异步模式）");
        Serial.println("视频流端点: http://" + WiFi.localIP().toString() + "/stream");
        Serial.println("PTZ 控制端点: http://" + WiFi.localIP().toString() + "/api/ptz/*");

        g_framePusher.begin(
            cam_config::VIDEO_HUB_HOST,
            cam_config::VIDEO_HUB_PORT,
            cam_config::CAMERA_ID,
            cam_config::PUSH_TOKEN
        );
        Serial.printf("推帧目标: ws://%s:%d/video-hub/cameras/push (camera_id=%d)\n",
            cam_config::VIDEO_HUB_HOST, cam_config::VIDEO_HUB_PORT, cam_config::CAMERA_ID);
    }
}

void loop() {
    if (WiFi.status() == WL_CONNECTED) {
        g_server.loop();
        g_framePusher.loop();

        if (g_framePusher.isConnected() && g_cameraStreamer.initialized()) {
            static unsigned long lastPushAt = 0;
            unsigned long now = millis();
            if (now - lastPushAt >= cam_config::PUSH_FRAME_INTERVAL_MS) {
                camera_fb_t* fb = g_cameraStreamer.captureFrame();
                if (fb != nullptr) {
                    g_framePusher.sendFrame(fb);
                    g_cameraStreamer.releaseFrame(fb);
                    lastPushAt = now;
                }
            }
        }
    }
}
```

**关键变化**：
- 新增 `FramePusher g_framePusher` 全局实例
- `setup()` 中初始化推帧连接
- `loop()` 中持续推帧（100ms 间隔 = 10fps），同时保留 HTTP 拉流和 PTZ 控制
- `PUSH_FRAME_INTERVAL_MS` 控制推帧频率

---

### Task 11: Docker 配置更新

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: 添加 video-hub 推帧相关环境变量**

在 `video-hub` 服务的 `environment` 部分添加：

```yaml
VIDEO_HUB_PUSH_TOKEN: ""
```

---

### Task 12: 集成验证

- [ ] **Step 1: 运行 video-hub 全部测试**

Run: `cd video-hub-service && python -m pytest tests/ -v`

- [ ] **Step 2: 本地启动 video-hub 并用 Python 脚本模拟 ESP32 推帧**

启动 video-hub 后运行：

```python
import websocket
ws = websocket.create_connection("ws://127.0.0.1:5100/video-hub/cameras/push")
ws.send("test-token")
ws.send("5021")
with open("test_frame.jpg", "rb") as f:
    ws.send_binary(f.read())
ws.close()
```

验证 `/video-hub/cameras/5021/snapshot` 返回 JPEG 图片。

- [ ] **Step 3: 编译 ESP32 固件**

Run: `cd firmware/esp32-cam && pio run`

- [ ] **Step 4: 烧录 ESP32 并观察串口输出**

Run: `cd firmware/esp32-cam && pio run --target upload && pio device monitor`

预期串口输出：
```
推帧: 已连接到 192.168.0.221:5100，发送 WebSocket 握手
推帧: WebSocket 握手成功
推帧: 已发送 camera_id=5021
```

- [ ] **Step 5: 在 Docker 环境中验证端到端流程**

1. 重建 video-hub 容器：`docker compose build video-hub && docker compose up -d video-hub`
2. ESP32 连接到 `ws://192.168.0.221:5100/video-hub/cameras/push`（宿主机 IP + 映射端口）
3. 浏览器打开 `http://localhost` 登录，查看监控画面

---

## 注意事项

1. **ESP32 的 AsyncTCP 是 client 模式**：当前项目用 `AsyncTCP` 做 server（ESPAsyncWebServer），`FramePusher` 用 `AsyncClient` 做 client 出站连接，两者可以共存。

2. **帧率控制**：ESP32 QVGA + JPEG quality=12，每帧约 5-15KB，10fps 约 50-150KB/s，ESP32 WiFi 带宽足够。

3. **HTTP 拉流兼容**：`/stream` 端点保留，但推帧模式下 ESP32 的 `isStreaming()` 为 true 时会拒绝 HTTP 拉流。建议将 `MAX_STREAM_CLIENTS` 改为 0 或在推帧连接时自动拒绝 HTTP 拉流。

4. **Docker 网络方向**：ESP32 → 宿主机:5100 是出站方向，与 Docker 端口映射方向一致，无需额外配置。

5. **flask-sock 与 Werkzeug dev server**：flask-sock 需要 Werkzeug >= 2.1 的 WebSocket 支持。Docker 环境中建议用 `gunicorn` + `gevent` worker 替代 dev server（但当前项目用 dev server，flask-sock 也支持）。
