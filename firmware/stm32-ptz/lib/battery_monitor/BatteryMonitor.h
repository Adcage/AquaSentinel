#pragma once

#include <cstdint>

struct BatteryReading {
    uint16_t raw = 0;
    uint16_t batteryMv = 0;
    uint8_t percent = 0;
    bool valid = false;
};

class BatteryMonitor {
   public:
    void begin();
    bool update(uint32_t nowMs);
    void reset();
    void ingestRawSample(uint16_t rawSample);
    BatteryReading reading() const;

    static uint16_t rawToBatteryMv(uint16_t raw);
    static uint8_t batteryMvToPercent(uint16_t batteryMv);

   private:
    BatteryReading currentReading;
    uint16_t samples[8] = {0};
    uint8_t sampleCount = 0;
    uint8_t sampleIndex = 0;
    uint32_t lastSampleMs = 0;
};
