#pragma once

#include <Arduino.h>

#include "PtzServo.h"

class UartHandler {
   public:
    explicit UartHandler(PtzServo& servoRef);
    void begin(HardwareSerial& serialRef);
    void poll();

   private:
    PtzServo& servo;
    HardwareSerial* serial = nullptr;
    String lineBuffer;

    void handleLine(String line);
    void sendAck();
    void sendStatus();
    void sendCalibOk();
    void sendCalibData();
    void sendErr(const char* code);
};
