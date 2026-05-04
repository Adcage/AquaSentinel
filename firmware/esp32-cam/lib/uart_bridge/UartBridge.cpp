#include "UartBridge.h"

#include "protocol.h"

UartBridge::UartBridge(HardwareSerial& serialRef) : serial(serialRef) {}

void UartBridge::begin(int baudRate, int rxPin, int txPin) {
    serial.begin(baudRate, SERIAL_8N1, rxPin, txPin);
}

String UartBridge::sendHome() { return sendCommand(bridge_protocol::CMD_HOME); }

String UartBridge::sendStatus() { return sendCommand(bridge_protocol::CMD_STATUS); }

String UartBridge::sendNudge(const String& dir, int step) {
    String cmd = "NUDGE:";
    cmd += dir;
    cmd += ",";
    cmd += String(step);
    cmd += "\n";
    return sendCommand(cmd);
}

String UartBridge::calibStart() { return sendCommand(bridge_protocol::CMD_CALIB_START); }

String UartBridge::calibData() { return sendCommand(bridge_protocol::CMD_CALIB_DATA); }

String UartBridge::calibSave() { return sendCommand(bridge_protocol::CMD_CALIB_SAVE); }

String UartBridge::calibExit() { return sendCommand(bridge_protocol::CMD_CALIB_EXIT); }

String UartBridge::calibSetPan(int pulseUs) {
    String cmd = "CALIB:PAN,";
    cmd += String(pulseUs);
    cmd += "\n";
    return sendCommand(cmd);
}

String UartBridge::calibSetTilt(int pulseUs) {
    String cmd = "CALIB:TILT,";
    cmd += String(pulseUs);
    cmd += "\n";
    return sendCommand(cmd);
}

String UartBridge::sendCommand(const String& cmd, uint32_t timeoutMs) {
    while (serial.available() > 0) {
        serial.read();
    }
    serial.print(cmd);

    String response;
    response.reserve(96);
    const uint32_t startAt = millis();
    while (millis() - startAt < timeoutMs) {
        while (serial.available() > 0) {
            const char ch = static_cast<char>(serial.read());
            if (ch == '\r') {
                continue;
            }
            if (ch == '\n') {
                response.trim();
                if (response.length() > 0) {
                    return response;
                }
                continue;
            }
            response += ch;
        }
        delay(2);
    }
    return "ERR:UART_TIMEOUT";
}
