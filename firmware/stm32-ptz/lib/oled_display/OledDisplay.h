#pragma once

#include <stdint.h>

#include "OledViewModel.h"

class PtzServo;

class OledDisplay {
   public:
    void begin();
    void update(const OledUiState& state);

   private:
    static const uint32_t REFRESH_INTERVAL_MS = 120;

    bool initialized = false;
    uint32_t lastRenderMs = 0;
    bool hasLastState = false;
    OledUiState lastState;

    static bool hasStateChanged(const OledUiState& current, const OledUiState& previous);
    void drawFrame(const OledFrame& frame);
};
