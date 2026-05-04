#pragma once

#include <Arduino.h>
#include <Servo.h>

#include "CalibStorage.h"

struct PtzState {
    uint8_t pan;
    uint8_t tilt;
};

class PtzServo {
   public:
    void begin(uint8_t panPin, uint8_t tiltPin);
    void home();
    bool nudge(const String& dir, uint8_t step);
    PtzState state() const;
    bool enterCalibration();
    bool exitCalibration();
    bool saveCalibration();
    bool setCalibrationPulse(bool panAxis, uint16_t pulseUs);
    bool isCalibrationMode() const;
    uint16_t currentPanPulseUs() const;
    uint16_t currentTiltPulseUs() const;
    uint16_t panMinPulseUs() const;
    uint16_t panMaxPulseUs() const;
    uint16_t panCenterPulseUs() const;
    uint16_t tiltMinPulseUs() const;
    uint16_t tiltMaxPulseUs() const;
    uint16_t tiltCenterPulseUs() const;

   private:
    Servo panServo;
    Servo tiltServo;

    uint8_t panAngle = 90;
    uint8_t tiltAngle = 90;

    uint8_t panMin = 10;
    uint8_t panMax = 170;
    uint8_t tiltMin = 20;
    uint8_t tiltMax = 160;
    bool calibrationMode = false;

    uint16_t panMinUs = 1000;
    uint16_t panMaxUs = 2000;
    uint16_t panCenterUs = 1500;
    uint16_t tiltMinUs = 1000;
    uint16_t tiltMaxUs = 2000;
    uint16_t tiltCenterUs = 1500;
    uint16_t panCurrentUs = 1500;
    uint16_t tiltCurrentUs = 1500;
    CalibStorage calibStorage;

    static uint8_t clampAngle(int value, uint8_t minAngle, uint8_t maxAngle);
    void apply();
};
