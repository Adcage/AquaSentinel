#pragma once

#include <cstddef>
#include <cstdint>

constexpr size_t OLED_TEXT_BUFFER_SIZE = 20;

struct OledDisplayState {
    uint8_t pan = 0;
    uint8_t tilt = 0;
    bool calibrationMode = false;
    uint16_t panPulseUs = 1500;
    uint16_t tiltPulseUs = 1500;
    uint32_t uptimeMs = 0;
};

enum class OledPage : uint8_t {
    Status = 0,
    Calibration = 1,
    Battery = 2,
};

struct OledUiState {
    OledPage page = OledPage::Status;
    uint8_t pan = 0;
    uint8_t tilt = 0;
    bool calibrationMode = false;
    uint16_t panPulseUs = 1500;
    uint16_t tiltPulseUs = 1500;
    uint32_t uptimeMs = 0;
    bool showActionMessage = false;
    char actionMessage[OLED_TEXT_BUFFER_SIZE] = {0};
    uint16_t batteryRaw = 0;
    uint16_t batteryMv = 0;
    uint8_t batteryPercent = 0;
    bool batteryValid = false;
};

struct OledFrame {
    char title[OLED_TEXT_BUFFER_SIZE];
    char lines[4][OLED_TEXT_BUFFER_SIZE];
    bool isBootScreen;
};

class OledViewModel {
   public:
    static OledFrame build(const OledUiState& state);

   private:
    static const uint32_t BOOT_SCREEN_DURATION_MS = 1800;

    static void buildAngleLine(char* target, const char* label, uint8_t angle);
    static void buildPulseLine(char* target, const char* label, uint16_t pulseUs);
    static void buildVoltageLine(char* target, uint16_t batteryMv, bool valid);
    static void buildPercentLine(char* target, uint8_t percent, bool valid);
    static void buildRawLine(char* target, uint16_t raw, bool valid);
    static void copyText(char* target, const char* text);
};
