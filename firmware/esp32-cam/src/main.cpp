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
