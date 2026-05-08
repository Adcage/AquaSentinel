#include "BatteryMonitor.h"

#include <cstddef>

#ifdef ARDUINO
#include <Arduino.h>
#endif

#include "config.h"

namespace {

struct BatteryPercentPoint {
    uint16_t mv;
    uint8_t percent;
};

const BatteryPercentPoint BATTERY_PERCENT_POINTS[] = {
    {3300, 0},
    {3400, 5},
    {3550, 15},
    {3700, 35},
    {3850, 60},
    {4000, 80},
    {4200, 100},
};

#ifdef ARDUINO
uint16_t readSettledRawSample() {
    (void)analogRead(ptz_config::PIN_BATTERY_ADC);
    delayMicroseconds(80);

    uint32_t total = 0;
    static const uint8_t SAMPLE_COUNT = 6;
    for (uint8_t index = 0; index < SAMPLE_COUNT; ++index) {
        total += analogRead(ptz_config::PIN_BATTERY_ADC);
        delayMicroseconds(80);
    }

    return static_cast<uint16_t>((total + SAMPLE_COUNT / 2U) / SAMPLE_COUNT);
}
#endif

}  // namespace

void BatteryMonitor::begin() {
#ifdef ARDUINO
    analogReadResolution(12);
    pinMode(ptz_config::PIN_BATTERY_ADC, INPUT_ANALOG);
#endif
    reset();
}

bool BatteryMonitor::update(uint32_t nowMs) {
#ifndef ARDUINO
    (void)nowMs;
    return false;
#else
    if (sampleCount > 0 && (nowMs - lastSampleMs) < ptz_config::BATTERY_SAMPLE_INTERVAL_MS) {
        return false;
    }

    lastSampleMs = nowMs;
    ingestRawSample(readSettledRawSample());
    return true;
#endif
}

void BatteryMonitor::reset() {
    currentReading = BatteryReading{};
    sampleCount = 0;
    sampleIndex = 0;
    lastSampleMs = 0;
    for (uint8_t index = 0; index < ptz_config::BATTERY_SAMPLE_WINDOW; ++index) {
        samples[index] = 0;
    }
}

void BatteryMonitor::ingestRawSample(uint16_t rawSample) {
    samples[sampleIndex] = rawSample;
    sampleIndex = static_cast<uint8_t>((sampleIndex + 1) % ptz_config::BATTERY_SAMPLE_WINDOW);
    if (sampleCount < ptz_config::BATTERY_SAMPLE_WINDOW) {
        ++sampleCount;
    }

    uint32_t total = 0;
    for (uint8_t index = 0; index < sampleCount; ++index) {
        total += samples[index];
    }

    currentReading.raw = static_cast<uint16_t>(total / sampleCount);
    currentReading.batteryMv = rawToBatteryMv(currentReading.raw);
    currentReading.percent = batteryMvToPercent(currentReading.batteryMv);
    currentReading.valid = true;
}

BatteryReading BatteryMonitor::reading() const {
    return currentReading;
}

uint16_t BatteryMonitor::rawToBatteryMv(uint16_t raw) {
    return static_cast<uint16_t>((static_cast<uint32_t>(raw) * 3300UL * 2UL + 2048UL) / 4096UL);
}

uint8_t BatteryMonitor::batteryMvToPercent(uint16_t batteryMv) {
    if (batteryMv <= BATTERY_PERCENT_POINTS[0].mv) {
        return BATTERY_PERCENT_POINTS[0].percent;
    }

    const size_t lastIndex = sizeof(BATTERY_PERCENT_POINTS) / sizeof(BATTERY_PERCENT_POINTS[0]) - 1;
    if (batteryMv >= BATTERY_PERCENT_POINTS[lastIndex].mv) {
        return BATTERY_PERCENT_POINTS[lastIndex].percent;
    }

    for (size_t index = 1; index <= lastIndex; ++index) {
        const BatteryPercentPoint lower = BATTERY_PERCENT_POINTS[index - 1];
        const BatteryPercentPoint upper = BATTERY_PERCENT_POINTS[index];
        if (batteryMv <= upper.mv) {
            const uint32_t mvRange = upper.mv - lower.mv;
            const uint32_t percentRange = upper.percent - lower.percent;
            const uint32_t mvOffset = batteryMv - lower.mv;
            return static_cast<uint8_t>(lower.percent + (mvOffset * percentRange + mvRange / 2U) / mvRange);
        }
    }

    return BATTERY_PERCENT_POINTS[lastIndex].percent;
}
