#pragma once

#include <Arduino.h>

namespace cam_config {

constexpr const char* WIFI_SSID = "ADCAGE";
constexpr const char* WIFI_PASSWORD = "12345678";

constexpr int UART_BAUD_RATE = 115200;
constexpr int UART_RX_PIN = 14;
constexpr int UART_TX_PIN = 13;

constexpr framesize_t FRAME_SIZE = FRAMESIZE_QVGA;
constexpr int JPEG_QUALITY = 12;
constexpr int FRAME_INTERVAL_MS = 120;
constexpr int MAX_STREAM_CLIENTS = 1;
constexpr unsigned long STREAM_CLIENT_TIMEOUT_MS = 30000;

constexpr bool FLIP_VERTICAL = true;
constexpr bool FLIP_HORIZONTAL = true;

constexpr int HTTP_PORT = 80;

constexpr const char* VIDEO_HUB_HOST = "192.168.0.221";
constexpr uint16_t VIDEO_HUB_PORT = 5100;
constexpr int CAMERA_ID = 5021;
constexpr const char* PUSH_TOKEN = "";
constexpr int PUSH_FRAME_INTERVAL_MS = 100;

}  // namespace cam_config
