#pragma once

#include <Arduino.h>

class UartBridge {
   public:
    explicit UartBridge(HardwareSerial& serialRef);
    void begin(int baudRate, int rxPin, int txPin);

    String sendHome();
    String sendStatus();
    String sendNudge(const String& dir, int step);
    String sendMove(int pan, int tilt);
    String calibStart();
    String calibData();
    String calibSave();
    String calibExit();
    String resetCalibration();
    String calibSetPan(int pulseUs);
    String calibSetTilt(int pulseUs);
    String calibSetValue(const String& axis, const String& key, int pulseUs);

   private:
    HardwareSerial& serial;

    String sendCommand(const String& cmd, uint32_t timeoutMs = 600);
};
