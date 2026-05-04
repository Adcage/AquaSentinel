#include <Arduino.h>
#include <WiFi.h>

#include "CameraStreamer.h"
#include "ControlServer.h"
#include "UartBridge.h"
#include "config.h"

CameraStreamer g_cameraStreamer;
UartBridge g_uartBridge(Serial1);
ControlServer g_server(g_cameraStreamer, g_uartBridge);

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
    } else {
        Serial.println("\nWiFi连接失败");
    }
}

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n=== AquaSentinel ESP32-CAM 视频与控制测试 ===");

    g_uartBridge.begin(cam_config::UART_BAUD_RATE, cam_config::UART_RX_PIN, cam_config::UART_TX_PIN);
    g_cameraStreamer.begin();
    connectWiFi();

    if (WiFi.status() == WL_CONNECTED) {
        g_server.begin(cam_config::HTTP_PORT);
        Serial.println("Web服务器已启动");
        Serial.println("访问 http://" + WiFi.localIP().toString() + " 查看视频流");
    }
}

void loop() {
    if (WiFi.status() == WL_CONNECTED) {
        g_server.loop();
    }
    delay(1);
}
