#pragma once

#include <ESPAsyncWebServer.h>

#include "CameraStreamer.h"
#include "UartBridge.h"

class ControlServer {
   public:
    ControlServer(CameraStreamer& cameraRef, UartBridge& bridgeRef);
    void begin(int port);
    void loop();

   private:
    CameraStreamer& camera;
    UartBridge& bridge;
    AsyncWebServer server;

    void setupRoutes();
};