#pragma once

#include <Arduino.h>

namespace ptz_config {

constexpr int UART_BAUD_RATE = 115200;

constexpr uint8_t PIN_SERVO_PAN = PA6;
constexpr uint8_t PIN_SERVO_TILT = PA7;

constexpr uint8_t PAN_MIN_ANGLE = 0;
constexpr uint8_t PAN_MAX_ANGLE = 180;

// TILT 角度定义：0=仰视（向上），90=平视（水平），180=俯视（向下）
// 用户输入直接对应舵机物理角度，OFFSET 用于微调安装偏差
constexpr int8_t TILT_MIN_ANGLE = 0;
constexpr uint8_t TILT_MAX_ANGLE = 180;

constexpr uint8_t DEFAULT_PAN_ANGLE = 90;
constexpr uint8_t DEFAULT_TILT_ANGLE = 90;

// TILT 角度映射偏移：逻辑角度 + 偏移 = 物理角度
// 设为 0 表示用户输入 = 舵机物理角度；校准后可微调
constexpr int8_t TILT_ANGLE_OFFSET = 0;

constexpr uint8_t DEFAULT_NUDGE_STEP = 5;
constexpr uint8_t MAX_NUDGE_STEP = 10;

// PAN 校准安全脉宽上限：超过该值时，当前机构已接近物理极限，继续推动可能导致堵转或串口异常。
constexpr uint16_t PAN_CALIBRATION_SAFE_MAX_US = 2350;

}  // namespace ptz_config
