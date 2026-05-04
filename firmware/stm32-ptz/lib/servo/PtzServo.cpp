#include "PtzServo.h"

#include "config.h"

uint8_t PtzServo::clampAngle(int value, uint8_t minAngle, uint8_t maxAngle) {
    if (value < static_cast<int>(minAngle)) {
        return minAngle;
    }
    if (value > static_cast<int>(maxAngle)) {
        return maxAngle;
    }
    return static_cast<uint8_t>(value);
}

void PtzServo::begin(uint8_t panPin, uint8_t tiltPin) {
    panMin = ptz_config::PAN_MIN_ANGLE;
    panMax = ptz_config::PAN_MAX_ANGLE;
    tiltMin = ptz_config::TILT_MIN_ANGLE;
    tiltMax = ptz_config::TILT_MAX_ANGLE;

    panServo.attach(panPin);
    tiltServo.attach(tiltPin);

    CalibData loaded{};
    if (calibStorage.load(loaded)) {
        panMinUs = loaded.panMin;
        panMaxUs = loaded.panMax;
        panCenterUs = loaded.panCenter;
        tiltMinUs = loaded.tiltMin;
        tiltMaxUs = loaded.tiltMax;
        tiltCenterUs = loaded.tiltCenter;
    }

    home();
}

void PtzServo::home() {
    panAngle = ptz_config::DEFAULT_PAN_ANGLE;
    tiltAngle = ptz_config::DEFAULT_TILT_ANGLE;
    panCurrentUs = panCenterUs;
    tiltCurrentUs = tiltCenterUs;
    apply();
}

bool PtzServo::nudge(const String& dir, uint8_t step) {
    if (calibrationMode) {
        return false;
    }
    int panTarget = panAngle;
    int tiltTarget = tiltAngle;

    if (dir == "LEFT") {
        panTarget -= step;
    } else if (dir == "RIGHT") {
        panTarget += step;
    } else if (dir == "UP") {
        tiltTarget -= step;
    } else if (dir == "DOWN") {
        tiltTarget += step;
    } else {
        return false;
    }

    panAngle = clampAngle(panTarget, panMin, panMax);
    tiltAngle = clampAngle(tiltTarget, tiltMin, tiltMax);
    panCurrentUs = panMinUs + static_cast<uint16_t>((panAngle * (panMaxUs - panMinUs)) / 180);
    tiltCurrentUs = tiltMinUs + static_cast<uint16_t>((tiltAngle * (tiltMaxUs - tiltMinUs)) / 180);
    apply();
    return true;
}

PtzState PtzServo::state() const { return {panAngle, tiltAngle}; }

bool PtzServo::enterCalibration() {
    calibrationMode = true;
    return true;
}

bool PtzServo::exitCalibration() {
    calibrationMode = false;
    home();
    return true;
}

bool PtzServo::saveCalibration() {
    CalibData data{};
    data.panMin = panMinUs;
    data.panMax = panMaxUs;
    data.panCenter = panCenterUs;
    data.tiltMin = tiltMinUs;
    data.tiltMax = tiltMaxUs;
    data.tiltCenter = tiltCenterUs;
    data.magic = CalibStorage::MAGIC;
    return calibStorage.save(data);
}

bool PtzServo::setCalibrationPulse(bool panAxis, uint16_t pulseUs) {
    if (!calibrationMode || pulseUs < 500 || pulseUs > 2500) {
        return false;
    }
    if (panAxis) {
        panCurrentUs = pulseUs;
        panServo.writeMicroseconds(static_cast<int>(pulseUs));

        if (pulseUs < panMinUs) {
            panMinUs = pulseUs;
        }
        if (pulseUs > panMaxUs) {
            panMaxUs = pulseUs;
        }
        panCenterUs = panCurrentUs;
    } else {
        tiltCurrentUs = pulseUs;
        tiltServo.writeMicroseconds(static_cast<int>(pulseUs));

        if (pulseUs < tiltMinUs) {
            tiltMinUs = pulseUs;
        }
        if (pulseUs > tiltMaxUs) {
            tiltMaxUs = pulseUs;
        }
        tiltCenterUs = tiltCurrentUs;
    }
    return true;
}

bool PtzServo::isCalibrationMode() const { return calibrationMode; }
uint16_t PtzServo::currentPanPulseUs() const { return panCurrentUs; }
uint16_t PtzServo::currentTiltPulseUs() const { return tiltCurrentUs; }
uint16_t PtzServo::panMinPulseUs() const { return panMinUs; }
uint16_t PtzServo::panMaxPulseUs() const { return panMaxUs; }
uint16_t PtzServo::panCenterPulseUs() const { return panCenterUs; }
uint16_t PtzServo::tiltMinPulseUs() const { return tiltMinUs; }
uint16_t PtzServo::tiltMaxPulseUs() const { return tiltMaxUs; }
uint16_t PtzServo::tiltCenterPulseUs() const { return tiltCenterUs; }

void PtzServo::apply() {
    if (calibrationMode) {
        panServo.writeMicroseconds(static_cast<int>(panCurrentUs));
        tiltServo.writeMicroseconds(static_cast<int>(tiltCurrentUs));
        return;
    }
    panServo.write(panAngle);
    tiltServo.write(tiltAngle);
}
