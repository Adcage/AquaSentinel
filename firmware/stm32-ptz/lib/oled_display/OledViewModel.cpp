#include "OledViewModel.h"

#include <cstdio>
#include <cstring>

namespace {

void buildLine(char* target, const char* label, int value, const char* suffix) {
    snprintf(target, OLED_TEXT_BUFFER_SIZE, "%s %d%s", label, value, suffix);
}

void buildText(char* target, const char* prefix, const char* value) {
    snprintf(target, OLED_TEXT_BUFFER_SIZE, "%s %s", prefix, value);
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
            buildVoltageLine(frame.lines[0], state.batteryMv, state.batteryValid);
            buildPercentLine(frame.lines[1], state.batteryPercent, state.batteryValid);
            buildRawLine(frame.lines[2], state.batteryRaw, state.batteryValid);
            copyText(frame.lines[3], "SHORT NEXT");
            break;
        case OledPage::Network:
            copyText(frame.title, "网络");
            if (state.espIp[0] != '\0') {
                snprintf(frame.lines[0], OLED_TEXT_BUFFER_SIZE, "IP %s", state.espIp);
                copyText(frame.lines[1], "WIFI OK");
            } else {
                copyText(frame.lines[0], "NO IP");
                copyText(frame.lines[1], "WIFI WAIT");
            }
            copyText(frame.lines[2], "");
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

void OledViewModel::buildVoltageLine(char* target, uint16_t batteryMv, bool valid) {
    if (!valid) {
        copyText(target, "BAT --.--V");
        return;
    }
    const uint16_t centivolts = static_cast<uint16_t>((batteryMv + 5U) / 10U);
    snprintf(target, OLED_TEXT_BUFFER_SIZE, "BAT %u.%02uV", centivolts / 100U, centivolts % 100U);
}

void OledViewModel::buildPercentLine(char* target, uint8_t percent, bool valid) {
    if (!valid) {
        copyText(target, "PCT --%");
        return;
    }
    snprintf(target, OLED_TEXT_BUFFER_SIZE, "PCT %u%%", percent);
}

void OledViewModel::buildRawLine(char* target, uint16_t raw, bool valid) {
    if (!valid) {
        copyText(target, "RAW ----");
        return;
    }
    buildLine(target, "RAW", raw, "");
}

void OledViewModel::copyText(char* target, const char* text) {
    strncpy(target, text, OLED_TEXT_BUFFER_SIZE - 1);
    target[OLED_TEXT_BUFFER_SIZE - 1] = '\0';
}
