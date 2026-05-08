#pragma once

#include <stdint.h>

#include "OledViewModel.h"

class PtzServo;

class OledDisplay {
   public:
    void begin();
    void update(const PtzServo& servo, uint32_t uptimeMs);

   private:
    static const uint32_t REFRESH_INTERVAL_MS = 120;

    bool initialized = false;
    uint32_t lastRenderMs = 0;
    bool hasLastState = false;
    OledDisplayState lastState;

    static OledDisplayState captureState(const PtzServo& servo, uint32_t uptimeMs);
    static bool hasStateChanged(const OledDisplayState& current, const OledDisplayState& previous);
    void drawFrame(const OledFrame& frame);
};
