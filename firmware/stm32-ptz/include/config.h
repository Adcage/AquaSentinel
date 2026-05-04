#pragma once

#include <Arduino.h>

namespace ptz_config {

constexpr int UART_BAUD_RATE = 115200;

constexpr uint8_t PIN_SERVO_PAN = PA6;
constexpr uint8_t PIN_SERVO_TILT = PA7;

constexpr uint8_t PAN_MIN_ANGLE = 10;
constexpr uint8_t PAN_MAX_ANGLE = 170;
constexpr uint8_t TILT_MIN_ANGLE = 20;
constexpr uint8_t TILT_MAX_ANGLE = 160;

constexpr uint8_t DEFAULT_PAN_ANGLE = 90;
constexpr uint8_t DEFAULT_TILT_ANGLE = 90;

constexpr uint8_t DEFAULT_NUDGE_STEP = 5;
constexpr uint8_t MAX_NUDGE_STEP = 10;

}  // namespace ptz_config
