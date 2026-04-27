#include <Arduino.h>

// Blue Pill 板载 LED 在 PC13 引脚
// LED 为低电平点亮
#define LED_PIN PC13

void setup() {
  pinMode(LED_PIN, OUTPUT);
}

void loop() {
  digitalWrite(LED_PIN, LOW);  // 开灯
  delay(500);
  digitalWrite(LED_PIN, HIGH); // 关灯
  delay(500);
}