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
    bool moveTo(int targetPan, int targetTilt);
    PtzState state() const;
     bool enterCalibration();
     bool exitCalibration();
     bool saveCalibration();
     bool setCalibrationPulse(bool panAxis, uint16_t pulseUs);
     bool setCalibrationValue(const String& axis, const String& key, uint16_t pulseUs);
     void resetCalibration();
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
     uint8_t tiltMin = 0;
     uint8_t tiltMax = 180;
     bool calibrationMode = false;

    // 默认脉宽范围：500-2500μs 覆盖 0-180 度
    uint16_t panMinUs = 500;
    uint16_t panMaxUs = 2500;
    uint16_t panCenterUs = 1500;
    uint16_t tiltMinUs = 500;
    uint16_t tiltMaxUs = 2500;
    uint16_t tiltCenterUs = 1500;
    uint16_t panCurrentUs = 1500;
    uint16_t tiltCurrentUs = 1500;
    CalibStorage calibStorage;

     static uint8_t clampAngle(int value, int minAngle, int maxAngle);
     static uint16_t angleToPulseUs(uint16_t minUs, uint16_t centerUs, uint16_t maxUs, uint8_t angle);
     static bool isAxisCalibrationValid(uint16_t minUs, uint16_t centerUs, uint16_t maxUs);
     void apply();
};
