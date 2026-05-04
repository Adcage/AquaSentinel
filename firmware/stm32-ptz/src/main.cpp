#include <Arduino.h>

#include "PtzServo.h"
#include "UartHandler.h"
#include "config.h"

PtzServo g_servo;
UartHandler g_uartHandler(g_servo);

void setup() {
    // Blue Pill 上 USART1 对应 PA9/PA10（即本项目与 ESP32 通信的引脚）
    Serial.begin(ptz_config::UART_BAUD_RATE);
    g_servo.begin(ptz_config::PIN_SERVO_PAN, ptz_config::PIN_SERVO_TILT);
    g_uartHandler.begin(Serial);
    Serial.println("STM32 PTZ controller ready");
}

void loop() {
    g_uartHandler.poll();
    delay(2);
}
