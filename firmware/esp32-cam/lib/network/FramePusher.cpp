#include "FramePusher.h"

FramePusher::FramePusher() {}

void FramePusher::begin(const char* host, uint16_t port, int cameraId, const char* token) {
    _host = host;
    _port = port;
    _cameraId = cameraId;
    _token = token ? token : "";
    _stopped = false;
    _connected = false;

    _ws.onEvent([this](websockets::WebsocketsClient& client,
                       websockets::WebsocketsEvent event,
                       String data) {
        switch (event) {
            case websockets::WebsocketsEvent::ConnectionOpened:
                _connected = true;
                Serial.println("推帧WebSocket已连接");
                _ws.send(_token);
                _ws.send(String(_cameraId));
                Serial.printf("已发送认证信息 (camera_id=%d)\n", _cameraId);
                break;

            case websockets::WebsocketsEvent::ConnectionClosed:
                _connected = false;
                Serial.println("推帧WebSocket断开，将自动重连");
                break;

            case websockets::WebsocketsEvent::GotPing:
                break;

            case websockets::WebsocketsEvent::GotPong:
                break;
        }
    });

    _ws.connect(host, port, "/video-hub/cameras/push");
    Serial.printf("推帧目标: ws://%s:%d/video-hub/cameras/push (camera_id=%d)\n",
                  host, port, cameraId);
}

void FramePusher::loop() {
    if (_stopped) return;

    if (!_connected) {
        unsigned long now = millis();
        if (now - _lastConnectAttempt >= RECONNECT_INTERVAL_MS) {
            _lastConnectAttempt = now;
            _ws.connect(_host.c_str(), _port, "/video-hub/cameras/push");
        }
        return;
    }

    _ws.poll();
}

void FramePusher::sendFrame(camera_fb_t* fb) {
    if (!_connected || fb == nullptr) return;
    _ws.sendBinary(reinterpret_cast<const char*>(fb->buf), fb->len);
}

bool FramePusher::isConnected() const {
    return _connected;
}

void FramePusher::stop() {
    _stopped = true;
    _connected = false;
    _ws.close();
}
