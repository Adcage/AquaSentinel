#include "OledDisplay.h"

#ifdef ARDUINO

#include <Arduino.h>
#include <Wire.h>

#include <string.h>

namespace {

const uint8_t OLED_I2C_ADDRESS = 0x3C;
const uint8_t OLED_WIDTH = 128;
const uint8_t OLED_HEIGHT = 64;
const uint16_t OLED_BUFFER_SIZE = OLED_WIDTH * OLED_HEIGHT / 8;
uint8_t g_oledBuffer[OLED_BUFFER_SIZE];

struct Utf8Glyph {
    const char* utf8;
    uint16_t rows[12];
};

const Utf8Glyph CHINESE_GLYPHS[] = {
    {"启", {0x40, 0x3FE, 0x202, 0x202, 0x3FE, 0x200, 0x200, 0x3FC, 0x504, 0x504, 0x5FC, 0x000}},
    {"动", {0x008, 0x788, 0x008, 0x03E, 0x7CA, 0x212, 0x292, 0x492, 0x4D2, 0x762, 0x02E, 0x000}},
    {"中", {0x000, 0x040, 0x7FE, 0x442, 0x442, 0x442, 0x7FE, 0x442, 0x040, 0x040, 0x040, 0x000}},
    {"云", {0x3FC, 0x000, 0x000, 0x7FE, 0x080, 0x088, 0x108, 0x104, 0x21C, 0x3E2, 0x000, 0x000}},
    {"台", {0x040, 0x080, 0x108, 0x204, 0x7FA, 0x002, 0x3FC, 0x204, 0x204, 0x204, 0x3FC, 0x000}},
    {"校", {0x210, 0x208, 0x27E, 0x724, 0x242, 0x366, 0x6A4, 0xA18, 0x218, 0x21C, 0x2E2, 0x000}},
    {"准", {0x050, 0x448, 0x2FE, 0x288, 0x188, 0x1FE, 0x288, 0x2FE, 0x488, 0x488, 0x4FE, 0x000}},
    {"电", {0x040, 0x040, 0x3FC, 0x244, 0x3FC, 0x244, 0x244, 0x3FC, 0x040, 0x042, 0x03E, 0x000}},
    {"量", {0x3FC, 0x204, 0x3FC, 0x1F8, 0x7FE, 0x3FC, 0x244, 0x3FC, 0x1F8, 0x3FC, 0x7FE, 0x000}},
    {"回", {0x7FE, 0x402, 0x402, 0x5F2, 0x512, 0x512, 0x4F2, 0x402, 0x402, 0x7FE, 0x000, 0x000}},
    {"正", {0x7FE, 0x020, 0x020, 0x020, 0x23C, 0x220, 0x220, 0x220, 0x220, 0x7FE, 0x000, 0x000}},
    {"在", {0x040, 0x080, 0x7FE, 0x100, 0x110, 0x210, 0x6FE, 0x210, 0x210, 0x210, 0x2FE, 0x000}},
    {"网", {0x000, 0x000, 0x3FC, 0x204, 0x3FC, 0x294, 0x294, 0x37C, 0x204, 0x21C, 0x000, 0x000}},
    {"络", {0x000, 0x000, 0x13C, 0x2EC, 0x3B8, 0x138, 0x3EC, 0x07C, 0x1C4, 0x27C, 0x000, 0x000}},
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
        case '%': { static const uint8_t glyph[7] = {0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13}; return glyph; }
        case '-': { static const uint8_t glyph[7] = {0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00}; return glyph; }
        case '.': { static const uint8_t glyph[7] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x0C}; return glyph; }
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
        case 'B': { static const uint8_t glyph[7] = {0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E}; return glyph; }
        case 'C': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E}; return glyph; }
        case 'D': { static const uint8_t glyph[7] = {0x1E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x1E}; return glyph; }
        case 'E': { static const uint8_t glyph[7] = {0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F}; return glyph; }
        case 'F': { static const uint8_t glyph[7] = {0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10}; return glyph; }
        case 'G': { static const uint8_t glyph[7] = {0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0E}; return glyph; }
        case 'H': { static const uint8_t glyph[7] = {0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11}; return glyph; }
        case 'I': { static const uint8_t glyph[7] = {0x0E, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0E}; return glyph; }
        case 'K': { static const uint8_t glyph[7] = {0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11}; return glyph; }
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
        case 'W': { static const uint8_t glyph[7] = {0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x0A}; return glyph; }
        case 'X': { static const uint8_t glyph[7] = {0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11}; return glyph; }
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
    for (uint8_t row = 0; row < 12; ++row) {
        for (uint8_t col = 0; col < 12; ++col) {
            if ((glyph.rows[row] >> (11 - col)) & 0x01) {
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
            cursorX += 13;
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

void OledDisplay::update(const OledUiState& state) {
    if (!initialized) {
        return;
    }

    const bool intervalElapsed = (state.uptimeMs - lastRenderMs) >= REFRESH_INTERVAL_MS;
    const bool stateChanged = !hasLastState || hasStateChanged(state, lastState);
    if (!intervalElapsed && !stateChanged) {
        return;
    }

    drawFrame(OledViewModel::build(state));
    lastState = state;
    hasLastState = true;
    lastRenderMs = state.uptimeMs;
}

bool OledDisplay::hasStateChanged(const OledUiState& current, const OledUiState& previous) {
    if (current.page != previous.page ||
        current.showActionMessage != previous.showActionMessage ||
        strcmp(current.actionMessage, previous.actionMessage) != 0) {
        return true;
    }

    return current.pan != previous.pan ||
           current.tilt != previous.tilt ||
           current.calibrationMode != previous.calibrationMode ||
           current.panPulseUs != previous.panPulseUs ||
           current.tiltPulseUs != previous.tiltPulseUs ||
           current.batteryRaw != previous.batteryRaw ||
           current.batteryMv != previous.batteryMv ||
           current.batteryPercent != previous.batteryPercent ||
           current.batteryValid != previous.batteryValid ||
           strcmp(current.espIp, previous.espIp) != 0;
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

void OledDisplay::update(const OledUiState&) {}

bool OledDisplay::hasStateChanged(const OledUiState&, const OledUiState&) { return false; }

void OledDisplay::drawFrame(const OledFrame&) {}

#endif
