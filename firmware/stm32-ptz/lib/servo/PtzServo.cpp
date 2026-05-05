#include "PtzServo.h"

#include "config.h"

uint16_t PtzServo::angleToPulseUs(uint16_t minUs, uint16_t centerUs, uint16_t maxUs, uint8_t angle) {
    if (angle <= 90) {
        return static_cast<uint16_t>(static_cast<int32_t>(minUs) +
                                     (static_cast<int32_t>(angle) *
                                      (static_cast<int32_t>(centerUs) - static_cast<int32_t>(minUs))) /
                                             90);
    }
    return static_cast<uint16_t>(static_cast<int32_t>(centerUs) +
                                 (static_cast<int32_t>(angle - 90) *
                                  (static_cast<int32_t>(maxUs) - static_cast<int32_t>(centerUs))) /
                                         90);
}

bool PtzServo::isAxisCalibrationValid(uint16_t minUs, uint16_t centerUs, uint16_t maxUs) {
    return (minUs <= centerUs && centerUs <= maxUs) || (maxUs <= centerUs && centerUs <= minUs);
}

uint8_t PtzServo::clampAngle(int value, int minAngle, int maxAngle) {
    if (value < minAngle) {
        return static_cast<uint8_t>(minAngle);
    }
    if (value > maxAngle) {
        return static_cast<uint8_t>(maxAngle);
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
    // PAN 镜像：用户角度 → 内部角度
    panAngle = clampAngle(180 - ptz_config::DEFAULT_PAN_ANGLE, panMin, panMax);
    // TILT 偏移：逻辑角度 → 物理角度
    tiltAngle = clampAngle(
        ptz_config::DEFAULT_TILT_ANGLE + ptz_config::TILT_ANGLE_OFFSET,
        ptz_config::TILT_MIN_ANGLE + ptz_config::TILT_ANGLE_OFFSET,
        ptz_config::TILT_MAX_ANGLE + ptz_config::TILT_ANGLE_OFFSET);
    panCurrentUs = angleToPulseUs(panMinUs, panCenterUs, panMaxUs, panAngle);
    tiltCurrentUs = angleToPulseUs(tiltMinUs, tiltCenterUs, tiltMaxUs, tiltAngle);
    apply();
}

bool PtzServo::nudge(const String& dir, uint8_t step) {
    if (calibrationMode) {
        return false;
    }
    int panTarget = panAngle;
    int logicalTiltTarget = static_cast<int>(tiltAngle) - ptz_config::TILT_ANGLE_OFFSET;

    // LEFT/RIGHT：交换方向
    if (dir == "LEFT") {
        panTarget += step;  // 向左转 = 增加角度
    } else if (dir == "RIGHT") {
        panTarget -= step;  // 向右转 = 减小角度
    } else if (dir == "UP") {
        // UP = 仰视 = TILT 角度减小（往0方向）
        logicalTiltTarget -= step;
    } else if (dir == "DOWN") {
        // DOWN = 俯视 = TILT 角度增加（往180方向）
        logicalTiltTarget += step;
    } else {
        return false;
    }

    panAngle = clampAngle(panTarget, panMin, panMax);
    // TILT 使用配置范围：0=仰视，90=平视，180=俯视
    int tiltMinVal = ptz_config::TILT_MIN_ANGLE;
    int tiltMaxVal = ptz_config::TILT_MAX_ANGLE;
    const int logicalTilt = clampAngle(logicalTiltTarget, tiltMinVal, tiltMaxVal);
    tiltAngle = clampAngle(
        logicalTilt + ptz_config::TILT_ANGLE_OFFSET,
        ptz_config::TILT_MIN_ANGLE + ptz_config::TILT_ANGLE_OFFSET,
        ptz_config::TILT_MAX_ANGLE + ptz_config::TILT_ANGLE_OFFSET);
    panCurrentUs = angleToPulseUs(panMinUs, panCenterUs, panMaxUs, panAngle);
    tiltCurrentUs = angleToPulseUs(tiltMinUs, tiltCenterUs, tiltMaxUs, tiltAngle);
    apply();
    return true;
}

bool PtzServo::moveTo(int targetPan, int targetTilt) {
    if (calibrationMode) {
        return false;
    }
    // PAN 镜像映射：用户 0=最左，180=最右 → 内部 180=最左，0=最右
    panAngle = clampAngle(180 - targetPan, panMin, panMax);

    // TILT 角度映射：逻辑角度 + 偏移 = 物理角度
    // 用户定义：0=仰视，90=平视，180=俯视
    int physicalTilt = targetTilt + ptz_config::TILT_ANGLE_OFFSET;
    int tiltMinVal = ptz_config::TILT_MIN_ANGLE + ptz_config::TILT_ANGLE_OFFSET;
    int tiltMaxVal = ptz_config::TILT_MAX_ANGLE + ptz_config::TILT_ANGLE_OFFSET;
    tiltAngle = clampAngle(physicalTilt, tiltMinVal, tiltMaxVal);

    panCurrentUs = angleToPulseUs(panMinUs, panCenterUs, panMaxUs, panAngle);
    tiltCurrentUs = angleToPulseUs(tiltMinUs, tiltCenterUs, tiltMaxUs, tiltAngle);
    apply();
    return true;
}

PtzState PtzServo::state() const { 
    // PAN 镜像反映射：内部角度 → 用户角度
    int logicalPan = 180 - static_cast<int>(panAngle);
    if (logicalPan < 0) logicalPan = 0;
    if (logicalPan > 180) logicalPan = 180;

    // TILT 反映射：物理角度 → 逻辑角度
    int logicalTilt = static_cast<int>(tiltAngle) - ptz_config::TILT_ANGLE_OFFSET;
    if (logicalTilt < ptz_config::TILT_MIN_ANGLE) logicalTilt = ptz_config::TILT_MIN_ANGLE;
    if (logicalTilt > ptz_config::TILT_MAX_ANGLE) logicalTilt = ptz_config::TILT_MAX_ANGLE;
    return {static_cast<uint8_t>(logicalPan), static_cast<uint8_t>(logicalTilt)}; 
}

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
    if (!isAxisCalibrationValid(panMinUs, panCenterUs, panMaxUs) ||
        !isAxisCalibrationValid(tiltMinUs, tiltCenterUs, tiltMaxUs)) {
        return false;
    }

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
    } else {
        tiltCurrentUs = pulseUs;
        tiltServo.writeMicroseconds(static_cast<int>(pulseUs));
    }
    return true;
}

bool PtzServo::setCalibrationValue(const String& axis, const String& key, uint16_t pulseUs) {
    if (!calibrationMode || pulseUs < 500 || pulseUs > 2500) {
        return false;
    }

    const bool isPan = axis == "PAN";
    if (!isPan && axis != "TILT") {
        return false;
    }

    uint16_t nextMinUs = isPan ? panMinUs : tiltMinUs;
    uint16_t nextCenterUs = isPan ? panCenterUs : tiltCenterUs;
    uint16_t nextMaxUs = isPan ? panMaxUs : tiltMaxUs;

    if (key == "MIN") {
        nextMinUs = pulseUs;
    } else if (key == "CENTER") {
        nextCenterUs = pulseUs;
    } else if (key == "MAX") {
        nextMaxUs = pulseUs;
    } else {
        return false;
    }

    if (!isAxisCalibrationValid(nextMinUs, nextCenterUs, nextMaxUs)) {
        return false;
    }

    if (key == "MIN") {
        if (isPan) {
            panMinUs = pulseUs;
        } else {
            tiltMinUs = pulseUs;
        }
        return true;
    }
    if (key == "CENTER") {
        if (isPan) {
            panCenterUs = pulseUs;
        } else {
            tiltCenterUs = pulseUs;
        }
        return true;
    }
    if (key == "MAX") {
        if (isPan) {
            panMaxUs = pulseUs;
        } else {
            tiltMaxUs = pulseUs;
        }
        return true;
    }
    return false;
}

void PtzServo::resetCalibration() {
    // 重置为默认脉宽范围
    panMinUs = 500;
    panMaxUs = 2500;
    panCenterUs = 1500;
    tiltMinUs = 500;
    tiltMaxUs = 2500;
    tiltCenterUs = 1500;
    // 保存默认值到存储，覆盖之前的校准数据
    CalibData data{};
    data.panMin = panMinUs;
    data.panMax = panMaxUs;
    data.panCenter = panCenterUs;
    data.tiltMin = tiltMinUs;
    data.tiltMax = tiltMaxUs;
    data.tiltCenter = tiltCenterUs;
    data.magic = CalibStorage::MAGIC;
    calibStorage.save(data);
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
    panServo.writeMicroseconds(static_cast<int>(panCurrentUs));
    tiltServo.writeMicroseconds(static_cast<int>(tiltCurrentUs));
}
