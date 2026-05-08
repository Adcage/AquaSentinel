#include "OledViewModel.h"

#include <cstdio>
#include <cstring>

namespace {

void buildLine(char* target, const char* label, int value, const char* suffix) {
    snprintf(target, OLED_TEXT_BUFFER_SIZE, "%s %d%s", label, value, suffix);
}

}  // namespace

OledFrame OledViewModel::build(const OledUiState& state) {
    OledFrame frame{};
    frame.isBootScreen = state.uptimeMs < BOOT_SCREEN_DURATION_MS;

    if (frame.isBootScreen) {
        copyText(frame.title, "AQUASENTINEL");
        copyText(frame.lines[0], "启动中");
        copyText(frame.lines[1], "OLED READY");
        buildAngleLine(frame.lines[2], "PAN", state.pan);
        buildAngleLine(frame.lines[3], "TILT", state.tilt);
        return frame;
    }

    if (state.showActionMessage) {
        copyText(frame.title, "云台");
        copyText(frame.lines[0], state.actionMessage);
        buildAngleLine(frame.lines[1], "PAN", state.pan);
        buildAngleLine(frame.lines[2], "TILT", state.tilt);
        copyText(frame.lines[3], "UART READY");
        return frame;
    }

    switch (state.page) {
        case OledPage::Status:
            copyText(frame.title, "云台");
            copyText(frame.lines[0], "MODE NORM");
            buildAngleLine(frame.lines[1], "PAN", state.pan);
            buildAngleLine(frame.lines[2], "TILT", state.tilt);
            copyText(frame.lines[3], "UART READY");
            break;
        case OledPage::Calibration:
            copyText(frame.title, "校准");
            copyText(frame.lines[0], state.calibrationMode ? "MODE CAL" : "CAL READY");
            buildPulseLine(frame.lines[1], "PAN", state.panPulseUs);
            buildPulseLine(frame.lines[2], "TILT", state.tiltPulseUs);
            copyText(frame.lines[3], "WEB CAL");
            break;
        case OledPage::Battery:
            copyText(frame.title, "电量");
            copyText(frame.lines[0], "ADC WAIT");
            copyText(frame.lines[1], "PA0 WAIT");
            copyText(frame.lines[2], "BAT WAIT");
            copyText(frame.lines[3], "SHORT NEXT");
            break;
    }

    return frame;
}

void OledViewModel::buildAngleLine(char* target, const char* label, uint8_t angle) {
    buildLine(target, label, angle, "");
}

void OledViewModel::buildPulseLine(char* target, const char* label, uint16_t pulseUs) {
    buildLine(target, label, pulseUs, "US");
}

void OledViewModel::copyText(char* target, const char* text) {
    strncpy(target, text, OLED_TEXT_BUFFER_SIZE - 1);
    target[OLED_TEXT_BUFFER_SIZE - 1] = '\0';
}
