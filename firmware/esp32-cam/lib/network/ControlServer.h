#pragma once

#include <WebServer.h>

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
    WebServer server;

    void setupRoutes();
};
