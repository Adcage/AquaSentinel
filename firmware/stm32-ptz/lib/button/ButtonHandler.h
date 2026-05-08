#pragma once

#include <cstdint>

enum class ButtonEvent : uint8_t {
    None = 0,
    ShortPress,
    LongPress,
};

class ButtonHandler {
   public:
    void begin();
    ButtonEvent update(bool rawPressed, uint32_t nowMs);
    void reset();

   private:
    bool stablePressed = false;
    bool lastRawPressed = false;
    bool longPressFired = false;
    uint32_t lastBounceMs = 0;
    uint32_t pressStartMs = 0;
};
