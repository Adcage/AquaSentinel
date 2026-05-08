#include "OledDisplay.h"

#ifdef ARDUINO

#include <Arduino.h>
#include <Wire.h>

#include <string.h>

#include "PtzServo.h"

namespace {

const uint8_t OLED_I2C_ADDRESS = 0x3C;
const uint8_t OLED_WIDTH = 128;
const uint8_t OLED_HEIGHT = 64;
const uint16_t OLED_BUFFER_SIZE = OLED_WIDTH * OLED_HEIGHT / 8;
uint8_t g_oledBuffer[OLED_BUFFER_SIZE];

struct Utf8Glyph {
    const char* utf8;
    uint8_t rows[8];
};

const Utf8Glyph CHINESE_GLYPHS[] = {
    {"启", {0x3C, 0x04, 0x3C, 0x10, 0x3E, 0x12, 0x3E, 0x10}},
    {"动", {0x10, 0x7C, 0x10, 0x7C, 0x12, 0x32, 0x54, 0x88}},
    {"中", {0x10, 0x7C, 0x52, 0x52, 0x7C, 0x10, 0x10, 0x00}},
    {"云", {0x1C, 0x00, 0x7E, 0x08, 0x10, 0x10, 0x1E, 0x00}},
    {"台", {0x1C, 0x00, 0x7E, 0x18, 0x3C, 0x10, 0x7E, 0x00}},
    {"校", {0x22, 0x14, 0x7F, 0x14, 0x22, 0x1C, 0x22, 0x41}},
    {"准", {0x04, 0x3E, 0x04, 0x7E, 0x24, 0x24, 0x44, 0x84}},
};

const uint8_t ASCII_SPACE[7] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

void sendCommand(uint8_t command) {
    Wire.beginTransmission(OLED_I2C_ADDRESS);
    Wire.write(0x00);
    Wire.write(command);
    Wire.endTransmission();
}

void sendCommandSequence(const uint8_t* commands, size_t count) {
    for (size_t index = 0; index < count; ++index) {
        sendCommand(commands[index]);
    }
}

void clearBuffer() {
    memset(g_oledBuffer, 0, sizeof(g_oledBuffer));
}

void drawPixel(int16_t x, int16_t y) {
    if (x < 0 || y < 0 || x >= OLED_WIDTH || y >= OLED_HEIGHT) {
        return;
    }

    const uint16_t offset = static_cast<uint16_t>(x) + (static_cast<uint16_t>(y) / 8U) * OLED_WIDTH;
    g_oledBuffer[offset] |= static_cast<uint8_t>(1U << (y & 0x07));
}

void drawHLine(int16_t x, int16_t y, uint8_t width) {
    for (uint8_t index = 0; index < width; ++index) {
        drawPixel(x + index, y);
    }
}

void sendBuffer() {
    for (uint8_t page = 0; page < 8; ++page) {
        sendCommand(static_cast<uint8_t>(0xB0 + page));
        sendCommand(0x00);
        sendCommand(0x10);

        for (uint8_t column = 0; column < OLED_WIDTH; column += 16) {
            Wire.beginTransmission(OLED_I2C_ADDRESS);
            Wire.write(0x40);
            for (uint8_t offset = 0; offset < 16; ++offset) {
                Wire.write(g_oledBuffer[page * OLED_WIDTH + column + offset]);
            }
            Wire.endTransmission();
        }
    }
}

const uint8_t* getAsciiGlyph(char ch) {
    switch (ch) {
        case '0': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E}; return glyph; }
        case '1': { static const uint8_t glyph[7] = {0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E}; return glyph; }
        case '2': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F}; return glyph; }
        case '3': { static const uint8_t glyph[7] = {0x1E, 0x01, 0x01, 0x06, 0x01, 0x01, 0x1E}; return glyph; }
        case '4': { static const uint8_t glyph[7] = {0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02}; return glyph; }
        case '5': { static const uint8_t glyph[7] = {0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E}; return glyph; }
        case '6': { static const uint8_t glyph[7] = {0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E}; return glyph; }
        case '7': { static const uint8_t glyph[7] = {0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08}; return glyph; }
        case '8': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E}; return glyph; }
        case '9': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C}; return glyph; }
        case 'A': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11}; return glyph; }
        case 'C': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E}; return glyph; }
        case 'D': { static const uint8_t glyph[7] = {0x1E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x1E}; return glyph; }
        case 'E': { static const uint8_t glyph[7] = {0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F}; return glyph; }
        case 'G': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0E}; return glyph; }
        case 'H': { static const uint8_t glyph[7] = {0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11}; return glyph; }
        case 'I': { static const uint8_t glyph[7] = {0x0E, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0E}; return glyph; }
        case 'L': { static const uint8_t glyph[7] = {0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F}; return glyph; }
        case 'M': { static const uint8_t glyph[7] = {0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11}; return glyph; }
        case 'N': { static const uint8_t glyph[7] = {0x11, 0x19, 0x15, 0x13, 0x11, 0x11, 0x11}; return glyph; }
        case 'O': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E}; return glyph; }
        case 'P': { static const uint8_t glyph[7] = {0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10}; return glyph; }
        case 'Q': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D}; return glyph; }
        case 'R': { static const uint8_t glyph[7] = {0x1E, 0x11, 0x11, 0x1E, 0x12, 0x11, 0x11}; return glyph; }
        case 'S': { static const uint8_t glyph[7] = {0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E}; return glyph; }
        case 'T': { static const uint8_t glyph[7] = {0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04}; return glyph; }
        case 'U': { static const uint8_t glyph[7] = {0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E}; return glyph; }
        case 'V': { static const uint8_t glyph[7] = {0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04}; return glyph; }
        case 'Y': { static const uint8_t glyph[7] = {0x11, 0x11, 0x0A, 0x04, 0x04, 0x04, 0x04}; return glyph; }
        case ' ': return ASCII_SPACE;
        default: return ASCII_SPACE;
    }
}

const Utf8Glyph* findChineseGlyph(const char* text) {
    for (size_t index = 0; index < sizeof(CHINESE_GLYPHS) / sizeof(CHINESE_GLYPHS[0]); ++index) {
        if (strncmp(text, CHINESE_GLYPHS[index].utf8, 3) == 0) {
            return &CHINESE_GLYPHS[index];
        }
    }
    return nullptr;
}

void drawAsciiChar(int16_t x, int16_t y, char ch) {
    const uint8_t* glyph = getAsciiGlyph(ch);
    for (uint8_t row = 0; row < 7; ++row) {
        for (uint8_t col = 0; col < 5; ++col) {
            if ((glyph[row] >> (4 - col)) & 0x01) {
                drawPixel(x + col, y + row);
            }
        }
    }
}

void drawChineseChar(int16_t x, int16_t y, const Utf8Glyph& glyph) {
    for (uint8_t row = 0; row < 8; ++row) {
        for (uint8_t col = 0; col < 8; ++col) {
            if ((glyph.rows[row] >> (7 - col)) & 0x01) {
                drawPixel(x + col, y + row);
            }
        }
    }
}

void drawCompactText(int16_t x, int16_t y, const char* text) {
    size_t index = 0;
    int16_t cursorX = x;
    const size_t textLength = strlen(text);
    while (index < textLength) {
        const unsigned char current = static_cast<unsigned char>(text[index]);
        if (current < 0x80) {
            drawAsciiChar(cursorX, y, text[index]);
            cursorX += 6;
            ++index;
            continue;
        }

        const Utf8Glyph* glyph = findChineseGlyph(text + index);
        if (glyph != nullptr) {
            drawChineseChar(cursorX, y, *glyph);
            cursorX += 9;
            index += 3;
            continue;
        }

        index += 3;
    }
}

}  // namespace

void OledDisplay::begin() {
    static const uint8_t initSequence[] = {
        0xAE, 0xD5, 0x80, 0xA8, 0x3F, 0xD3, 0x00, 0x40,
        0x8D, 0x14, 0x20, 0x02, 0xA1, 0xC8, 0xDA, 0x12,
        0x81, 0xCF, 0xD9, 0xF1, 0xDB, 0x40, 0xA4, 0xA6, 0xAF,
    };

    Wire.setSCL(PB6);
    Wire.setSDA(PB7);
    Wire.begin();
    sendCommandSequence(initSequence, sizeof(initSequence));
    clearBuffer();
    sendBuffer();
    initialized = true;
}

void OledDisplay::update(const PtzServo& servo, uint32_t uptimeMs) {
    if (!initialized) {
        return;
    }

    const OledDisplayState state = captureState(servo, uptimeMs);
    const bool intervalElapsed = (uptimeMs - lastRenderMs) >= REFRESH_INTERVAL_MS;
    const bool stateChanged = !hasLastState || hasStateChanged(state, lastState);
    if (!intervalElapsed && !stateChanged) {
        return;
    }

    drawFrame(OledViewModel::build(state));
    lastState = state;
    hasLastState = true;
    lastRenderMs = uptimeMs;
}

OledDisplayState OledDisplay::captureState(const PtzServo& servo, uint32_t uptimeMs) {
    const PtzState state = servo.state();
    OledDisplayState displayState;
    displayState.pan = state.pan;
    displayState.tilt = state.tilt;
    displayState.calibrationMode = servo.isCalibrationMode();
    displayState.panPulseUs = servo.currentPanPulseUs();
    displayState.tiltPulseUs = servo.currentTiltPulseUs();
    displayState.uptimeMs = uptimeMs;
    return displayState;
}

bool OledDisplay::hasStateChanged(const OledDisplayState& current, const OledDisplayState& previous) {
    return current.pan != previous.pan ||
           current.tilt != previous.tilt ||
           current.calibrationMode != previous.calibrationMode ||
           current.panPulseUs != previous.panPulseUs ||
           current.tiltPulseUs != previous.tiltPulseUs;
}

void OledDisplay::drawFrame(const OledFrame& frame) {
    clearBuffer();
    drawCompactText(0, 2, frame.title);
    drawHLine(0, 13, 128);
    drawCompactText(0, 18, frame.lines[0]);
    drawCompactText(0, 30, frame.lines[1]);
    drawCompactText(0, 42, frame.lines[2]);
    drawCompactText(0, 54, frame.lines[3]);
    sendBuffer();
}

#else

void OledDisplay::begin() {}

void OledDisplay::update(const PtzServo&, uint32_t) {}

OledDisplayState OledDisplay::captureState(const PtzServo&, uint32_t uptimeMs) {
    OledDisplayState state;
    state.uptimeMs = uptimeMs;
    return state;
}

bool OledDisplay::hasStateChanged(const OledDisplayState&, const OledDisplayState&) { return false; }

void OledDisplay::drawFrame(const OledFrame&) {}

#endif
