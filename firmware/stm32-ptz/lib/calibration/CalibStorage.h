#pragma once

#include <Arduino.h>

struct CalibData {
    uint16_t panMin;
    uint16_t panMax;
    uint16_t panCenter;
    uint16_t tiltMin;
    uint16_t tiltMax;
    uint16_t tiltCenter;
    uint32_t magic;
};

class CalibStorage {
   public:
    static constexpr uint32_t MAGIC = 0xA5A5A5A5;

    bool load(CalibData& outData);
    bool save(const CalibData& data);
};
