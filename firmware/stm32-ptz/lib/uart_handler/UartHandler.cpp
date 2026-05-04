#include "UartHandler.h"

#include "config.h"
#include "protocol.h"

UartHandler::UartHandler(PtzServo& servoRef) : servo(servoRef) {}

void UartHandler::begin(HardwareSerial& serialRef) {
    serial = &serialRef;
    lineBuffer.reserve(96);
}

void UartHandler::poll() {
    if (serial == nullptr) {
        return;
    }
    while (serial->available() > 0) {
        const char ch = static_cast<char>(serial->read());
        if (ch == '\r') {
            continue;
        }
        if (ch == '\n') {
            if (lineBuffer.length() > 0) {
                handleLine(lineBuffer);
                lineBuffer = "";
            }
            continue;
        }
        if (lineBuffer.length() < 90) {
            lineBuffer += ch;
        }
    }
}

void UartHandler::handleLine(String line) {
    line.trim();
    if (line.length() == 0) {
        return;
    }

    if (line == ptz_protocol::CMD_HOME) {
        servo.home();
        sendAck();
        return;
    }
    if (line == ptz_protocol::CMD_CALIB_START) {
        servo.enterCalibration();
        sendCalibOk();
        return;
    }
    if (line == ptz_protocol::CMD_CALIB_EXIT) {
        servo.exitCalibration();
        sendCalibOk();
        return;
    }
    if (line == ptz_protocol::CMD_CALIB_SAVE) {
        if (!servo.saveCalibration()) {
            sendErr(ptz_protocol::ERR_BAD_ARG);
            return;
        }
        sendCalibData();
        return;
    }
    if (line == ptz_protocol::CMD_CALIB_DATA) {
        sendCalibData();
        return;
    }
    if (line.startsWith(ptz_protocol::CMD_CALIB_PAN)) {
        const int pulseVal = line.substring(10).toInt();
        if (!servo.setCalibrationPulse(true, static_cast<uint16_t>(pulseVal))) {
            sendErr(ptz_protocol::ERR_BAD_ARG);
            return;
        }
        sendCalibOk();
        return;
    }
    if (line.startsWith(ptz_protocol::CMD_CALIB_TILT)) {
        const int pulseVal = line.substring(11).toInt();
        if (!servo.setCalibrationPulse(false, static_cast<uint16_t>(pulseVal))) {
            sendErr(ptz_protocol::ERR_BAD_ARG);
            return;
        }
        sendCalibOk();
        return;
    }
    if (line == ptz_protocol::CMD_STATUS) {
        sendStatus();
        return;
    }
    if (!line.startsWith(ptz_protocol::CMD_NUDGE)) {
        sendErr(ptz_protocol::ERR_BAD_CMD);
        return;
    }

    const int commaIndex = line.indexOf(',', 6);
    if (commaIndex < 0) {
        sendErr(ptz_protocol::ERR_BAD_ARG);
        return;
    }

    String dir = line.substring(6, commaIndex);
    dir.trim();
    dir.toUpperCase();

    String stepText = line.substring(commaIndex + 1);
    stepText.trim();
    const int stepVal = stepText.toInt();
    if (stepVal <= 0) {
        sendErr(ptz_protocol::ERR_BAD_ARG);
        return;
    }

    const uint8_t step =
            static_cast<uint8_t>(min(stepVal, static_cast<int>(ptz_config::MAX_NUDGE_STEP)));
    if (!servo.nudge(dir, step)) {
        sendErr(ptz_protocol::ERR_BAD_ARG);
        return;
    }
    sendAck();
}

void UartHandler::sendAck() {
    if (serial == nullptr) {
        return;
    }
    const PtzState state = servo.state();
    serial->print(ptz_protocol::RESP_ACK);
    serial->print(state.pan);
    serial->print(',');
    serial->print(state.tilt);
    serial->print('\n');
}

void UartHandler::sendStatus() {
    if (serial == nullptr) {
        return;
    }
    const PtzState state = servo.state();
    serial->print(ptz_protocol::RESP_STATUS);
    serial->print(state.pan);
    serial->print(',');
    serial->print(state.tilt);
    serial->print(',');
    serial->print(servo.isCalibrationMode() ? 1 : 0);
    serial->print('\n');
}

void UartHandler::sendCalibOk() {
    if (serial == nullptr) {
        return;
    }
    serial->print(ptz_protocol::RESP_CALIB_OK);
    serial->print(servo.currentPanPulseUs());
    serial->print(',');
    serial->print(servo.currentTiltPulseUs());
    serial->print('\n');
}

void UartHandler::sendCalibData() {
    if (serial == nullptr) {
        return;
    }
    serial->print(ptz_protocol::RESP_CALIB_DATA);
    serial->print(servo.panMinPulseUs());
    serial->print(',');
    serial->print(servo.panMaxPulseUs());
    serial->print(',');
    serial->print(servo.panCenterPulseUs());
    serial->print(',');
    serial->print(servo.tiltMinPulseUs());
    serial->print(',');
    serial->print(servo.tiltMaxPulseUs());
    serial->print(',');
    serial->print(servo.tiltCenterPulseUs());
    serial->print('\n');
}

void UartHandler::sendErr(const char* code) {
    if (serial == nullptr) {
        return;
    }
    serial->print(ptz_protocol::RESP_ERR);
    serial->print(code);
    serial->print('\n');
}
