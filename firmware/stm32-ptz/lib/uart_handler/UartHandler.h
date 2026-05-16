#pragma once

#include <Arduino.h>

#include "PtzServo.h"

class UartHandler {
   public:
    UartHandler(PtzServo& servoRef, char* ipBufferRef);
    void begin(HardwareSerial& serialRef);
    void poll();

   private:
    PtzServo& servo;
    char* ipBuffer;
    HardwareSerial* serial = nullptr;
    String lineBuffer;

    void handleLine(String line);
    void sendAck();
    void sendStatus();
    void sendCalibOk();
    void sendCalibData();
    void sendErr(const char* code);
};
