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

struct OledFrame {
    char title[OLED_TEXT_BUFFER_SIZE];
    char lines[4][OLED_TEXT_BUFFER_SIZE];
    bool isBootScreen;
};

class OledViewModel {
   public:
    static OledFrame build(const OledDisplayState& state);

   private:
    static const uint32_t BOOT_SCREEN_DURATION_MS = 1800;

    static void buildAngleLine(char* target, const char* label, uint8_t angle);
    static void buildPulseLine(char* target, const char* label, uint16_t pulseUs);
    static void copyText(char* target, const char* text);
};
