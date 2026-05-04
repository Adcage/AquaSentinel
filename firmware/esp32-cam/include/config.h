#pragma once

#include <Arduino.h>

namespace cam_config {

constexpr const char* WIFI_SSID = "ADCAGE";
constexpr const char* WIFI_PASSWORD = "12345678";

constexpr int UART_BAUD_RATE = 115200;
constexpr int UART_RX_PIN = 14;
constexpr int UART_TX_PIN = 13;

constexpr framesize_t FRAME_SIZE = FRAMESIZE_SVGA;
constexpr int JPEG_QUALITY = 4;
constexpr int FRAME_INTERVAL_MS = 33;

constexpr bool FLIP_VERTICAL = true;
constexpr bool FLIP_HORIZONTAL = true;

constexpr int HTTP_PORT = 80;

}  // namespace cam_config
