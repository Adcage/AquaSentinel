#include <Arduino.h>

#include <string.h>

#include "BatteryMonitor.h"
#include "ButtonHandler.h"
#include "OledDisplay.h"
#include "PtzServo.h"
#include "UartHandler.h"
#include "config.h"

PtzServo g_servo;
UartHandler g_uartHandler(g_servo);
OledDisplay g_oledDisplay;
ButtonHandler g_buttonHandler;
BatteryMonitor g_batteryMonitor;
OledPage g_currentPage = OledPage::Status;
uint32_t g_actionMessageUntilMs = 0;
char g_actionMessage[OLED_TEXT_BUFFER_SIZE] = {0};

namespace {

OledPage nextPage(OledPage page) {
    switch (page) {
        case OledPage::Status:
            return OledPage::Calibration;
        case OledPage::Calibration:
            return OledPage::Battery;
        case OledPage::Battery:
            return OledPage::Status;
    }
    return OledPage::Status;
}

void setActionMessage(const char* message, uint32_t nowMs) {
    strncpy(g_actionMessage, message, OLED_TEXT_BUFFER_SIZE - 1);
    g_actionMessage[OLED_TEXT_BUFFER_SIZE - 1] = '\0';
    g_actionMessageUntilMs = nowMs + ptz_config::OLED_ACTION_MESSAGE_MS;
}

OledUiState buildUiState(uint32_t nowMs) {
    const PtzState servoState = g_servo.state();
    const BatteryReading batteryReading = g_batteryMonitor.reading();

    OledUiState state{};
    state.page = g_currentPage;
    state.pan = servoState.pan;
    state.tilt = servoState.tilt;
    state.calibrationMode = g_servo.isCalibrationMode();
    state.panPulseUs = g_servo.currentPanPulseUs();
    state.tiltPulseUs = g_servo.currentTiltPulseUs();
    state.uptimeMs = nowMs;
    state.showActionMessage = nowMs < g_actionMessageUntilMs;
    if (state.showActionMessage) {
        strncpy(state.actionMessage, g_actionMessage, OLED_TEXT_BUFFER_SIZE - 1);
        state.actionMessage[OLED_TEXT_BUFFER_SIZE - 1] = '\0';
    }
    state.batteryRaw = batteryReading.raw;
    state.batteryMv = batteryReading.batteryMv;
    state.batteryPercent = batteryReading.percent;
    state.batteryValid = batteryReading.valid;

    return state;
}

}  // namespace

void setup() {
    // Blue Pill 上 USART1 对应 PA9/PA10（即本项目与 ESP32 通信的引脚）
    Serial.begin(ptz_config::UART_BAUD_RATE);
    pinMode(ptz_config::PIN_BUTTON_USER, INPUT_PULLUP);
    g_buttonHandler.begin();
    g_batteryMonitor.begin();
    g_servo.begin(ptz_config::PIN_SERVO_PAN, ptz_config::PIN_SERVO_TILT);
    g_uartHandler.begin(Serial);
    g_oledDisplay.begin();
    Serial.println("STM32 PTZ controller ready");
}

void loop() {
    const uint32_t nowMs = millis();
    g_uartHandler.poll();
    g_batteryMonitor.update(nowMs);

    const bool rawPressed = digitalRead(ptz_config::PIN_BUTTON_USER) == LOW;
    const ButtonEvent event = g_buttonHandler.update(rawPressed, nowMs);
    if (event == ButtonEvent::ShortPress) {
        g_currentPage = nextPage(g_currentPage);
        g_actionMessageUntilMs = 0;
        g_actionMessage[0] = '\0';
    } else if (event == ButtonEvent::LongPress) {
        g_servo.home();
        setActionMessage("正在回中", nowMs);
    }

    g_oledDisplay.update(buildUiState(nowMs));
    delay(2);
}
