#pragma once

#include <Arduino.h>
#include <WebServer.h>
#include "esp_camera.h"

class CameraStreamer {
   public:
    bool begin();
    void handleStream(WebServer& server);
    String statusJson() const;
    bool initialized() const;

   private:
    bool cameraInitialized = false;
    unsigned long lastFrameTime = 0;
};
