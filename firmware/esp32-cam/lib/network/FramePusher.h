#pragma once

#include <Arduino.h>
#include <ArduinoWebsockets.h>
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
    websockets::WebsocketsClient _ws;
    String _host;
    uint16_t _port;
    int _cameraId;
    String _token;
    bool _connected = false;
    bool _stopped = false;
    unsigned long _lastConnectAttempt = 0;

    static constexpr unsigned long RECONNECT_INTERVAL_MS = 5000;
};
